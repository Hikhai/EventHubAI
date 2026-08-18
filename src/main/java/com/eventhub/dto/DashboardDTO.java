package com.eventhub.dto;

import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import java.util.List;

/**
 * DTO chứa toàn bộ dữ liệu cho trang Dashboard của Admin.
 * DashboardService đóng gói tất cả vào đây, Servlet truyền sang JSP.
 */
public class DashboardDTO {

    // ===== SỐ TỔNG QUAN (KPI Cards) =====
    private int totalEvents;
    private int activeEvents;       // Đang mở đăng ký
    private int completedEvents;
    private int cancelledEvents;
    private int totalRegistrations;
    private int totalUsers;
    private double overallAvgRating;
    private int totalReviews;

    // ===== DỮ LIỆU BIỂU ĐỒ =====
    private List<MonthStat> registrationsByMonth;   // Line chart
    private List<CategoryStat> eventsByCategory;    // Doughnut chart

    // ===== DỮ LIỆU BẢNG =====
    private List<Event> topEvents;              // Top 10 sự kiện nhiều đăng ký
    private List<Event> upcomingEvents;         // Sự kiện trong 7 ngày tới
    private List<Event> almostFullEvents;       // Sự kiện sắp hết chỗ
    private List<Event> topRatedEvents;         // Sự kiện được đánh giá cao
    private List<Registration> recentRegistrations;  // 10 đăng ký gần đây

    // ===== INNER CLASS: Dữ liệu 1 tháng cho line chart =====
    public static class MonthStat {
        private String month;   // Format: "01/2025"
        private int count;

        public MonthStat(String month, int count) {
            this.month = month;
            this.count = count;
        }

        public String getMonth() { return month; }
        public int getCount() { return count; }
    }

    // ===== INNER CLASS: Dữ liệu theo danh mục cho doughnut chart =====
    public static class CategoryStat {
        private String categoryName;
        private int totalEvents;
        private int totalRegistered;

        public CategoryStat(String categoryName, int totalEvents, int totalRegistered) {
            this.categoryName = categoryName;
            this.totalEvents = totalEvents;
            this.totalRegistered = totalRegistered;
        }

        public String getCategoryName() { return categoryName; }
        public int getTotalEvents() { return totalEvents; }
        public int getTotalRegistered() { return totalRegistered; }
    }

    // ===== GETTERS & SETTERS =====
    public int getTotalEvents() { return totalEvents; }
    public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }

    public int getActiveEvents() { return activeEvents; }
    public void setActiveEvents(int activeEvents) { this.activeEvents = activeEvents; }

    public int getCompletedEvents() { return completedEvents; }
    public void setCompletedEvents(int completedEvents) { this.completedEvents = completedEvents; }

    public int getCancelledEvents() { return cancelledEvents; }
    public void setCancelledEvents(int cancelledEvents) { this.cancelledEvents = cancelledEvents; }

    public int getTotalRegistrations() { return totalRegistrations; }
    public void setTotalRegistrations(int totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public double getOverallAvgRating() { return overallAvgRating; }
    public void setOverallAvgRating(double overallAvgRating) {
        this.overallAvgRating = overallAvgRating;
    }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public List<MonthStat> getRegistrationsByMonth() { return registrationsByMonth; }
    public void setRegistrationsByMonth(List<MonthStat> registrationsByMonth) {
        this.registrationsByMonth = registrationsByMonth;
    }

    public List<CategoryStat> getEventsByCategory() { return eventsByCategory; }
    public void setEventsByCategory(List<CategoryStat> eventsByCategory) {
        this.eventsByCategory = eventsByCategory;
    }

    public List<Event> getTopEvents() { return topEvents; }
    public void setTopEvents(List<Event> topEvents) { this.topEvents = topEvents; }

    public List<Event> getUpcomingEvents() { return upcomingEvents; }
    public void setUpcomingEvents(List<Event> upcomingEvents) {
        this.upcomingEvents = upcomingEvents;
    }

    public List<Event> getAlmostFullEvents() { return almostFullEvents; }
    public void setAlmostFullEvents(List<Event> almostFullEvents) {
        this.almostFullEvents = almostFullEvents;
    }

    public List<Event> getTopRatedEvents() { return topRatedEvents; }
    public void setTopRatedEvents(List<Event> topRatedEvents) {
        this.topRatedEvents = topRatedEvents;
    }

    public List<Registration> getRecentRegistrations() { return recentRegistrations; }
    public void setRecentRegistrations(List<Registration> recentRegistrations) {
        this.recentRegistrations = recentRegistrations;
    }
}