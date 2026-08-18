package com.eventhub.dto;

/**
 * DTO chứa các tiêu chí lọc/tìm kiếm sự kiện.
 * Servlet tạo object này từ request params rồi truyền vào DAO.
 */
public class EventFilterDTO {

    private String keyword;      // Từ khóa tìm kiếm (null = không tìm kiếm)
    private Integer categoryId;  // Lọc theo danh mục (null = tất cả)
    private String status;       // Lọc theo trạng thái (null = tất cả) - Admin dùng
    private int page;            // Trang hiện tại (bắt đầu từ 1)
    private int pageSize;        // Số item mỗi trang

    public EventFilterDTO() {
        this.page = 1;
        this.pageSize = 9;  // Mặc định 9 sự kiện mỗi trang
    }

    /** Tính offset cho SQL LIMIT clause */
    public int getOffset() {
        return (page - 1) * pageSize;
    }

    // ===== GETTERS & SETTERS =====
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}