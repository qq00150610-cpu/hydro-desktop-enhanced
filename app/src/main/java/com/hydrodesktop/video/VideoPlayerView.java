package com.hydrodesktop.video;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * 增强型 VideoView
 * 车机优化：自动适配、错误恢复
 */
public class VideoPlayerView extends VideoView {

    private MediaPlayer.OnPreparedListener onPreparedListener;
    private MediaPlayer.OnCompletionListener onCompletionListener;
    private MediaPlayer.OnErrorListener onErrorListener;
    private MediaPlayer.OnInfoListener onInfoListener;

    public VideoPlayerView(Context context) {
        super(context);
        init();
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        super.setOnPreparedListener(mp -> {
            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            if (onPreparedListener != null) onPreparedListener.onPrepared(mp);
        });

        super.setOnCompletionListener(mp -> {
            if (onCompletionListener != null) onCompletionListener.onCompletion(mp);
        });

        super.setOnErrorListener (mp, what, extra -> {
            if (onErrorListener != null) return onErrorListener.onError(mp, what, extra);
            return true;
        });

        super.setOnInfoListener((mp, what, extra) -> {
            if (onInfoListener != null) return onInfoListener.onInfo(mp, what, extra);
            return false;
        });
    }

    @Override
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    @Override
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    @Override
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        this.onErrorListener = listener;
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener listener) {
        this.onInfoListener = listener;
    }
}
