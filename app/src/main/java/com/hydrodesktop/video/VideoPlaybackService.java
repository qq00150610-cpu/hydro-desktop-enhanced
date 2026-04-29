package com.hydrodesktop.video;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hydrodesktop.R;
import com.hydrodesktop.launcher.MainActivity;

/**
 * 视频后台播放服务
 * 支持切换到后台继续播放音频（如听歌/听播客场景）
 */
public class VideoPlaybackService extends Service {

    private static final String CHANNEL_ID = "video_playback_channel";
    private static final int NOTIFICATION_ID = 2001;

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();
    private boolean isPrepared = false;

    public class LocalBinder extends Binder {
        public VideoPlaybackService getService() {
            return VideoPlaybackService.this;
        }
    }

    public interface OnPreparedListener { void onPrepared(); }
    public interface OnCompletionListener { void onCompletion(); }
    public interface OnErrorListener { void onError(int what, int extra); }

    private OnPreparedListener preparedListener;
    private OnCompletionListener completionListener;
    private OnErrorListener errorListener;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    /**
     * 播放视频/音频 URI
     */
    public void play(Uri uri) {
        release();
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mp.start();
                showNotification();
                if (preparedListener != null) preparedListener.onPrepared();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                if (completionListener != null) completionListener.onCompletion();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                if (errorListener != null) errorListener.onError(what, extra);
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            if (errorListener != null) errorListener.onError(-1, -1);
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) return mediaPlayer.getCurrentPosition();
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) return mediaPlayer.getDuration();
        return 0;
    }

    public void setOnPreparedListener(OnPreparedListener l) { this.preparedListener = l; }
    public void setOnCompletionListener(OnCompletionListener l) { this.completionListener = l; }
    public void setOnErrorListener(OnErrorListener l) { this.errorListener = l; }

    private void showNotification() {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("氢桌面")
                .setContentText("正在播放媒体...")
                .setSmallIcon(R.drawable.ic_music)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "媒体播放", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("视频/音频后台播放");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
    }
}
