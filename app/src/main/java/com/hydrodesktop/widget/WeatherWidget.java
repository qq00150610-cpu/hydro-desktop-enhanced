package com.hydrodesktop.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 天气小部件
 * 通过公共天气 API 获取天气信息
 */
public class WeatherWidget {

    private static final String WEATHER_API = "https://wttr.in/?format=j1&lang=zh";

    public interface WeatherCallback {
        void onWeatherLoaded(String weatherText);
        void onError(String error);
    }

    /**
     * 刷新天气信息并更新 TextView
     */
    public static void refreshWeather(Context context, TextView tvWeather) {
        fetchWeather(new WeatherCallback() {
            @Override
            public void onWeatherLoaded(String weatherText) {
                new Handler(Looper.getMainLooper()).post(() -> tvWeather.setText(weatherText));
            }

            @Override
            public void onError(String error) {
                new Handler(Looper.getMainLooper()).post(() ->
                        tvWeather.setText("天气获取失败"));
            }
        });
    }

    /**
     * 获取天气信息
     */
    public static void fetchWeather(WeatherCallback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(WEATHER_API)
                .addHeader("Accept-Language", "zh-CN")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        callback.onError("HTTP " + response.code());
                        return;
                    }

                    String body = response.body().string();
                    String weatherText = parseWeather(body);
                    callback.onWeatherLoaded(weatherText);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * 解析天气 JSON 数据
     */
    private static String parseWeather(String json) {
        try {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json, JsonObject.class);

            JsonObject current = root.getAsJsonArray("current_condition").get(0).getAsJsonObject();

            String temp = current.get("temp_C").getAsString();
            String feelsLike = current.get("FeelsLikeC").getAsString();
            String humidity = current.get("humidity").getAsString();
            String windSpeed = current.get("windspeedKmph").getAsString();

            // 天气描述
            String weatherDesc = "";
            if (current.has("lang_zh")) {
                weatherDesc = current.getAsJsonArray("lang_zh").get(0).getAsJsonObject()
                        .get("value").getAsString();
            } else if (current.has("weatherDesc")) {
                weatherDesc = current.getAsJsonArray("weatherDesc").get(0).getAsJsonObject()
                        .get("value").getAsString();
            }

            return String.format("%s %s°C (体感%s°C)\n湿度%s%% 风速%skm/h",
                    weatherDesc, temp, feelsLike, humidity, windSpeed);

        } catch (Exception e) {
            return "天气数据解析失败";
        }
    }
}
