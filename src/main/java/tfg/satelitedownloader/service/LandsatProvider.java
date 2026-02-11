package tfg.satelitedownloader.service;

import tfg.satelitedownloader.core.Provider;
import tfg.satelitedownloader.core.Tile;
import tfg.satelitedownloader.util.propsReader;
import tfg.satelitedownloader.model.LandsatTile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LandsatProvider implements Provider {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Tile> getTile(String name, String dateStart, String dateEnd, String area)
            throws IOException, InterruptedException {
        // GET ACCESS TOKEN

        // Construct the body
        String loginToken = String.format("{\"username\":\"%s\", \"token\":\"%s\"}", propsReader.get("LANDSAT_USER"),
                propsReader.get("LANDSAT_TOKEN"));

        // Make the request
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(propsReader.get("LANDSAT_API") + "login-token"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginToken))
                .build();
        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        // Get the access token from the response
        JsonNode rootNodeToken = objectMapper.readTree(loginResponse.body());
        String accessToken = rootNodeToken.path("data").asText();

        // GET THE DATASET

        // Parse coordinates
        String[] coordinates = area.split(",");
        double lat1 = Double.parseDouble(coordinates[0].trim());
        double lon1 = Double.parseDouble(coordinates[1].trim());
        double lat2 = Double.parseDouble(coordinates[2].trim());
        double lon2 = Double.parseDouble(coordinates[3].trim());

        // Prepare dataset search body
        ObjectNode datasetSearchBody = objectMapper.createObjectNode();
        datasetSearchBody.put("datasetName", name);

        ObjectNode spatialFilterNode = datasetSearchBody.putObject("spatialFilter");
        spatialFilterNode.put("filterType", "mbr");

        ObjectNode lowerLeftNode = spatialFilterNode.putObject("lowerLeft");
        lowerLeftNode.put("latitude", lat1);
        lowerLeftNode.put("longitude", lon1);

        ObjectNode upperRightNode = spatialFilterNode.putObject("upperRight");
        upperRightNode.put("latitude", lat2);
        upperRightNode.put("longitude", lon2);

        ObjectNode temporalFilterNode = datasetSearchBody.putObject("temporalFilter");
        temporalFilterNode.put("start", dateStart);
        temporalFilterNode.put("end", dateEnd);

        String datasetSearchJson = objectMapper.writeValueAsString(datasetSearchBody);

        // Make the request
        HttpRequest datasetSearchRequest = HttpRequest.newBuilder()
                .uri(URI.create(propsReader.get("LANDSAT_API") + "dataset-search"))
                .header("Content-Type", "application/json")
                .header("X-Auth-Token", accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(datasetSearchJson))
                .build();

        HttpResponse<String> datasetSearchResponse = client.send(datasetSearchRequest,
                HttpResponse.BodyHandlers.ofString());

        // Get the data from the response
        JsonNode datasetRoot = objectMapper.readTree(datasetSearchResponse.body());
        JsonNode datasets = datasetRoot.path("data");

        List<Tile> tiles = new ArrayList<>();

        if (datasets.isArray()) {
            for (JsonNode dataset : datasets) {
                // GET THE SCENE

                // Build scene search request
                String datasetName = dataset.path("datasetAlias").asText();

                ObjectNode sceneSearchBody = objectMapper.createObjectNode();
                sceneSearchBody.put("datasetName", datasetName);

                ObjectNode sceneSpatialFilter = sceneSearchBody.putObject("spatialFilter");
                sceneSpatialFilter.put("filterType", "mbr");

                ObjectNode sceneLowerLeft = sceneSpatialFilter.putObject("lowerLeft");
                sceneLowerLeft.put("latitude", lat1);
                sceneLowerLeft.put("longitude", lon1);

                ObjectNode sceneUpperRight = sceneSpatialFilter.putObject("upperRight");
                sceneUpperRight.put("latitude", lat2);
                sceneUpperRight.put("longitude", lon2);

                ObjectNode sceneTemporalFilter = sceneSearchBody.putObject("temporalFilter");
                sceneTemporalFilter.put("start", dateStart);
                sceneTemporalFilter.put("end", dateEnd);

                String sceneSearchJson = objectMapper.writeValueAsString(sceneSearchBody);

                // Make the request
                HttpRequest sceneSearchRequest = HttpRequest.newBuilder()
                        .uri(URI.create(propsReader.get("LANDSAT_API") + "scene-search"))
                        .header("Content-Type", "application/json")
                        .header("X-Auth-Token", accessToken)
                        .POST(HttpRequest.BodyPublishers.ofString(sceneSearchJson))
                        .build();

                HttpResponse<String> sceneSearchResponse = client.send(sceneSearchRequest,
                        HttpResponse.BodyHandlers.ofString());

                // get the data from the response
                JsonNode sceneRoot = objectMapper.readTree(sceneSearchResponse.body());
                JsonNode results = sceneRoot.path("data").path("results");

                if (results.isArray()) {
                    for (JsonNode scene : results) {
                        String entityId = scene.path("entityId").asText();
                        if (!entityId.isEmpty()) {
                            tiles.add(new LandsatTile(datasetName, entityId));
                            System.out.println("Found scene: Dataset = " + datasetName + ", Entity ID = " + entityId);
                        }
                    }
                }
            }
        }

        return tiles;
    }

    @Override
    public void downloadTile(Tile tile) {
        try {
            String datasetName = tile.getParametersForDownload()[0];
            String entityId = tile.getParametersForDownload()[1];
            // GET ACCESS TOKEN

            // Construct the body
            String loginToken = String.format("{\"username\":\"%s\", \"token\":\"%s\"}", propsReader.get("USER"),
                    propsReader.get("LANDSAT_TOKEN"));

            // Make the request
            HttpRequest loginRequest = HttpRequest.newBuilder()
                    .uri(URI.create(propsReader.get("LANDSAT_API") + "login-token"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginToken))
                    .build();

            HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

            // Get the access token from the response
            String accessToken = objectMapper.readTree(loginResponse.body()).path("data").asText();

            // MAKE THE DOWNLOAD

            // Prepare the download body
            ObjectNode downloadRequestBody = objectMapper.createObjectNode();
            downloadRequestBody.put("datasetName", datasetName);
            downloadRequestBody.putArray("entityIds").add(entityId);
            downloadRequestBody.putArray("products").add("STANDARD");

            String requestJson = objectMapper.writeValueAsString(downloadRequestBody);

            // Make the request
            HttpRequest downloadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(propsReader.get("LANDSAT_API") + "download-request"))
                    .header("Content-Type", "application/json")
                    .header("X-Auth-Token", accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> downloadResponse = client.send(downloadRequest, HttpResponse.BodyHandlers.ofString());

            // Get the available url from the response
            JsonNode responseJson = objectMapper.readTree(downloadResponse.body());
            JsonNode available = responseJson.path("data").path("available");

            if (available.isArray() && !available.isEmpty()) {
                // DOWNLOAD THE FILE
                String downloadUrl = available.get(0).path("url").asText();
                System.out.println("Download URL: " + downloadUrl);

                HttpRequest fileRequest = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .build();

                HttpResponse<InputStream> fileResponse = client.send(fileRequest,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (fileResponse.statusCode() == 200) {
                    Path outputPath = Path.of("/tmp/" + entityId + ".zip");
                    try (InputStream input = fileResponse.body();
                            FileOutputStream output = new FileOutputStream(outputPath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        System.out.println("Downloaded successfully to " + outputPath);
                    }
                } else {
                    System.out.println("Failed to download file. HTTP Status: " + fileResponse.statusCode());
                }
            } else {
                System.out.println("No downloadable product available for entity: " + entityId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error downloading tile");
        }
    }

}
