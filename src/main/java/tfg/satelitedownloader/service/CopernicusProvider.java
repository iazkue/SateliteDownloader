package tfg.satelitedownloader.service;

import tfg.satelitedownloader.core.Provider;
import tfg.satelitedownloader.db.CopernicusTileDAO;
import tfg.satelitedownloader.core.Tile;
import tfg.satelitedownloader.model.CopernicusTile;
import tfg.satelitedownloader.db.CopernicusTileEntity;
import tfg.satelitedownloader.util.propsReader;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CopernicusProvider implements Provider {

    private final CopernicusTileDAO tileDAO;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedAccessToken;
    private Instant tokenExpiresAt = Instant.MIN;
    private final Object tokenLock = new Object();

    public CopernicusProvider() {
        this(null);
    }

    public CopernicusProvider(CopernicusTileDAO tileDAO) {
        this.tileDAO = tileDAO;
    }

    @Override
    public List<Tile> getTile(String name, String dateStart, String dateEnd, String area)
            throws IOException, InterruptedException {
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String encodedDateStart = URLEncoder.encode(dateStart, StandardCharsets.UTF_8);
        String encodedDateEnd = URLEncoder.encode(dateEnd, StandardCharsets.UTF_8);
        String encodedArea = URLEncoder.encode(area, StandardCharsets.UTF_8);

        String query = String.format(
                "?$filter=Collection/Name eq '%s' and OData.CSC.Intersects(area=geography'%s') and ContentDate/Start gt %s and ContentDate/Start lt %s&$expand=Assets&$top=1000",
                encodedName, encodedArea, encodedDateStart, encodedDateEnd);

        query = query.replace(" ", "%20");

        String url = propsReader.get("COPERNICUS_API") + query;

        System.out.println(url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        List<Tile> tiles = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(response.body());
        JsonNode valueNode = rootNode.path("value");

        System.out.println(valueNode);
        if (valueNode.isArray()) {
            for (JsonNode productNode : valueNode) {
                String productId = productNode.path("Id").asText();
                String productName = productNode.path("Name").asText();

                // Extract MD5 hash from Checksum array
                String md5Hash = null;
                JsonNode checksumArray = productNode.path("Checksum");
                if (checksumArray.isArray()) {
                    for (JsonNode checksumNode : checksumArray) {
                        if ("MD5".equals(checksumNode.path("Algorithm").asText())) {
                            md5Hash = checksumNode.path("Value").asText();
                            break;
                        }
                    }
                }

                // Extract preview link from Assets
                String previewLink = null;
                JsonNode assetsArray = productNode.path("Assets");
                if (assetsArray.isArray()) {
                    for (JsonNode assetNode : assetsArray) {
                        if ("QUICKLOOK".equals(assetNode.path("Type").asText())) {
                            previewLink = assetNode.path("DownloadLink").asText();
                            break;
                        }
                    }
                }

                tiles.add(new CopernicusTile(
                        productId,
                        productName,
                        md5Hash,
                        dateStart,
                        dateEnd,
                        area,
                        previewLink));
                System.out.println("Product ID: " + productId + ", Name: " + productName + ", MD5: " + md5Hash
                        + ", Preview: " + previewLink);
            }
        }

        return tiles;
    }

    @Override
    public void downloadTile(Tile tile) throws IOException, InterruptedException {
        if (tileDAO != null) {
            String tileProductId = tile.getParametersForDownload()[0];
            boolean alreadyDownloaded = tileDAO.findByProductId(tileProductId).isPresent();
            if (alreadyDownloaded) {
                System.out.println("Tile ya descargada: " + tileProductId);
                return;
            }
        }

        String accessToken = getAccessToken();
        downloadFile(accessToken, (CopernicusTile) tile);
    }

    private void downloadFile(String token, CopernicusTile tile) throws IOException, InterruptedException {
        String productId = tile.getParametersForDownload()[0];
        String downloadUrl = propsReader.get("COPERNICUS_API")
                .replace("catalogue.dataspace.copernicus.eu", "download.dataspace.copernicus.eu")
                + "(" + productId + ")/$value";
        String outputFile = tile.getName() + ".zip";
        String outputDirectory = propsReader.get("COPERNICUS_FOLDER");
        Path outputPath = Paths.get(outputDirectory, outputFile);
        Files.createDirectories(outputPath.getParent());
        String expectedMd5 = tile.getMd5Hash();

        System.out.println("Descargando: " + downloadUrl + " -> " + outputPath);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "curl",
                "-f",
                "-sS",
                "--location-trusted",
                "--connect-timeout", "30",
                "--speed-limit", "1024",
                "--speed-time", "60",
                "-H", "Authorization: Bearer " + token,
                downloadUrl,
                "-o", outputPath.toString());

        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = processBuilder.start();

        StringBuilder errLog = new StringBuilder();
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
                errLog.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("-------- Descargado correctamente. Comprobando MD5 --------");

            // Verify MD5 checksum
            if (expectedMd5 != null && !expectedMd5.isEmpty()) {
                String actualMd5 = calculateMD5(outputPath.toString());
                if (expectedMd5.equalsIgnoreCase(actualMd5)) {
                    System.out.println("MD5 correcto!");
                    System.out.println("Esperado: " + expectedMd5);
                    System.out.println("Actual:   " + actualMd5);
                    persistSuccessfulTile(tile, outputPath, actualMd5);
                } else {
                    System.err.println("MD5 incorrecto!");
                    System.err.println("Esperado: " + expectedMd5);
                    System.err.println("Actual:   " + actualMd5);
                    try {
                        Files.deleteIfExists(outputPath);
                        System.err.println("Archivo eliminado por MD5 incorrecto: " + outputPath);
                    } catch (IOException e) {
                        System.err.println("No se pudo eliminar el archivo corrupto: " + outputPath);
                    }
                    throw new IOException("MD5 checksum mismatch for file: " + outputPath + " (Esperado: " + expectedMd5 + ", Actual: " + actualMd5 + ")");
                }
            } else {
                System.out.println("No hay MD5 disponible para verificar");
            }
        } else {
            System.err.println("Descarga fallida con código de salida: " + exitCode + ". Error: " + errLog);
            throw new IOException("Descarga fallida (curl exit code " + exitCode + "): " + errLog);
        }
    }

    private void persistSuccessfulTile(CopernicusTile tile, Path outputPath, String actualMd5) {
        if (tileDAO == null) {
            return;
        }

        try {
            Long fileSize = Files.exists(outputPath) ? Files.size(outputPath) : null;
            CopernicusTileEntity entity = new CopernicusTileEntity(
                    tile.getParametersForDownload()[0],
                    tile.getName(),
                    actualMd5,
                    outputPath.toString(),
                    tile.getDateStart(),
                    tile.getDateEnd(),
                    tile.getArea(),
                    tile.getPreviewLink(),
                    Instant.now(),
                    fileSize);
            tileDAO.save(entity);
            System.out.println("Tile persisted in database: " + tile.getName());
        } catch (IOException e) {
            System.err.println("Unable to capture file metadata for persistence: " + e.getMessage());
        }
    }

    private static String calculateMD5(String filePath) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(Paths.get(filePath));
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[65536]; // 64 KB buffer instead of loading multi-GB file into heap
                while (dis.read(buffer) != -1) {
                    // dis updates md automatically
                }
            }
            byte[] digest = md.digest();

            // Convert byte array to hex string
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }

    /**
     * Downloads a preview image from the given preview link URL.
     * 
     * @param previewLink The URL to the preview image
     * @param accessToken The access token for authorization
     * @param outputPath  The path where to save the preview image
     * @throws IOException          If the download fails
     * @throws InterruptedException If the request is interrupted
     */
    public void downloadPreviewImage(String previewLink, String accessToken, String outputPath)
            throws IOException, InterruptedException {

        if (previewLink == null || previewLink.isEmpty()) {
            System.out.println("Preview link is null or empty, skipping download");
            return;
        }

        String targetUrl = previewLink.replace("catalogue.dataspace.copernicus.eu", "download.dataspace.copernicus.eu");
        String token = (accessToken != null && !accessToken.isEmpty()) ? accessToken : getAccessToken();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream body = response.body()) {
                if (response.statusCode() == 200) {
                    Path outputFilePath = Paths.get(outputPath);
                    if (outputFilePath.getParent() != null) {
                        Files.createDirectories(outputFilePath.getParent());
                    }

                    Files.copy(
                            body,
                            outputFilePath,
                            StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Preview image downloaded successfully to: " + outputPath);
                } else if (response.statusCode() == 401) {
                    System.err.println("Preview download unauthorized (401). Token expired, retrying with fresh token...");
                    invalidateToken();
                    String freshToken = getAccessToken();

                    HttpRequest retryRequest = HttpRequest.newBuilder()
                            .uri(URI.create(targetUrl))
                            .header("Authorization", "Bearer " + freshToken)
                            .GET()
                            .build();

                    HttpResponse<InputStream> retryResponse = client.send(retryRequest, HttpResponse.BodyHandlers.ofInputStream());
                    try (InputStream retryBody = retryResponse.body()) {
                        if (retryResponse.statusCode() == 200) {
                            Path outputFilePath = Paths.get(outputPath);
                            if (outputFilePath.getParent() != null) {
                                Files.createDirectories(outputFilePath.getParent());
                            }
                            Files.copy(retryBody, outputFilePath, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Preview image downloaded successfully after token refresh to: " + outputPath);
                        } else {
                            System.err.println("Retry preview download failed. Status: " + retryResponse.statusCode());
                        }
                    }
                } else {
                    System.err.println("Failed to download preview image. Status code: " + response.statusCode());
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error downloading preview image: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Invalidates the cached access token, forcing the next call to getAccessToken() to fetch a fresh token.
     */
    public void invalidateToken() {
        synchronized (tokenLock) {
            this.cachedAccessToken = null;
            this.tokenExpiresAt = Instant.MIN;
        }
    }

    /**
     * Gets an access token for Copernicus API authentication, with caching and expiration tracking.
     * 
     * @return The access token
     * @throws IOException          If the token request fails
     * @throws InterruptedException If the request is interrupted
     */
    public String getAccessToken() throws IOException, InterruptedException {
        synchronized (tokenLock) {
            // Return cached token if valid for at least another 60 seconds
            if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
                return cachedAccessToken;
            }

            String username = propsReader.get("COPERNICUS_USERNAME");
            String password = propsReader.get("COPERNICUS_PASSWORD");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new IOException("Faltan las credenciales COPERNICUS_USERNAME / COPERNICUS_PASSWORD en config.properties o .env");
            }

            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);

            String formData = "grant_type=password" +
                    "&client_id=cdse-public" +
                    "&username=" + encodedUsername +
                    "&password=" + encodedPassword;

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(propsReader.get("COPERNICUS_TOKEN")))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            if (tokenResponse.statusCode() != 200) {
                System.err.println("Fallo al obtener token de Copernicus (HTTP " + tokenResponse.statusCode() + "): " + tokenResponse.body());
                throw new IOException("Fallo en la autenticación de Copernicus (HTTP " + tokenResponse.statusCode() + "): " + tokenResponse.body());
            }

            JsonNode rootNode = objectMapper.readTree(tokenResponse.body());
            String accessToken = rootNode.path("access_token").asText();

            if (accessToken == null || accessToken.isEmpty()) {
                throw new IOException("La respuesta del token de Copernicus no incluye access_token: " + tokenResponse.body());
            }

            long expiresInSeconds = rootNode.path("expires_in").asLong(600);
            this.cachedAccessToken = accessToken;
            this.tokenExpiresAt = Instant.now().plusSeconds(expiresInSeconds);

            System.out.println("Access token obtenido correctamente (válido por " + expiresInSeconds + "s).");
            return accessToken;
        }
    }

}