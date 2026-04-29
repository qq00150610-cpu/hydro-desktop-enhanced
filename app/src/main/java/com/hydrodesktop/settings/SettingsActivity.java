package com.hydrodesktop.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hydrodesktop.R;
import com.hydrodesktop.util.AIHelper;
import com.hydrodesktop.util.CarUtils;

/**
 * 设置界面
 * AI API 配置、语音设置、车机个性化设置
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText etApiUrl;
    private EditText etApiKey;
    private EditText etModel;
    private EditText etSystemPrompt;
    private EditText etWakeWord;
    private SeekBar seekBrightness;
    private SeekBar seekVolume;
    private TextView tvBrightnessValue;
    private TextView tvVolumeValue;
    private Switch switchAutoStart;
    private Switch switchVoiceWake;
    private Switch switchTTS;
    private Button btnSaveAI;
    private Button btnTestAI;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        loadCurrentSettings();
        setupListeners();
    }

    private void initViews() {
        etApiUrl = findViewById(R.id.et_api_url);
        etApiKey = findViewById(R.id.et_api_key);
        etModel = findViewById(R.id.et_model);
        etSystemPrompt = findViewById(R.id.et_system_prompt);
        etWakeWord = findViewById(R.id.et_wake_word);

        seekBrightness = findViewById(R.id.seek_brightness);
        seekVolume = findViewById(R.id.seek_volume);
        tvBrightnessValue = findViewById(R.id.tv_brightness_value);
        tvVolumeValue = findViewById(R.id.tv_volume_value);

        switchAutoStart = findViewById(R.id.switch_auto_start);
        switchVoiceWake = findViewById(R.id.switch_voice_wake);
        switchTTS = findViewById(R.id.switch_tts);

        btnSaveAI = findViewById(R.id.btn_save_ai);
        btnTestAI = findViewById(R.id.btn_test_ai);
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadCurrentSettings() {
        // AI 设置
        AIHelper helper = new AIHelper(this);
        etApiUrl.setText(helper.getApiUrl());
        etApiKey.setText(helper.getApiKey());
        etModel.setText(helper.getModel());
        etSystemPrompt.setText(helper.getSystemPrompt());

        // 语音设置
        SharedPreferences voicePrefs = getSharedPreferences("voice_settings", MODE_PRIVATE);
        etWakeWord.setText(voicePrefs.getString("wake_word", "你好氢桌面"));

        // 系统设置
        SharedPreferences carPrefs = getSharedPreferences("car_settings", MODE_PRIVATE);
        switchAutoStart.setChecked(carPrefs.getBoolean("auto_start", true));
        switchVoiceWake.setChecked(carPrefs.getBoolean("voice_wake", true));
        switchTTS.setChecked(carPrefs.getBoolean("tts_enabled", true));

        // 亮度
        int brightness = CarUtils.getScreenBrightness(this);
        seekBrightness.setProgress(brightness);
        tvBrightnessValue.setText(String.valueOf(brightness));

        // 音量
        int volume = CarUtils.getVolume(this);
        int maxVolume = CarUtils.getMaxVolume(this);
        seekVolume.setMax(maxVolume);
        seekVolume.setProgress(volume);
        tvVolumeValue.setText(volume + "/" + maxVolume);
    }

    private void setupListeners() {
        // 亮度调节
        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvBrightnessValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                CarUtils.setScreenBrightness(SettingsActivity.this, seekBar.getProgress());
            }
        });

        // 音量调节
        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int max = CarUtils.getMaxVolume(SettingsActivity.this);
                tvVolumeValue.setText(progress + "/" + max);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                CarUtils.setVolume(SettingsActivity.this, seekBar.getProgress());
            }
        });

        // 保存 AI 设置
        btnSaveAI.setOnClickListener(v -> saveAISettings());

        // 测试 AI 连接
        btnTestAI.setOnClickListener(v -> testAIConnection());

        // 开机自启
        switchAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CarUtils.saveBoolSetting(this, "auto_start", isChecked);
        });

        // 语音唤醒
        switchVoiceWake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CarUtils.saveBoolSetting(this, "voice_wake", isChecked);
        });

        // TTS 语音播报
        switchTTS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CarUtils.saveBoolSetting(this, "tts_enabled", isChecked);
        });

        // 唤醒词保存
        etWakeWord.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String wakeWord = etWakeWord.getText().toString().trim();
                if (!TextUtils.isEmpty(wakeWord)) {
                    SharedPreferences.Editor editor = getSharedPreferences("voice_settings", MODE_PRIVATE).edit();
                    editor.putString("wake_word", wakeWord);
                    editor.apply();
                }
            }
        });

        // 返回
        btnBack.setOnClickListener(v -> finish());
    }

    private void saveAISettings() {
        String apiUrl = etApiUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String model = etModel.getText().toString().trim();
        String systemPrompt = etSystemPrompt.getText().toString().trim();

        if (TextUtils.isEmpty(apiUrl)) {
            etApiUrl.setError("API 地址不能为空");
            return;
        }

        AIHelper.saveSettings(this, apiUrl, apiKey, model, systemPrompt);
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }

    private void testAIConnection() {
        saveAISettings();
        Toast.makeText(this, R.string.testing_connection, Toast.LENGTH_SHORT).show();

        AIHelper helper = new AIHelper(this);
        helper.chat("你好，请回复'连接成功'来确认通信正常", new AIHelper.AIResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                        "✅ " + getString(R.string.connection_success) + ": " + response,
                        Toast.LENGTH_LONG).show());
                helper.shutdown();
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                        "❌ " + getString(R.string.connection_failed) + ": " + error,
                        Toast.LENGTH_LONG).show());
                helper.shutdown();
            }
        });
    }
}
