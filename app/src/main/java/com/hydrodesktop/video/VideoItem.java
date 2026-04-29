package com.hydrodesktop.video;

import android.net.Uri;

/**
 * 视频列表项数据模型
 */
public class VideoItem {

    private Uri uri;
    private String title;
    private long duration;   // 毫秒
    private long size;       // 字节
    private String path;

    public VideoItem(Uri uri, String title, long duration, long size, String path) {
        this.uri = uri;
        this.title = title;
        this.duration = duration;
        this.size = size;
        this.path = path;
    }

    public Uri getUri() { return uri; }
    public String getTitle() { return title; }
    public long getDuration() { return duration; }
    public long getSize() { return size; }
    public String getPath() { return path; }

    /**
     * 格式化时长 mm:ss 或 h:mm:ss
     */
    public String getFormattedDuration() {
        long totalSeconds = duration / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 格式化文件大小
     */
    public String getFormattedSize() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
