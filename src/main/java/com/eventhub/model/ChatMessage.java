package com.eventhub.model;

import java.time.LocalDateTime;

/**
 * Một tin nhắn trong lịch sử trò chuyện.
 *
 * role chỉ nhận "user" hoặc "assistant" ở tầng lưu trữ/UI. Khi gửi
 * sang Gemini, role assistant sẽ được đổi thành model.
 */
public class ChatMessage {

    private long logId;
    private int userId;
    private String sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public ChatMessage() {
    }

    public ChatMessage(long logId, int userId, String sessionId,
                       String role, String content, LocalDateTime createdAt) {
        this.logId = logId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
