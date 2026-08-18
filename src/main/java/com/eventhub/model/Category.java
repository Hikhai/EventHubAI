package com.eventhub.model;

import java.time.LocalDateTime;

/**
 * Model đại diện cho danh mục sự kiện.
 * Map với bảng "categories".
 */
public class Category {

    private int categoryId;
    private String categoryName;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Category() {}

    // ===== GETTERS & SETTERS =====
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}