package com.hydrodesktop.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.hydrodesktop.launcher.MainActivity;
import com.hydrodesktop.voice.VoiceRecognitionService;

/**
 * 开机自启广播接收器
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // 启动桌面
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);

            // 检查是否需要自动启动语音服务
            SharedPreferences prefs = context.getSharedPreferences("car_settings", Context.MODE_PRIVATE);
            if (prefs.getBoolean("auto_start", true) && prefs.getBoolean("voice_wake", true)) {
                Intent serviceIntent = new Intent(context, VoiceRecognitionService.class);
                context.startForegroundService(serviceIntent);
            }
        }
    }
}
