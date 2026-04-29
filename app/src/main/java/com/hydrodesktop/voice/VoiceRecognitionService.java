package com.hydrodesktop.voice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hydrodesktop.R;
import com.hydrodesktop.ai.AIAssistantActivity;
import com.hydrodesktop.launcher.MainActivity;
import com.hydrodesktop.util.AIHelper;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 语音识别服务
 * 持续监听语音指令，支持唤醒词和命令解析
 */
public class VoiceRecognitionService extends Service {

    private static final String TAG = "VoiceService";
    private static final String CHANNEL_ID = "voice_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    private static final String PREF_NAME = "voice_settings";
    private static final String PREF_WAKE_WORD = "wake_word";
    private static final String DEFAULT_WAKE_WORD = "你好氢桌面";

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private AIHelper aiHelper;
    private boolean isListening = false;
    private boolean ttsReady = false;
    private String wakeWord;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        aiHelper = new AIHelper(this);
        loadSettings();
        initTTS();
        initSpeechRecognizer();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        wakeWord = prefs.getString(PREF_WAKE_WORD, DEFAULT_WAKE_WORD);
    }

    /**
     * 初始化 TTS 语音合成
     */
    private void initTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.CHINA);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.US);
                }
                textToSpeech.setSpeechRate(1.1f); // 车机语速稍快
                ttsReady = true;
                startListening();
            }
        });

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // TTS 开始播放时暂停监听，避免回声
                stopListening();
            }

            @Override
            public void onDone(String utteranceId) {
                // TTS 播放完毕后恢复监听
                startListeningDelayed(500);
            }

            @Override
            public void onError(String utteranceId) {
                startListeningDelayed(500);
            }
        });
    }

    /**
     * 初始化语音识别器
     */
    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "设备不支持语音识别");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "准备就绪，请说话...");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "检测到语音输入");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "语音输入结束");
            }

            @Override
            public void onError(int error) {
                String errorMsg;
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMsg = "未识别到语音";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        errorMsg = "语音输入超时";
                        break;
                    case SpeechRecognizer.ERROR_AUDIO:
                        errorMsg = "音频错误";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMsg = "网络错误";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        errorMsg = "网络超时";
                        break;
                    default:
                        errorMsg = "错误码: " + error;
                        break;
                }
                Log.w(TAG, "语音识别错误: " + errorMsg);
                // 自动重新开始监听
                startListeningDelayed(1000);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    Log.d(TAG, "识别结果: " + text);
                    processVoiceCommand(text);
                }
                // 继续监听
                startListeningDelayed(500);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    String text = partial.get(0);
                    // 检查唤醒词
                    if (text.contains(wakeWord)) {
                        Log.d(TAG, "检测到唤醒词!");
                        speak("我在听，请说");
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    /**
     * 开始监听
     */
    private void startListening() {
        if (speechRecognizer == null || isListening) return;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        // 持续监听模式
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);

        try {
            speechRecognizer.startListening(intent);
            isListening = true;
        } catch (Exception e) {
            Log.e(TAG, "启动语音识别失败", e);
        }
    }

    /**
     * 停止监听
     */
    private void stopListening() {
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.stopListening();
                isListening = false;
            } catch (Exception e) {
                Log.e(TAG, "停止语音识别失败", e);
            }
        }
    }

    /**
     * 延迟开始监听
     */
    private void startListeningDelayed(long delayMs) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                this::startListening, delayMs);
    }

    /**
     * 处理语音指令
     */
    private void processVoiceCommand(String command) {
        command = command.trim().toLowerCase();

        // 1. 检查唤醒词
        if (command.contains(wakeWord)) {
            speak("你好，我是氢助手，有什么可以帮你的吗？");
            return;
        }

        // 2. 导航指令
        if (command.contains("导航") || command.contains("去") || command.contains("怎么走")) {
            String destination = extractDestination(command);
            if (destination != null) {
                speak("正在为您导航到" + destination);
                launchNavigation(destination);
            } else {
                speak("请问您要去哪里？");
            }
            return;
        }

        // 3. 音乐指令
        if (command.contains("播放") || command.contains("放一首") || command.contains("听")) {
            if (command.contains("音乐") || command.contains("歌")) {
                speak("正在为您播放音乐");
                sendMediaCommand("play");
            }
            return;
        }

        if (command.contains("暂停") || command.contains("停止播放")) {
            speak("已暂停");
            sendMediaCommand("pause");
            return;
        }

        if (command.contains("下一首") || command.contains("下一个")) {
            speak("下一首");
            sendMediaCommand("next");
            return;
        }

        if (command.contains("上一首") || command.contains("上一个")) {
            speak("上一首");
            sendMediaCommand("previous");
            return;
        }

        // 4. 音量指令
        if (command.contains("音量")) {
            if (command.contains("增大") || command.contains("调高") || command.contains("大声")) {
                adjustVolume(1);
                speak("已增大音量");
            } else if (command.contains("减小") || command.contains("调低") || command.contains("小声")) {
                adjustVolume(-1);
                speak("已减小音量");
            }
            return;
        }

        // 5. 电话指令
        if (command.contains("打电话") || command.contains("拨打") || command.contains("呼叫")) {
            speak("正在拨打电话");
            // 实际项目中需要解析联系人并拨号
            return;
        }

        // 6. 天气查询
        if (command.contains("天气") || command.contains("温度")) {
            speak("正在查询天气信息");
            // 触发天气查询
            return;
        }

        // 7. 时间查询
        if (command.contains("几点") || command.contains("时间")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH点mm分", Locale.CHINA);
            String time = sdf.format(new java.util.Date());
            speak("现在是" + time);
            return;
        }

        // 8. 打开 AI 助手
        if (command.contains("助手") || command.contains("聊天") || command.contains("问问")) {
            speak("好的，打开AI助手");
            Intent intent = new Intent(this, AIAssistantActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        // 9. 返回桌面
        if (command.contains("返回") || command.contains("桌面") || command.contains("主页")) {
            speak("返回桌面");
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        // 10. 未知指令 -> 交给 AI 处理
        handleWithAI(command);
    }

    /**
     * 提取导航目的地
     */
    private String extractDestination(String command) {
        String[] prefixes = {"导航去", "导航到", "去", "怎么去"};
        for (String prefix : prefixes) {
            int idx = command.indexOf(prefix);
            if (idx >= 0) {
                return command.substring(idx + prefix.length()).trim();
            }
        }
        return null;
    }

    /**
     * 启动导航
     */
    private void launchNavigation(String destination) {
        try {
            // 使用 Intent 调用地图导航
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("androidamap://keywordNavi?keyword=" + destination));
            intent.setPackage("com.autonavi.minimap");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "启动导航失败", e);
            speak("抱歉，无法启动导航应用");
        }
    }

    /**
     * 发送媒体控制指令
     */
    private void sendMediaCommand(String command) {
        // 使用 AudioManager 的媒体按键模拟
        android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int keyCode;
        switch (command) {
            case "play":
            case "pause":
                keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;
            case "next":
                keyCode = android.view.KeyEvent.KEYCODE_MEDIA_NEXT;
                break;
            case "previous":
                keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                break;
            default:
                return;
        }
        // 通过 Runtime 模拟按键（需要 root 或系统权限）
        // 在实际车机中可能需要不同的实现
    }

    /**
     * 调节音量
     */
    private void adjustVolume(int direction) {
        android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (direction > 0) {
            am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.ADJUST_RAISE, 0);
        } else {
            am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.ADJUST_LOWER, 0);
        }
    }

    /**
     * 将未知指令交给 AI 处理
     */
    private void handleWithAI(String command) {
        aiHelper.chat(command, new AIHelper.AIResponseCallback() {
            @Override
            public void onSuccess(String response) {
                speak(response);
            }

            @Override
            public void onError(String error) {
                speak("抱歉，我暂时无法回答这个问题");
            }
        });
    }

    /**
     * TTS 语音播报
     */
    private void speak(String text) {
        if (textToSpeech != null && ttsReady) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_" + System.currentTimeMillis());
            }
        }
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "语音助手服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("氢桌面语音助手后台服务");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("氢助手")
                .setContentText("语音助手正在监听...")
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (aiHelper != null) {
            aiHelper.shutdown();
        }
    }
}
