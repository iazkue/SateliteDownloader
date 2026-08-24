package tfg.satelitedownloader.api;

import tfg.satelitedownloader.service.CopernicusProvider;
import tfg.satelitedownloader.db.CopernicusTileDAO;
import tfg.satelitedownloader.db.CopernicusTileEntity;
import tfg.satelitedownloader.core.Provider;
import tfg.satelitedownloader.core.Tile;
import tfg.satelitedownloader.util.GeoJsonConverter;
import tfg.satelitedownloader.service.LandsatProvider;
import tfg.satelitedownloader.service.ModisProvider;
import tfg.satelitedownloader.model.CopernicusTile;
import tfg.satelitedownloader.util.propsReader;
import tfg.satelitedownloader.api.SatelliteDownloadRequest;
import tfg.satelitedownloader.api.SatelliteDownloadResponse;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple REST resource for the Satellite Downloader application.
 * This is where you define your HTTP endpoints.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SateliteDownloaderResource {

    private static final Logger LOGGER = Logger.getLogger(SateliteDownloaderResource.class.getName());
    private final CopernicusProvider copernicusProvider;
    private final CopernicusTileDAO copernicusTileDAO;
    private final LinkedBlockingQueue<SatelliteDownloadRequest> queue;

    public SateliteDownloaderResource(CopernicusProvider copernicusProvider,
            CopernicusTileDAO copernicusTileDAO,
            LinkedBlockingQueue<SatelliteDownloadRequest> queue) {
        this.copernicusProvider = copernicusProvider;
        this.copernicusTileDAO = copernicusTileDAO;
        this.queue = queue;
    }

    @GET
    public String hello() {
        return "{\"message\": \"Satellite Downloader API is running!\"}";
    }

    @GET
    @Path("/health")
    public String health() {
        return "{\"status\": \"healthy\"}";
    }

    @GET
    @Path("/tiles")
    @UnitOfWork
    public Response listTiles() {
        List<CopernicusTileEntity> tiles = copernicusTileDAO.findAll();
        return Response.ok(tiles).build();
    }

    @POST
    @Path("/downloadImages")
    @UnitOfWork
    public Response downloadImages(SatelliteDownloadRequest request) {
        try {
            if (request == null) {
                SatelliteDownloadResponse errorResponse = new SatelliteDownloadResponse(
                        "error", "Request body is required");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }

            if (request.getInitialDay() == null || request.getFinalDay() == null || request.getGeoJson() == null) {
                SatelliteDownloadResponse errorResponse = new SatelliteDownloadResponse(
                        "error", "All fields (iday, fday, geojson) are required");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }

            LOGGER.info("Processing satellite download request: " + request.toString());

            int count = request.getSelectedImages() != null ? request.getSelectedImages().size() : 1;
            tfg.satelitedownloader.model.SatelliteDownloadTask task =
                    tfg.satelitedownloader.service.DownloadQueueManager.getInstance().createTask(
                            request.getInitialDay(),
                            request.getFinalDay(),
                            count
                    );
            request.setTaskId(task.getTaskId());

            // Add the request to the queue (Producer)
            queue.put(request);

            LOGGER.info("Satellite download queued successfully with taskId: " + task.getTaskId());
            return Response.ok(task).build();

        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error processing satellite download", e);
            SatelliteDownloadResponse errorResponse = new SatelliteDownloadResponse(
                    "error", "Failed to download satellite data: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid request parameters", e);
            SatelliteDownloadResponse errorResponse = new SatelliteDownloadResponse(
                    "error", "Invalid request parameters: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error processing satellite download", e);
            SatelliteDownloadResponse errorResponse = new SatelliteDownloadResponse(
                    "error", "An unexpected error occurred: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }

    @GET
    @Path("/downloadQueue")
    public Response getDownloadQueue() {
        List<tfg.satelitedownloader.model.SatelliteDownloadTask> tasks =
                tfg.satelitedownloader.service.DownloadQueueManager.getInstance().getAllTasks();
        return Response.ok(tasks).build();
    }

    @POST
    @Path("/cancelDownload/{taskId}")
    public Response cancelDownload(@PathParam("taskId") String taskId) {
        tfg.satelitedownloader.service.DownloadQueueManager.getInstance().cancelTask(taskId);
        return Response.ok("{\"status\":\"success\",\"message\":\"Task cancelled\"}").build();
    }

    @POST
    @Path("/downloadPreviews")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadPreviews(SatelliteDownloadRequest request) {
        if (request == null || request.getInitialDay() == null || request.getFinalDay() == null || request.getGeoJson() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"status\":\"error\",\"message\":\"Missing required fields\"}\n").build();
        }

        LOGGER.info("Processing preview download request stream: " + request.toString());

        jakarta.ws.rs.core.StreamingOutput stream = output -> {
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(output, java.nio.charset.StandardCharsets.UTF_8), true);
            try {
                executePreviewDownloadStream(request.getInitialDay(), request.getFinalDay(), request.getGeoJson().toString(), writer);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in streaming preview download", e);
                writer.println("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
                writer.flush();
            }
        };

        return Response.ok(stream).build();
    }

    @GET
    @Path("/previews/{filename}")
    @Produces("image/png")
    public Response getPreviewImage(@PathParam("filename") String filename) {
        String previewsFolder = tfg.satelitedownloader.util.propsReader.get("COPERNICUS_PREVIEW_FOLDER");
        if (previewsFolder != null) {
            previewsFolder = previewsFolder.trim();
        } else {
            previewsFolder = "imagesPreviewFolder"; // fallback
        }
        File file = new File(previewsFolder, filename);
        if (!file.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(file).build();
    }

    /**
     * Executes preview download with stream updates (NDJSON chunks)
     */
    private void executePreviewDownloadStream(String iday, String fday, String geojsonString, java.io.PrintWriter writer) throws Exception {
        writer.println("{\"status\":\"searching\",\"message\":\"Buscando imágenes satelitales...\"}");
        writer.flush();

        String dateStart = iday.contains("T") ? iday : iday + "T00:00:00.000Z";
        String dateEnd = fday.contains("T") ? fday : fday + "T00:00:00.000Z";
        String area = GeoJsonConverter.convertToWKT(geojsonString);

        List<Tile> tiles = copernicusProvider.getTile("SENTINEL-2", dateStart, dateEnd, area);

        if (tiles == null || tiles.isEmpty()) {
            writer.println("{\"status\":\"completed\",\"previewsDownloaded\":0,\"total\":0,\"previewImages\":[],\"message\":\"No se encontraron imágenes.\"}");
            writer.flush();
            return;
        }

        int total = tiles.size();
        writer.println("{\"status\":\"found\",\"total\":" + total + ",\"message\":\"Encontradas " + total + " imágenes. Descargando previews...\"}");
        writer.flush();

        String previewsFolder = propsReader.get("COPERNICUS_PREVIEW_FOLDER");
        if (previewsFolder == null) previewsFolder = "imagesPreviewFolder";
        List<String> previewImages = new ArrayList<>();
        String accessToken = copernicusProvider.getAccessToken();

        int downloaded = 0;
        for (int i = 0; i < tiles.size(); i++) {
            Tile tile = tiles.get(i);
            if (tile instanceof CopernicusTile cTile) {
                String previewLink = cTile.getPreviewLink();
                if (previewLink != null && !previewLink.isEmpty()) {
                    String filename = cTile.getName() + "_preview.png";
                    String outputPath = previewsFolder + "/" + filename;
                    try {
                        copernicusProvider.downloadPreviewImage(previewLink, accessToken, outputPath);
                        downloaded++;
                        previewImages.add(filename);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to download preview for tile: " + cTile.getName(), e);
                    }
                }
            }

            int percent = Math.min(100, (int) (((i + 1) / (double) total) * 100));
            String msg = "Descargando preview " + (i + 1) + " de " + total + " (" + percent + "%)";
            writer.println("{\"status\":\"progress\",\"current\":" + (i + 1) + ",\"total\":" + total + ",\"percent\":" + percent + ",\"message\":\"" + msg + "\"}");
            writer.flush();
        }

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String imagesJson = mapper.writeValueAsString(previewImages);
        writer.println("{\"status\":\"completed\",\"previewsDownloaded\":" + downloaded + ",\"total\":" + total + ",\"previewImages\":" + imagesJson + ",\"message\":\"Vistas previas cargadas correctamente.\"}");
        writer.flush();
    }
}
