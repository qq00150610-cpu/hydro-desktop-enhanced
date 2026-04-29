package com.hydrodesktop.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI 大模型 API 调用工具
 * 支持 OpenAI / DeepSeek / 通义千问 / 本地 Ollama 等兼容接口
 */
public class AIHelper {

    private static final String PREF_NAME = "ai_settings";
    private static final String PREF_API_URL = "api_url";
    private static final String PREF_API_KEY = "api_key";
    private static final String PREF_MODEL = "model";
    private static final String PREF_SYSTEM_PROMPT = "system_prompt";

    private static final String DEFAULT_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个车载AI助手，名叫「氢助手」。你帮助驾驶员回答问题、导航建议、" +
                    "音乐推荐、天气查询等。回答要简洁、友好，适合驾驶场景。" +
                    "优先保证安全，如果驾驶员需要集中注意力驾驶，请提醒他们注意安全。";

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    // 对话历史
    private final List<JsonObject> conversationHistory;
    private static final int MAX_HISTORY = 20; // 最大历史消息数

    public interface AIResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public AIHelper(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.conversationHistory = new ArrayList<>();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发送消息到 AI API
     */
    public void chat(String userMessage, AIResponseCallback callback) {
        String apiUrl = getApiUrl();
        String apiKey = getApiKey();
        String model = getModel();

        if (TextUtils.isEmpty(apiUrl)) {
            callback.onError("请先在设置中配置 AI API 地址");
            return;
        }

        // 构建请求体
        JsonObject requestBody = buildRequestBody(userMessage, model);

        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(gson.toJson(requestBody), JSON));

        if (!TextUtils.isEmpty(apiKey)) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
        }

        requestBuilder.addHeader("Content-Type", "application/json");

        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        callback.onError("API 错误 (" + response.code() + "): " +
                                (response.body() != null ? response.body().string() : "未知错误"));
                        return;
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
                    String aiResponse = parseResponse(responseBody);

                    // 添加到对话历史
                    addToHistory("user", userMessage);
                    addToHistory("assistant", aiResponse);

                    callback.onSuccess(aiResponse);
                } catch (Exception e) {
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 构建 API 请求体
     */
    private JsonObject buildRequestBody(String userMessage, String model) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 2048);

        JsonArray messages = new JsonArray();

        // 系统提示词
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", getSystemPrompt());
        messages.add(systemMsg);

        // 对话历史
        for (JsonObject historyMsg : conversationHistory) {
            messages.add(historyMsg);
        }

        // 当前用户消息
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        body.add("messages", messages);
        return body;
    }

    /**
     * 解析 API 响应
     */
    private String parseResponse(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                if (message != null) {
                    return message.get("content").getAsString().trim();
                }
            }
            return "未收到有效回复";
        } catch (Exception e) {
            return "解析回复失败: " + e.getMessage();
        }
    }

    /**
     * 添加到对话历史
     */
    private void addToHistory(String role, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", content);
        conversationHistory.add(msg);

        // 限制历史长度
        while (conversationHistory.size() > MAX_HISTORY) {
            conversationHistory.remove(0);
        }
    }

    /**
     * 清除对话历史
     */
    public void clearHistory() {
        conversationHistory.clear();
    }

    // ===== 设置读取 =====

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getApiUrl() {
        return getPrefs().getString(PREF_API_URL, DEFAULT_API_URL);
    }

    public String getApiKey() {
        return getPrefs().getString(PREF_API_KEY, "");
    }

    public String getModel() {
        return getPrefs().getString(PREF_MODEL, DEFAULT_MODEL);
    }

    public String getSystemPrompt() {
        return getPrefs().getString(PREF_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT);
    }

    public static void saveSettings(Context context, String apiUrl, String apiKey,
                                     String model, String systemPrompt) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_API_URL, apiUrl);
        editor.putString(PREF_API_KEY, apiKey);
        editor.putString(PREF_MODEL, model);
        editor.putString(PREF_SYSTEM_PROMPT, systemPrompt);
        editor.apply();
    }

    public void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
