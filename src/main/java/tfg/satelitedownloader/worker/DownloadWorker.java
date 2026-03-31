package tfg.satelitedownloader.worker;

import io.dropwizard.lifecycle.Managed;
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
    private Thread workerThread;

    public DownloadWorker(LinkedBlockingQueue<SatelliteDownloadRequest> queue, CopernicusProvider copernicusProvider) {
        this.queue = queue;
        this.copernicusProvider = copernicusProvider;
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
                processRequest(request);
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

            Tile firstTile = tiles.get(0);
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
