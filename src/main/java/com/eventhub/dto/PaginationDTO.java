package com.eventhub.dto;

/**
 * DTO chứa thông tin phân trang để truyền sang JSP.
 */
public class PaginationDTO {

    private int currentPage;
    private int pageSize;
    private int totalItems;
    private int totalPages;

    public PaginationDTO(int currentPage, int pageSize, int totalItems) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        // Tính tổng số trang (làm tròn lên)
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }

    public boolean hasPrevious() { return currentPage > 1; }
    public boolean hasNext() { return currentPage < totalPages; }

    // ===== GETTERS =====
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }
    public int getTotalItems() { return totalItems; }
    public int getTotalPages() { return totalPages; }
}