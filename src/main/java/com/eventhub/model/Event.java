package com.eventhub.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model đại diện cho một sự kiện.
 * Map với bảng "events", có thêm các trường JOIN từ bảng khác.
 */
public class Event {

    // ===== TRƯỜNG TỪ BẢNG EVENTS =====
    private int eventId;
    private String title;
    private String description;
    private String summaryAi;          // Tóm tắt do Gemini tạo ra
    private String imagePath;          // Tên file ảnh (không phải full path)
    private String imageSource;        // "UPLOADED", "AI_GENERATED", "DEFAULT"
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registrationDeadline;
    private int maxParticipants;
    private int currentRegistered;
    private double avgRating;
    private int totalReviews;
    private String status;             // "DRAFT","PUBLISHED","CANCELLED","COMPLETED"
    private int categoryId;
    private int createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== TRƯỜNG JOIN TỪ BẢNG KHÁC (không có trong DB) =====
    private String categoryName;       // Lấy từ bảng categories
    private String createdByName;      // Tên admin tạo (từ bảng users)

    public Event() {}

    // ===== CÁC METHOD TÍNH TOÁN (không lưu DB) =====

    /** Số chỗ còn trống */
    public int getAvailableSlots() {
        return maxParticipants - currentRegistered;
    }

    /** Sự kiện đã đầy chưa */
    public boolean isFull() {
        return currentRegistered >= maxParticipants;
    }

    /** Còn trong thời hạn đăng ký không (published + chưa hết deadline) */
    public boolean isRegistrationOpen() {
        return "PUBLISHED".equals(status)
                && LocalDateTime.now().isBefore(registrationDeadline);
    }

    /** Sự kiện chưa bắt đầu */
    public boolean isUpcoming() {
        return startTime != null && startTime.isAfter(LocalDateTime.now());
    }

    /** Sự kiện đã kết thúc */
    public boolean isEnded() {
        return endTime != null && endTime.isBefore(LocalDateTime.now());
    }

    /** Tỷ lệ phần trăm chỗ đã đăng ký (dùng cho progress bar) */
    public double getFillRatePercent() {
        if (maxParticipants == 0) return 0;
        return (currentRegistered * 100.0) / maxParticipants;
    }

    /**
     * Đường dẫn ảnh để hiển thị trong JSP.
     * Trả về ảnh default nếu không có ảnh cụ thể.
     */
    /**
     * Đường dẫn ảnh để hiển thị trong JSP (xử lý đúng thư mục events vs defaults)
     */
    public String getDisplayImagePath() {
        if (imagePath != null && !imagePath.isEmpty()) {
            // Nếu là ảnh mặc định -> Trỏ vào thư mục defaults
            if ("DEFAULT".equals(imageSource) || imagePath.startsWith("default_")) {
                return "/eventhub/uploads/defaults/" + imagePath;
            }
            // Nếu là ảnh upload hoặc AI -> Trỏ vào thư mục events (có cache-busting)
            String cacheKey = updatedAt != null
                    ? "?v=" + updatedAt.toEpochSecond(java.time.ZoneOffset.UTC)
                    : "";
            return "/eventhub/uploads/events/" + imagePath + cacheKey;
        }
        return "/eventhub/uploads/defaults/default_other.jpg";
    }

    /** Format thời gian đẹp hơn để hiển thị (dd/MM/yyyy HH:mm) */
    public String getFormattedStartTime() {
        if (startTime == null) return "";
        return startTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getFormattedEndTime() {
        if (endTime == null) return "";
        return endTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getFormattedDeadline() {
        if (registrationDeadline == null) return "";
        return registrationDeadline.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Format cho HTML input datetime-local (yyyy-MM-ddTHH:mm)
     */
    public String getStartTimeInput() {
        if (startTime == null) return "";
        return startTime.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        );
    }

    public String getEndTimeInput() {
        if (endTime == null) return "";
        return endTime.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        );
    }

    public String getRegistrationDeadlineInput() {
        if (registrationDeadline == null) return "";
        return registrationDeadline.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        );
    }
    // ===== GETTERS & SETTERS =====
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSummaryAi() { return summaryAi; }
    public void setSummaryAi(String summaryAi) { this.summaryAi = summaryAi; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getImageSource() { return imageSource; }
    public void setImageSource(String imageSource) { this.imageSource = imageSource; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public int getCurrentRegistered() { return currentRegistered; }
    public void setCurrentRegistered(int currentRegistered) {
        this.currentRegistered = currentRegistered;
    }

    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
}