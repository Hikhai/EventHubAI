package com.eventhub.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model đại diện cho một đánh giá sự kiện.
 * Map với bảng "reviews".
 */
public class Review {

    private int reviewId;
    private int userId;
    private int eventId;
    private int rating;         // 1-5 sao
    private String comment;
    private LocalDateTime createdAt;

    // Trường JOIN từ bảng users
    private String userFullName;

    public Review() {}

    /** Tạo chuỗi hiển thị sao: ví dụ rating=4 → "★★★★☆" */
    public String getStarsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= rating ? "★" : "☆");
        }
        return sb.toString();
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ===== GETTERS & SETTERS =====
    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }
}