package tfg.satelitedownloader.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SatelliteDownloadTask {

    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("iday")
    private String iday;

    @JsonProperty("fday")
    private String fday;

    @JsonProperty("selectedImagesCount")
    private int selectedImagesCount;

    @JsonProperty("status")
    private volatile String status; // QUEUED, DOWNLOADING, COMPLETED, FAILED, CANCELLED

    @JsonProperty("currentImage")
    private volatile int currentImage;

    @JsonProperty("totalImages")
    private volatile int totalImages;

    @JsonProperty("currentFilename")
    private volatile String currentFilename;

    @JsonProperty("percent")
    private volatile int percent;

    @JsonProperty("message")
    private volatile String message;

    private volatile boolean cancelled = false;

    public SatelliteDownloadTask() {
    }

    public SatelliteDownloadTask(String taskId, String iday, String fday, int selectedImagesCount) {
        this.taskId = taskId;
        this.iday = iday;
        this.fday = fday;
        this.selectedImagesCount = selectedImagesCount;
        this.status = "QUEUED";
        this.currentImage = 0;
        this.totalImages = selectedImagesCount;
        this.currentFilename = "";
        this.percent = 0;
        this.message = "Waiting to queue...";
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public int getSelectedImagesCount() {
        return selectedImagesCount;
    }

    public void setSelectedImagesCount(int selectedImagesCount) {
        this.selectedImagesCount = selectedImagesCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentImage() {
        return currentImage;
    }

    public void setCurrentImage(int currentImage) {
        this.currentImage = currentImage;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public void setTotalImages(int totalImages) {
        this.totalImages = totalImages;
    }

    public String getCurrentFilename() {
        return currentFilename;
    }

    public void setCurrentFilename(String currentFilename) {
        this.currentFilename = currentFilename;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        if (cancelled) {
            this.status = "CANCELLED";
            this.message = "Download cancelled";
        }
    }
}
