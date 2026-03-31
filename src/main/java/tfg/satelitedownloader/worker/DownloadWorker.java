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
                LOGGER.log(Level.SEVERE, "Error inesperado procesando descarga asíncrona.", e);
            }
        }
    }

    private void processRequest(SatelliteDownloadRequest request) {
        try {
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

            LOGGER.info("Iniciando búsqueda de imágenes satelitales...");
            List<Tile> tiles = tileProvider.getTile(name, dateStart, dateEnd, area);

            if (tiles.isEmpty()) {
                LOGGER.info("No se encontraron imágenes en el área y fecha especificadas.");
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
                LOGGER.info("Filtrando a " + tilesToDownload.size() + " imágenes seleccionadas.");
            }

            if (tilesToDownload.isEmpty()) {
                LOGGER.info("No hay tiles que coincidan con la selección del usuario.");
                return;
            }

            Tile firstTile = tilesToDownload.get(0);
            String tileInfo = firstTile.getParametersForDownload().length > 0 ? firstTile.getParametersForDownload()[0]
                    : "tile";
            LOGGER.info("Iniciando descarga de: " + tileInfo);
            tileProvider.downloadTile(firstTile);
            LOGGER.info("Descarga completada con éxito para la petición pendiente.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Falló la descarga de la imagen en segundo plano: " + e.getMessage(), e);
        }
    }
}
