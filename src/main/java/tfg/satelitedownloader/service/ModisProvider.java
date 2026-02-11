package tfg.satelitedownloader.service;

import tfg.satelitedownloader.core.Provider;
import tfg.satelitedownloader.core.Tile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModisProvider implements Provider {
    private static final String MODIS_API = "https://ladsweb.modaps.eosdis.nasa.gov/api/v1/files/";
    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public List<Tile> getTile(String name, String dateStart, String dateEnd, String area)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODIS_API + "product=MYD021KM&collection=61&dateRanges=" + dateStart + ".." + dateEnd
                        + "&dayCoverage=true&dnboundCoverage=true&nightCoverage=true&areaOfInterest=" + area))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return new ArrayList<>();
    }

    @Override
    public void downloadTile(Tile tile) {
        String accessToken = "MY_TOKEN"; // Replace with your actual token
        String baseUrl = "https://ladsweb.modaps.eosdis.nasa.gov/archive/allData/PATH_TO_DATA_DIRECTORY/";

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<String> fileUrls = List.of(baseUrl + "example1.hdf", baseUrl + "example2.hdf");
                for (String fileUrl : fileUrls) {
                    HttpClient fileClient = HttpClient.newHttpClient();
                    HttpRequest fileRequest = HttpRequest.newBuilder()
                            .uri(URI.create(fileUrl))
                            .header("Authorization", "Bearer " + accessToken)
                            .GET()
                            .build();

                    HttpResponse<byte[]> fileResponse = fileClient.send(fileRequest,
                            HttpResponse.BodyHandlers.ofByteArray());

                    if (fileResponse.statusCode() == 200) {
                        Path filePath = Path.of("targetDirectory", fileUrl.substring(fileUrl.lastIndexOf("/") + 1));
                        Files.createDirectories(filePath.getParent());
                        Files.write(filePath, fileResponse.body());
                        System.out.println("Downloaded: " + filePath);
                    } else {
                        System.err.println("Failed to download file: " + fileUrl);
                    }
                }
            } else {
                throw new RuntimeException("Failed to fetch directory listing: HTTP " + response.statusCode());
            }
            System.out.println("Download completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}