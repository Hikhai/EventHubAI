package com.eventhub.service;

import com.eventhub.dao.DashboardDAO;
import com.eventhub.dao.RegistrationDAO;
import com.eventhub.dao.ReviewDAO;
import com.eventhub.dao.UserDAO;
import com.eventhub.dto.DashboardDTO;

import java.sql.SQLException;

/**
 * Service lấy dữ liệu cho Dashboard Admin.
 * Gọi nhiều DAO và đóng gói vào DashboardDTO.
 */
public class DashboardService {

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final RegistrationDAO registrationDAO = new RegistrationDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Lấy toàn bộ dữ liệu Dashboard trong 1 lần gọi.
     * DashboardServlet gọi method này rồi đẩy sang JSP.
     *
     * @return DashboardDTO chứa tất cả số liệu
     */
    public DashboardDTO getDashboardData() throws SQLException {
        DashboardDTO dto = new DashboardDTO();

        // --- KPI: Số liệu tổng quan ---
        int[] eventStats = dashboardDAO.getOverviewStats();
        // [0]=total, [1]=active, [2]=completed, [3]=cancelled
        dto.setTotalEvents(eventStats[0]);
        dto.setActiveEvents(eventStats[1]);
        dto.setCompletedEvents(eventStats[2]);
        dto.setCancelledEvents(eventStats[3]);

        // Tổng đăng ký và user
        dto.setTotalRegistrations(registrationDAO.countTotal());
        dto.setTotalUsers(userDAO.countActiveUsers());

        // Rating tổng hợp
        dto.setOverallAvgRating(reviewDAO.getOverallAvgRating());
        dto.setTotalReviews(reviewDAO.countAll());

        // --- Dữ liệu Chart ---
        dto.setRegistrationsByMonth(dashboardDAO.getRegistrationsByMonth());
        dto.setEventsByCategory(dashboardDAO.getEventsByCategory());

        // --- Dữ liệu Table ---
        dto.setTopEvents(dashboardDAO.getTopEvents(10));
        dto.setUpcomingEvents(dashboardDAO.getUpcomingEvents(7));
        dto.setAlmostFullEvents(dashboardDAO.getAlmostFullEvents(0.9)); // >= 90%
        dto.setTopRatedEvents(dashboardDAO.getTopRatedEvents(1, 5));    // >= 1 review
        dto.setRecentRegistrations(registrationDAO.findRecent(10));

        return dto;
    }
}