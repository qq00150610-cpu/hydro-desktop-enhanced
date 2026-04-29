package com.hydrodesktop.video;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hydrodesktop.R;

/**
 * 全屏视频播放器
 * 车机优化：大按钮、简洁控件、自动隐藏
 */
public class VideoPlayerActivity extends AppCompatActivity {

    private VideoPlayerView videoView;
    private View controlOverlay;
    private ImageButton btnPlayPause;
    private ImageButton btnBack;
    private ImageButton btnFullscreen;
    private TextView tvTitle;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar seekBar;
    private ProgressBar loadingSpinner;
    private View topBar;
    private View bottomBar;

    private Handler handler;
    private boolean isPlaying = false;
    private boolean controlsVisible = true;
    private boolean isUserSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        setupFullScreen();
        initViews();
        loadVideo();
        setupAutoHide();
    }

    private void setupFullScreen() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void initViews() {
        videoView = findViewById(R.id.video_view);
        controlOverlay = findViewById(R.id.control_overlay);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnBack = findViewById(R.id.btn_video_back);
        btnFullscreen = findViewById(R.id.btn_fullscreen);
        tvTitle = findViewById(R.id.tv_video_title);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        seekBar = findViewById(R.id.seek_bar);
        loadingSpinner = findViewById(R.id.loading_spinner);
        topBar = findViewById(R.id.top_bar);
        bottomBar = findViewById(R.id.bottom_bar);

        handler = new Handler(Looper.getMainLooper());

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 播放/暂停
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        // 全屏切换
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());

        // 进度条拖动
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                videoView.seekTo(seekBar.getProgress());
            }
        });

        // 点击视频区域切换控件显示
        videoView.setOnClickListener(v -> toggleControls());

        // VideoView 回调
        videoView.setOnPreparedListener(mp -> {
            loadingSpinner.setVisibility(View.GONE);
            seekBar.setMax(mp.getDuration());
            tvTotalTime.setText(formatTime(mp.getDuration()));
            mp.start();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            updateProgress();
        });

        videoView.setOnCompletionListener(mp -> {
            isPlaying = false;
            btnPlayPause.setImageResource(R.drawable.ic_play);
            showControls();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            loadingSpinner.setVisibility(View.GONE);
            tvTitle.setText(R.string.video_play_error);
            return true;
        });

        videoView.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                loadingSpinner.setVisibility(View.VISIBLE);
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                loadingSpinner.setVisibility(View.GONE);
            }
            return false;
        });
    }

    private void loadVideo() {
        Intent intent = getIntent();
        String uriStr = intent.getStringExtra("uri");
        String title = intent.getStringExtra("title");

        if (title != null) {
            tvTitle.setText(title);
        }

        if (uriStr != null) {
            loadingSpinner.setVisibility(View.VISIBLE);
            videoView.setVideoURI(Uri.parse(uriStr));
        } else {
            tvTitle.setText(R.string.video_no_uri);
        }
    }

    /**
     * 播放/暂停切换
     */
    private void togglePlayPause() {
        if (isPlaying) {
            videoView.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play);
            isPlaying = false;
            showControls();
        } else {
            videoView.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            isPlaying = true;
            resetAutoHide();
        }
    }

    /**
     * 更新进度
     */
    private void updateProgress() {
        if (!isUserSeeking && videoView.isPlaying()) {
            int current = videoView.getCurrentPosition();
            seekBar.setProgress(current);
            tvCurrentTime.setText(formatTime(current));
        }
        handler.postDelayed(this::updateProgress, 500);
    }

    /**
     * 切换控件显示
     */
    private void toggleControls() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        controlsVisible = true;
        topBar.animate().alpha(1f).translationY(0).setDuration(200).start();
        bottomBar.animate().alpha(1f).translationY(0).setDuration(200).start();
        resetAutoHide();
    }

    private void hideControls() {
        controlsVisible = false;
        topBar.animate().alpha(0f).translationY(-topBar.getHeight()).setDuration(200).start();
        bottomBar.animate().alpha(0f).translationY(bottomBar.getHeight()).setDuration(200).start();
    }

    private void setupAutoHide() {
        resetAutoHide();
    }

    private void resetAutoHide() {
        handler.removeCallbacksAndMessages(null);
        if (isPlaying) {
            handler.postDelayed(this::hideControls, 4000);
        }
    }

    private void toggleFullscreen() {
        if (getResources().getConfiguration().orientation == 1) {
            // 竖屏 → 横屏
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    /**
     * 格式化时间 mm:ss
     */
    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
            isPlaying = false;
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (videoView != null) videoView.stopPlayback();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setupFullScreen();
    }
}
