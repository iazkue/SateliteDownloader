package tfg.satelitedownloader.api;

/**
 * DTO for the satellite download response
 */
public class SatelliteDownloadResponse {

    private String status;
    private String message;
    private int tilesFound;
    private String downloadedTile;

    // Default constructor
    public SatelliteDownloadResponse() {
    }

    // Constructor
    public SatelliteDownloadResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    // Constructor with tiles info
    public SatelliteDownloadResponse(String status, String message, int tilesFound, String downloadedTile) {
        this.status = status;
        this.message = message;
        this.tilesFound = tilesFound;
        this.downloadedTile = downloadedTile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTilesFound() {
        return tilesFound;
    }

    public void setTilesFound(int tilesFound) {
        this.tilesFound = tilesFound;
    }

    public String getDownloadedTile() {
        return downloadedTile;
    }

    public void setDownloadedTile(String downloadedTile) {
        this.downloadedTile = downloadedTile;
    }
}
