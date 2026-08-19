package com.eventhub.servlet.admin;

import com.eventhub.dto.DashboardDTO;
import com.eventhub.service.DashboardService;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Hiển thị Dashboard Admin.
 * GET /admin/dashboard
 */
@WebServlet("/admin/dashboard")
public class DashboardServlet extends HttpServlet {

    private final DashboardService dashboardService = new DashboardService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        DashboardDTO dashboard;

        try {
            dashboard = dashboardService.getDashboardData();
        } catch (Exception e) {
            // Log lỗi để debug
            System.err.println("[DashboardServlet] Lỗi lấy data: " + e.getMessage());
            e.printStackTrace();

            // Tạo DTO rỗng để tránh NPE trong JSP
            dashboard = new DashboardDTO();
            dashboard.setRegistrationsByMonth(new ArrayList<>());
            dashboard.setEventsByCategory(new ArrayList<>());
            dashboard.setTopEvents(new ArrayList<>());
            dashboard.setUpcomingEvents(new ArrayList<>());
            dashboard.setAlmostFullEvents(new ArrayList<>());
            dashboard.setTopRatedEvents(new ArrayList<>());
            dashboard.setRecentRegistrations(new ArrayList<>());

            req.setAttribute("errorMsg", "Không thể tải một số dữ liệu Dashboard.");
        }

        // Đảm bảo các list không null (nếu Service trả null)
        if (dashboard.getRegistrationsByMonth() == null) {
            dashboard.setRegistrationsByMonth(new ArrayList<>());
        }
        if (dashboard.getEventsByCategory() == null) {
            dashboard.setEventsByCategory(new ArrayList<>());
        }

        // Set attributes
        req.setAttribute("dashboard", dashboard);

        // Serialize chart data sang JSON (luôn có giá trị, ít nhất là "[]")
        req.setAttribute("regByMonthJson",
                gson.toJson(dashboard.getRegistrationsByMonth()));
        req.setAttribute("byCategoryJson",
                gson.toJson(dashboard.getEventsByCategory()));

        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp")
                .forward(req, resp);
    }
}