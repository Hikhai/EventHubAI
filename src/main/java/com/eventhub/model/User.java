package com.eventhub.model;

import java.time.LocalDateTime;

/**
 * Model đại diện cho một tài khoản người dùng.
 * Map với bảng "users" trong database.
 */
public class User {

    private int userId;
    private String fullName;
    private String email;
    private String password;   // Lưu BCrypt hash, không bao giờ plain text
    private String role;       // "ADMIN" hoặc "USER"
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor rỗng (cần cho DAO khi tạo object từ ResultSet)
    public User() {}

    // Constructor đầy đủ
    public User(int userId, String fullName, String email,
                String password, String role, boolean isActive,
                LocalDateTime createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    /** Kiểm tra user có phải Admin không */
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    // ===== GETTERS & SETTERS =====
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}