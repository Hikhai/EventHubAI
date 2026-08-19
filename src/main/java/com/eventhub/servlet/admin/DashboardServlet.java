package com.eventhub.servlet.admin;

import com.eventhub.dto.DashboardDTO;
import com.eventhub.service.DashboardService;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Hiển thị Dashboard Admin với thống kê và biểu đồ.
 * GET /admin/dashboard
 */
@WebServlet("/admin/dashboard")
public class DashboardServlet extends HttpServlet {

    private final DashboardService dashboardService = new DashboardService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            DashboardDTO dashboard = dashboardService.getDashboardData();

            // Set object cho JSP dùng JSTL
            req.setAttribute("dashboard", dashboard);

            // Serialize chart data sang JSON (Chart.js cần JavaScript array)
            // Dùng Gson để đảm bảo escape đúng
            req.setAttribute("regByMonthJson",
                    gson.toJson(dashboard.getRegistrationsByMonth()));
            req.setAttribute("byCategoryJson",
                    gson.toJson(dashboard.getEventsByCategory()));

            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            req.setAttribute("errorMsg",
                    "Lỗi tải dữ liệu Dashboard: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp")
                    .forward(req, resp);
        }
    }
}