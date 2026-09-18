package tfg.satelitedownloader.service;

import tfg.satelitedownloader.model.SatelliteDownloadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadQueueManager {

    private static final DownloadQueueManager INSTANCE = new DownloadQueueManager();

    private final Map<String, SatelliteDownloadTask> tasks = new ConcurrentHashMap<>();

    private DownloadQueueManager() {
    }

    public static DownloadQueueManager getInstance() {
        return INSTANCE;
    }

    public SatelliteDownloadTask createTask(String iday, String fday, int selectedImagesCount) {
        pruneOldTasks();
        String taskId = "task_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        SatelliteDownloadTask task = new SatelliteDownloadTask(taskId, iday, fday, selectedImagesCount);
        tasks.put(taskId, task);
        return task;
    }

    public void registerTask(SatelliteDownloadTask task) {
        if (task != null && task.getTaskId() != null) {
            pruneOldTasks();
            tasks.put(task.getTaskId(), task);
        }
    }

    private void pruneOldTasks() {
        if (tasks.size() > 50) {
            for (Map.Entry<String, SatelliteDownloadTask> entry : tasks.entrySet()) {
                String status = entry.getValue().getStatus();
                if ("COMPLETED".equals(status) || "CANCELLED".equals(status) || "FAILED".equals(status)) {
                    tasks.remove(entry.getKey());
                    if (tasks.size() <= 30) {
                        break;
                    }
                }
            }
        }
    }

    public SatelliteDownloadTask getTask(String taskId) {
        return taskId != null ? tasks.get(taskId) : null;
    }

    public List<SatelliteDownloadTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public void cancelTask(String taskId) {
        SatelliteDownloadTask task = getTask(taskId);
        if (task != null) {
            task.setCancelled(true);
        }
    }

    public void updateProgress(String taskId, String status, int currentImage, int totalImages, String currentFilename, int percent, String message) {
        SatelliteDownloadTask task = getTask(taskId);
        if (task != null && !task.isCancelled()) {
            if (status != null) task.setStatus(status);
            task.setCurrentImage(currentImage);
            task.setTotalImages(totalImages);
            if (currentFilename != null) task.setCurrentFilename(currentFilename);
            task.setPercent(percent);
            if (message != null) task.setMessage(message);
        }
    }
}
