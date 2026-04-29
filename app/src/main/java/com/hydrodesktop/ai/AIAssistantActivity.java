package com.hydrodesktop.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hydrodesktop.R;
import com.hydrodesktop.util.AIHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 助手对话界面
 * 支持文字输入和语音输入，调用大语言模型API
 */
public class AIAssistantActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etInput;
    private ImageButton btnSend;
    private ImageButton btnVoiceInput;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private TextView tvTitle;
    // private ScrollView scrollView; // unused with RecyclerView

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private AIHelper aiHelper;
    private Handler mainHandler;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        mainHandler = new Handler(Looper.getMainLooper());
        aiHelper = new AIHelper(this);

        initViews();
        setupRecyclerView();
        addWelcomeMessage();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        btnVoiceInput = findViewById(R.id.btn_voice_input);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);
        tvTitle = findViewById(R.id.tv_title);
        // scrollView = findViewById(R.id.scroll_view);

        tvTitle.setText(R.string.ai_assistant_title);

        btnSend.setOnClickListener(v -> sendMessage());

        btnVoiceInput.setOnClickListener(v -> startVoiceInput());

        btnBack.setOnClickListener(v -> finish());

        // 软键盘发送
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);
    }

    private void addWelcomeMessage() {
        ChatMessage welcome = new ChatMessage(
                ChatMessage.TYPE_AI,
                getString(R.string.ai_welcome_message)
        );
        messages.add(welcome);
        chatAdapter.notifyItemInserted(messages.size() - 1);
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text) || isProcessing) return;

        // 添加用户消息
        ChatMessage userMsg = new ChatMessage(ChatMessage.TYPE_USER, text);
        messages.add(userMsg);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        etInput.setText("");

        scrollToBottom();
        showLoading(true);

        // 调用 AI API
        isProcessing = true;
        aiHelper.chat(text, new AIHelper.AIResponseCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    showLoading(false);
                    isProcessing = false;

                    ChatMessage aiMsg = new ChatMessage(ChatMessage.TYPE_AI, response);
                    messages.add(aiMsg);
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    showLoading(false);
                    isProcessing = false;

                    String errorMsg = getString(R.string.ai_error_prefix) + error;
                    ChatMessage err = new ChatMessage(ChatMessage.TYPE_ERROR, errorMsg);
                    messages.add(err);
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                });
            }
        });
    }

    /**
     * 语音输入
     */
    private void startVoiceInput() {
        Toast.makeText(this, R.string.voice_input_hint, Toast.LENGTH_SHORT).show();
        // 语音输入通过 VoiceRecognitionService 处理
        // 这里启动系统语音识别作为备选
        try {
            android.content.Intent intent = new android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_input_hint));
            startActivityForResult(intent, 100);
        } catch (Exception e) {
            Toast.makeText(this, R.string.voice_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(
                    android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                etInput.setText(results.get(0));
                sendMessage();
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!show);
    }

    private void scrollToBottom() {
        if (messages.size() > 0) {
            rvMessages.smoothScrollToPosition(messages.size() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiHelper != null) {
            aiHelper.shutdown();
        }
    }
}
