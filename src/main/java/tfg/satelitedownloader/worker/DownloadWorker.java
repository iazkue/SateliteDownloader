package tfg.satelitedownloader.worker;

import io.dropwizard.lifecycle.Managed;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import tfg.satelitedownloader.api.SatelliteDownloadRequest;
import tfg.satelitedownloader.core.Provider;
import tfg.satelitedownloader.core.Tile;
import tfg.satelitedownloader.service.CopernicusProvider;
import tfg.satelitedownloader.service.LandsatProvider;
import tfg.satelitedownloader.service.ModisProvider;
import tfg.satelitedownloader.util.GeoJsonConverter;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadWorker implements Managed, Runnable {

    private static final Logger LOGGER = Logger.getLogger(DownloadWorker.class.getName());

    private final LinkedBlockingQueue<SatelliteDownloadRequest> queue;
    private final CopernicusProvider copernicusProvider;
    private final SessionFactory sessionFactory;
    private Thread workerThread;

    public DownloadWorker(LinkedBlockingQueue<SatelliteDownloadRequest> queue, CopernicusProvider copernicusProvider,
            SessionFactory sessionFactory) {
        this.queue = queue;
        this.copernicusProvider = copernicusProvider;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void start() throws Exception {
        workerThread = new Thread(this, "DownloadWorkerThread");
        workerThread.start();
        LOGGER.info("DownloadWorkerThread started.");
    }

    @Override
    public void stop() throws Exception {
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread.join(2000);
            LOGGER.info("DownloadWorkerThread stopped.");
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Petizioa egon arte haria blokeatzen du (consumer)
                SatelliteDownloadRequest request = queue.take();
                LOGGER.info("Se ha extraído una petición de descarga de la cola.");

                // Since this is a background thread without @UnitOfWork, we need to manually
                // open a session
                // and bind it to the thread context so that the DAOs can use currentSession().
                try (Session session = sessionFactory.openSession()) {
                    ManagedSessionContext.bind(session);
                    Transaction transaction = session.beginTransaction();
                    try {
                        processRequest(request);
                        transaction.commit();
                    } catch (Exception e) {
                        transaction.rollback();
                        throw e;
                    } finally {
                        ManagedSessionContext.unbind(sessionFactory);
                    }
                }

            } catch (InterruptedException e) {
                LOGGER.info("DownloadWorkerThread ha sido interrumpido.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error processing asynchronous download.", e);
            }
        }
    }

    private void processRequest(SatelliteDownloadRequest request) {
        String taskId = request.getTaskId();
        tfg.satelitedownloader.service.DownloadQueueManager queueManager = tfg.satelitedownloader.service.DownloadQueueManager
                .getInstance();
        tfg.satelitedownloader.model.SatelliteDownloadTask task = queueManager.getTask(taskId);

        if (task == null) {
            int count = request.getSelectedImages() != null ? request.getSelectedImages().size() : 1;
            task = new tfg.satelitedownloader.model.SatelliteDownloadTask(
                    taskId != null ? taskId : "task_" + System.currentTimeMillis(),
                    request.getInitialDay(),
                    request.getFinalDay(),
                    count);
            queueManager.registerTask(task);
            taskId = task.getTaskId();
        }

        if (task.isCancelled()) {
            LOGGER.info("Task " + taskId + " was cancelled before processing.");
            return;
        }

        try {
            queueManager.updateProgress(taskId, "DOWNLOADING", 0, task.getTotalImages(), "", 0, "Buscando imágenes...");

            int option = 1; // Default to Copernicus
            Provider tileProvider;
            String name = "";
            String dateStart = request.getInitialDay();
            String dateEnd = request.getFinalDay();
            String area = "";

            switch (option) {
                case 1 -> {
                    tileProvider = copernicusProvider;
                    name = "SENTINEL-2";
                    if (!dateStart.contains("T")) {
                        dateStart = dateStart + "T00:00:00.000Z";
                    }
                    if (!dateEnd.contains("T")) {
                        dateEnd = dateEnd + "T00:00:00.000Z";
                    }
                    area = GeoJsonConverter.convertToWKT(request.getGeoJson().toString());
                }
                case 2 -> {
                    tileProvider = new LandsatProvider();
                    name = "Global Land Survey";
                    area = GeoJsonConverter.convertToBoundingBox(request.getGeoJson().toString());
                }
                case 3 -> {
                    tileProvider = new ModisProvider();
                    area = GeoJsonConverter.convertToBoundingBox(request.getGeoJson().toString());
                }
                default -> throw new IllegalArgumentException("Invalid option");
            }

            LOGGER.info("Starting to search for satellite images...");
            List<Tile> tiles = tileProvider.getTile(name, dateStart, dateEnd, area);

            if (tiles.isEmpty()) {
                LOGGER.info("No images found in the specified area and date.");
                queueManager.updateProgress(taskId, "FAILED", 0, 0, "", 0, "No images found.");
                return;
            }

            // Also applies selected images filter if they are attached to the request
            List<Tile> tilesToDownload = tiles;
            List<String> selectedImages = request.getSelectedImages();
            if (selectedImages != null && !selectedImages.isEmpty()) {
                tilesToDownload = new java.util.ArrayList<>();
                for (Tile tile : tiles) {
                    if (tile instanceof tfg.satelitedownloader.model.CopernicusTile) {
                        tfg.satelitedownloader.model.CopernicusTile cTile = (tfg.satelitedownloader.model.CopernicusTile) tile;
                        String expectedFilename = cTile.getName() + "_preview.png";
                        if (selectedImages.contains(expectedFilename)) {
                            tilesToDownload.add(tile);
                        }
                    } else {
                        tilesToDownload.add(tile);
                    }
                }
                LOGGER.info("Filtering to " + tilesToDownload.size() + " selected images.");
            }

            if (tilesToDownload.isEmpty()) {
                LOGGER.info("No tiles match the user's selection.");
                queueManager.updateProgress(taskId, "FAILED", 0, 0, "", 0,
                        "No images match the user's selection.");
                return;
            }

            int totalTiles = tilesToDownload.size();
            queueManager.updateProgress(taskId, "DOWNLOADING", 0, totalTiles, "", 0,
                    "Starting download of " + totalTiles + " image(s)...");

            for (int i = 0; i < totalTiles; i++) {
                if (task.isCancelled()) {
                    LOGGER.info("Task " + taskId + " cancelled during execution.");
                    queueManager.updateProgress(taskId, "CANCELLED", i, totalTiles, "", (i * 100) / totalTiles,
                            "Download cancelled by the user.");
                    return;
                }

                Tile currentTile = tilesToDownload.get(i);
                String tileName = currentTile instanceof tfg.satelitedownloader.model.CopernicusTile c ? c.getName()
                        : (currentTile.getParametersForDownload().length > 0 ? currentTile.getParametersForDownload()[0]
                                : "tile");
                int currentPercent = (i * 100) / totalTiles;

                queueManager.updateProgress(
                        taskId,
                        "DOWNLOADING",
                        i + 1,
                        totalTiles,
                        tileName,
                        currentPercent,
                        "Downloading image " + (i + 1) + " of " + totalTiles + ": " + tileName);

                tileProvider.downloadTile(currentTile);

                int endPercent = ((i + 1) * 100) / totalTiles;
                queueManager.updateProgress(
                        taskId,
                        "DOWNLOADING",
                        i + 1,
                        totalTiles,
                        tileName,
                        endPercent,
                        "Completed image " + (i + 1) + " of " + totalTiles);
            }

            queueManager.updateProgress(taskId, "COMPLETED", totalTiles, totalTiles, "", 100,
                    "Download completed successfully.");
            LOGGER.info("Download completed successfully for task " + taskId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to download image in background: " + e.getMessage(), e);
            queueManager.updateProgress(taskId, "FAILED", 0, 0, "", 0, "Error in download: " + e.getMessage());
        }
    }
}
