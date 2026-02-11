package tfg.satelitedownloader.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the satellite download request payload
 */
public class SatelliteDownloadRequest {

    @JsonProperty("iday")
    private String iday;

    @JsonProperty("fday")
    private String fday;

    @JsonProperty("geojson")
    private String geojson;

    // Default constructor
    public SatelliteDownloadRequest() {
    }

    // Constructor
    public SatelliteDownloadRequest(String iday, String fday, String geojson) {
        this.iday = iday;
        this.fday = fday;
        this.geojson = geojson;
    }

    public String getIday() {
        return iday;
    }

    public void setIday(String iday) {
        this.iday = iday;
    }

    public String getFday() {
        return fday;
    }

    public void setFday(String fday) {
        this.fday = fday;
    }

    public String getGeojson() {
        return geojson;
    }

    public void setGeojson(String geojson) {
        this.geojson = geojson;
    }

    // Compatibility methods for the service layer
    public String getInitialDay() {
        return iday;
    }

    public String getFinalDay() {
        return fday;
    }

    public String getGeoJson() {
        return geojson;
    }

    @Override
    public String toString() {
        return "SatelliteDownloadRequest{" +
                "iday='" + iday + '\'' +
                ", fday='" + fday + '\'' +
                ", geojson='" + geojson + '\'' +
                '}';
    }
}
