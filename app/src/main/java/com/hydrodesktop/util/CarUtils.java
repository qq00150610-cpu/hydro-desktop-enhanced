package com.hydrodesktop.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.provider.Settings;

/**
 * 车机系统工具类
 */
public class CarUtils {

    private static final String PREF_NAME = "car_settings";

    /**
     * 获取屏幕亮度
     */
    public static int getScreenBrightness(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return 128;
        }
    }

    /**
     * 设置屏幕亮度
     */
    public static void setScreenBrightness(Context context, int brightness) {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, brightness);
    }

    /**
     * 获取系统音量
     */
    public static int getVolume(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * 设置系统音量
     */
    public static void setVolume(Context context, int volume) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    /**
     * 获取最大音量
     */
    public static int getMaxVolume(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * 保存字符串设置
     */
    public static void saveSetting(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    /**
     * 读取字符串设置
     */
    public static String getSetting(Context context, String key, String defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    /**
     * 保存布尔设置
     */
    public static void saveBoolSetting(Context context, String key, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }

    /**
     * 读取布尔设置
     */
    public static boolean getBoolSetting(Context context, String key, boolean defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defaultValue);
    }
}
