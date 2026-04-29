package com.hydrodesktop.ai;

/**
 * 聊天消息模型
 */
public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_ERROR = 2;

    private int type;
    private String content;
    private long timestamp;

    public ChatMessage(int type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public int getType() { return type; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }

    public boolean isUser() { return type == TYPE_USER; }
    public boolean isAI() { return type == TYPE_AI; }
    public boolean isError() { return type == TYPE_ERROR; }
}
