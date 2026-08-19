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

    public void setInitialDay(String initialDay) {
        this.iday = initialDay;
    }

    public String getFinalDay() {
        return fday;
    }

    public void setFinalDay(String finalDay) {
        this.fday = finalDay;
    }

    public String getGeoJson() {
        return geojson;
    }

    public void setGeoJson(String geoJson) {
        this.geojson = geoJson;
    }

    private java.util.List<String> selectedImages;

    public java.util.List<String> getSelectedImages() {
        return selectedImages;
    }

    public void setSelectedImages(java.util.List<String> selectedImages) {
        this.selectedImages = selectedImages;
    }

    @Override
    public String toString() {
        return "SatelliteDownloadRequest{" +
                "iday='" + iday + '\'' +
                ", fday='" + fday + '\'' +
                ", geojson='" + geojson + '\'' +
                ", selectedImages=" + selectedImages +
                '}';
    }
}
