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
import java.io.IOException;
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
            // Validate request
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

            // Add the request to the queue (Producer)
            queue.put(request);

            SatelliteDownloadResponse response = new SatelliteDownloadResponse(
                    "success",
                    "Satellite download request added to queue for background processing",
                    0,
                    null);

            LOGGER.info("Satellite download queued successfully");
            return Response.ok(response).build();

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

    @POST
    @Path("/downloadPreviews")
    public Response downloadPreviews(SatelliteDownloadRequest request) {
        try {
            // Validate request
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

            LOGGER.info("Processing preview download request: " + request.toString());

            // Execute the preview download logic
            SatelliteDownloadResponse response = executePreviewDownloadLogic(
                    request.getInitialDay(),
                    request.getFinalDay(),
                    request.getGeoJson().toString());

            LOGGER.info("Preview download completed successfully");
            return Response.ok(response).build();

        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error processing preview download", e);
            SatelliteDownloadResponse errorResponse = new SatelliteDownloadResponse(
                    "error", "Failed to download previews: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid request parameters", e);
            SatelliteDownloadResponse errorResponse = new SatelliteDownloadResponse(
                    "error", "Invalid request parameters: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error processing preview download", e);
            SatelliteDownloadResponse errorResponse = new SatelliteDownloadResponse(
                    "error", "An unexpected error occurred: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }

    /**
     * Executes the preview download logic - searches for tiles and downloads their
     * preview images
     */
    private SatelliteDownloadResponse executePreviewDownloadLogic(String iday, String fday, String geojsonString)
            throws IOException, InterruptedException {

        int option = 1; // Default to Copernicus
        Provider tileProvider;
        String name = "";
        String dateStart = "";
        String dateEnd = "";
        String area = "";

        // Set the dates from the request
        dateStart = iday;
        dateEnd = fday;

        switch (option) {
            case 1 -> {
                tileProvider = copernicusProvider;
                name = "SENTINEL-2";
                // Convert dates to proper format if needed
                if (!dateStart.contains("T")) {
                    dateStart = dateStart + "T00:00:00.000Z";
                }
                if (!dateEnd.contains("T")) {
                    dateEnd = dateEnd + "T00:00:00.000Z";
                }
                // Convert GeoJSON to WKT format for Copernicus
                area = GeoJsonConverter.convertToWKT(geojsonString);
            }
            case 2 -> {
                tileProvider = new LandsatProvider();
                name = "Global Land Survey";
                // Convert GeoJSON to bounding box format for Landsat
                area = GeoJsonConverter.convertToBoundingBox(geojsonString);
            }
            case 3 -> {
                tileProvider = new ModisProvider();
                // Convert GeoJSON to bounding box format for MODIS
                area = GeoJsonConverter.convertToBoundingBox(geojsonString);
            }
            default -> throw new IllegalArgumentException("Invalid option");
        }

        // Search for tiles
        List<Tile> tiles = tileProvider.getTile(name, dateStart, dateEnd, area);

        if (tiles.isEmpty()) {
            return new SatelliteDownloadResponse("success", "No tiles found for the specified criteria", 0, null);
        }

        // Download preview for each Copernicus tile
        if (tileProvider instanceof CopernicusProvider) {
            int previewsDownloaded = 0;
            String previewsFolder = propsReader.get("COPERNICUS_PREVIEW_FOLDER");

            // Get access token for preview downloads
            String accessToken = null;
            try {
                accessToken = ((CopernicusProvider) tileProvider).getAccessToken();
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Failed to obtain access token for previews: " + e.getMessage(), e);
                return new SatelliteDownloadResponse("error", "Failed to obtain access token: " + e.getMessage());
            }

            for (Tile tile : tiles) {
                if (tile instanceof CopernicusTile) {
                    CopernicusTile cTile = (CopernicusTile) tile;
                    String previewLink = cTile.getPreviewLink();

                    if (previewLink != null && !previewLink.isEmpty()) {
                        try {
                            String outputPath = previewsFolder + "/" + cTile.getName() + "_preview.png";
                            ((CopernicusProvider) tileProvider).downloadPreviewImage(previewLink, accessToken,
                                    outputPath);
                            previewsDownloaded++;
                            LOGGER.info("Preview downloaded for tile: " + cTile.getName());
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to download preview for tile: " + cTile.getName(), e);
                        }
                    }
                }
            }

            return new SatelliteDownloadResponse(
                    "success",
                    "Preview downloads completed",
                    previewsDownloaded,
                    "Downloaded " + previewsDownloaded + " out of " + tiles.size() + " preview images");
        }

        return new SatelliteDownloadResponse(
                "success",
                "Preview download not supported for this provider",
                0,
                null);
    }
}
