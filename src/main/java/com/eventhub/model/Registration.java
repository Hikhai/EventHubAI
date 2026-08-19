package com.eventhub.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model đại diện cho một lượt đăng ký sự kiện.
 * Map với bảng "registrations", JOIN thêm thông tin user + event.
 */
public class Registration {

    private int registrationId;
    private int userId;
    private int eventId;
    private String status;           // "REGISTERED" hoặc "CANCELLED"
    private LocalDateTime registeredAt;
    private LocalDateTime cancelledAt;

    // Trường JOIN từ bảng users
    private String userFullName;
    private String userEmail;

    // Trường JOIN từ bảng events
    private String eventTitle;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;
    private String eventLocation;
    private String eventStatus;
    private String eventImagePath;
    private double eventAvgRating;

    public Registration() {}

    /** Sự kiện đã kết thúc chưa (để hiển thị form đánh giá) */
    public boolean isEventEnded() {
        return eventEndTime != null && eventEndTime.isBefore(LocalDateTime.now());
    }

    /** Sự kiện chưa bắt đầu (để hiển thị nút hủy) */
    public boolean isEventUpcoming() {
        return eventStartTime != null && eventStartTime.isAfter(LocalDateTime.now());
    }

    public String getFormattedEventStartTime() {
        if (eventStartTime == null) return "";
        return eventStartTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    /**
     * Format thời gian đăng ký để hiển thị.
     */
    public String getFormattedRegisteredAt() {
        if (registeredAt == null) return "";
        return registeredAt.format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        );
    }

    // ===== GETTERS & SETTERS =====
    public int getRegistrationId() { return registrationId; }
    public void setRegistrationId(int registrationId) { this.registrationId = registrationId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public LocalDateTime getEventStartTime() { return eventStartTime; }
    public void setEventStartTime(LocalDateTime eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    public LocalDateTime getEventEndTime() { return eventEndTime; }
    public void setEventEndTime(LocalDateTime eventEndTime) { this.eventEndTime = eventEndTime; }

    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }

    public String getEventStatus() { return eventStatus; }
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; }

    public String getEventImagePath() { return eventImagePath; }
    public void setEventImagePath(String eventImagePath) { this.eventImagePath = eventImagePath; }

    public double getEventAvgRating() { return eventAvgRating; }
    public void setEventAvgRating(double eventAvgRating) { this.eventAvgRating = eventAvgRating; }
}