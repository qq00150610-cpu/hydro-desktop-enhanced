package com.hydrodesktop.launcher;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hydrodesktop.R;
import com.hydrodesktop.ai.AIAssistantActivity;
import com.hydrodesktop.settings.SettingsActivity;
import com.hydrodesktop.video.VideoListActivity;
import com.hydrodesktop.voice.VoiceRecognitionService;
import com.hydrodesktop.widget.WeatherWidget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 氢桌面增强版 v2 - 主界面
 * 左侧信息面板 + 中央功能卡片 + 底部快捷Dock
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
    };

    // 信息面板
    private TextView tvTime;
    private TextView tvSeconds;
    private TextView tvDate;
    private TextView tvGreeting;
    private TextView tvWeatherIcon;
    private TextView tvWeatherTemp;
    private TextView tvWeatherDesc;
    private TextView tvWeatherDetail;

    // 功能卡片
    private View btnAIAssistant;
    private View btnVoiceControl;
    private View btnNavigation;
    private View btnMusic;
    private View btnPhone;
    private View btnMedia;
    private View btnApps;

    // 底部 Dock
    private LinearLayout dockNav;
    private LinearLayout dockMusic;
    private LinearLayout dockPhone;
    private LinearLayout dockAI;
    private LinearLayout dockSettings;

    // 设置按钮
    private ImageButton btnSettings;

    // 语音状态
    private LinearLayout voiceStatusBar;
    private TextView voiceStatusText;
    private View voiceCardBg;

    private Handler clockHandler;
    private boolean voiceActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupFullScreen();
        setContentView(R.layout.activity_main);
        requestPermissions();
        initViews();
        startClock();
        animateEntrance();
    }

    private void setupFullScreen() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void initViews() {
        // 信息面板
        tvTime = findViewById(R.id.tv_time);
        tvSeconds = findViewById(R.id.tv_seconds);
        tvDate = findViewById(R.id.tv_date);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvWeatherIcon = findViewById(R.id.tv_weather_icon);
        tvWeatherTemp = findViewById(R.id.tv_weather_temp);
        tvWeatherDesc = findViewById(R.id.tv_weather_desc);
        tvWeatherDetail = findViewById(R.id.tv_weather_detail);

        // 功能卡片
        btnAIAssistant = findViewById(R.id.btn_ai_assistant);
        btnVoiceControl = findViewById(R.id.btn_voice_control);
        btnNavigation = findViewById(R.id.btn_navigation);
        btnMusic = findViewById(R.id.btn_music);
        btnPhone = findViewById(R.id.btn_phone);
        btnMedia = findViewById(R.id.btn_media);
        btnApps = findViewById(R.id.btn_apps);

        // Dock
        dockNav = findViewById(R.id.dock_nav);
        dockMusic = findViewById(R.id.dock_music);
        dockPhone = findViewById(R.id.dock_phone);
        dockAI = findViewById(R.id.dock_ai);
        dockSettings = findViewById(R.id.dock_settings);

        // 设置
        btnSettings = findViewById(R.id.btn_settings);

        // 语音状态
        voiceStatusBar = findViewById(R.id.voice_status_bar);
        voiceStatusText = findViewById(R.id.voice_status_text);
        voiceCardBg = findViewById(R.id.voice_card_bg);

        // ===== 点击事件 =====

        // AI 助手
        View.OnClickListener aiClick = v -> {
            startActivity(new Intent(this, AIAssistantActivity.class));
            overridePendingTransition(R.anim.slide_up, R.anim.no_anim);
        };
        btnAIAssistant.setOnClickListener(aiClick);
        dockAI.setOnClickListener(aiClick);

        // 语音控制
        btnVoiceControl.setOnClickListener(v -> toggleVoiceControl());

        // 导航
        View.OnClickListener navClick = v -> launchNavigation();
        btnNavigation.setOnClickListener(navClick);
        dockNav.setOnClickListener(navClick);

        // 音乐
        View.OnClickListener musicClick = v -> launchMusic();
        btnMusic.setOnClickListener(musicClick);
        dockMusic.setOnClickListener(musicClick);

        // 电话
        View.OnClickListener phoneClick = v -> launchPhone();
        btnPhone.setOnClickListener(phoneClick);
        dockPhone.setOnClickListener(phoneClick);

        // 视频
        View.OnClickListener videoClick = v -> {
            startActivity(new Intent(this, VideoListActivity.class));
        };
        btnMedia.setOnClickListener(videoClick);

        // 应用抽屉
        btnApps.setOnClickListener(v -> {
            // 显示所有应用
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
        });

        // 设置
        View.OnClickListener settingsClick = v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        };
        btnSettings.setOnClickListener(settingsClick);
        dockSettings.setOnClickListener(settingsClick);

        // 天气刷新
        findViewById(R.id.btn_weather).setOnClickListener(v -> refreshWeather());

        updateGreeting();
        refreshWeather();
    }

    /**
     * 入场动画
     */
    private void animateEntrance() {
        // 卡片依次淡入
        View[] cards = {btnAIAssistant, btnVoiceControl, btnNavigation, btnMusic, btnPhone, btnMedia, btnApps};
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            card.setAlpha(0f);
            card.setTranslationY(40f);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(100 + i * 60)
                    .setDuration(400)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // 时间淡入
        tvTime.setAlpha(0f);
        tvTime.animate().alpha(1f).setStartDelay(50).setDuration(600).start();
    }

    /**
     * 切换语音控制
     */
    private void toggleVoiceControl() {
        if (!voiceActive) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startVoiceService();
                voiceActive = true;
                voiceStatusBar.setVisibility(View.VISIBLE);
                voiceStatusText.setText(R.string.voice_listening);
                Toast.makeText(this, R.string.voice_activated, Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            }
        } else {
            stopVoiceService();
            voiceActive = false;
            voiceStatusBar.setVisibility(View.GONE);
            voiceStatusText.setText(R.string.voice_control_desc);
            Toast.makeText(this, R.string.voice_deactivated, Toast.LENGTH_SHORT).show();
        }
    }

    private void startVoiceService() {
        Intent serviceIntent = new Intent(this, VoiceRecognitionService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopVoiceService() {
        Intent serviceIntent = new Intent(this, VoiceRecognitionService.class);
        stopService(serviceIntent);
    }

    private void launchNavigation() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.autonavi.minimap");
            if (intent != null) { startActivity(intent); return; }
            intent = getPackageManager().getLaunchIntentForPackage("com.baidu.BaiduMap");
            if (intent != null) { startActivity(intent); return; }
            intent = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
            if (intent != null) { startActivity(intent); return; }
            Toast.makeText(this, R.string.no_navigation_app, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchMusic() {
        try {
            Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(android.provider.MediaStore.INTENT_ACTION_MUSIC_PLAYER));
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_music_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchPhone() {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL));
        } catch (Exception e) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 刷新天气
     */
    private void refreshWeather() {
        WeatherWidget.fetchWeather(new WeatherWidget.WeatherCallback() {
            @Override
            public void onWeatherLoaded(String weatherText) {
                runOnUiThread(() -> {
                    // 解析并分别显示
                    tvWeatherDesc.setText(weatherText);
                    tvWeatherTemp.setText("--°C");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvWeatherDesc.setText("点击刷新天气");
                    tvWeatherTemp.setText("--°C");
                });
            }
        });
    }

    /**
     * 时钟
     */
    private void startClock() {
        clockHandler = new Handler(Looper.getMainLooper());
        clockHandler.post(new Runnable() {
            @Override
            public void run() {
                updateClock();
                clockHandler.postDelayed(this, 1000);
            }
        });
    }

    private void updateClock() {
        Date now = new Date();
        tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        tvSeconds.setText(new SimpleDateFormat("ss", Locale.getDefault()).format(now));
        tvDate.setText(new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA).format(now));
    }

    private void updateGreeting() {
        int hour = new Date().getHours();
        String greeting;
        if (hour < 6) greeting = getString(R.string.greeting_night);
        else if (hour < 12) greeting = getString(R.string.greeting_morning);
        else if (hour < 14) greeting = getString(R.string.greeting_noon);
        else if (hour < 18) greeting = getString(R.string.greeting_afternoon);
        else greeting = getString(R.string.greeting_evening);
        tvGreeting.setText(greeting);
    }

    private void requestPermissions() {
        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setupFullScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null) clockHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        // 车机桌面不响应返回键
    }
}
