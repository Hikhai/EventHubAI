package com.eventhub.servlet.admin;

import com.eventhub.dao.CategoryDAO;
import com.eventhub.dto.EventFilterDTO;
import com.eventhub.dto.PaginationDTO;
import com.eventhub.model.Category;
import com.eventhub.model.Event;
import com.eventhub.service.EventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

/**
 * Danh sách sự kiện cho Admin (tất cả trạng thái).
 * GET /admin/events
 */
@WebServlet("/admin/events")
public class AdminEventListServlet extends HttpServlet {

    private final EventService  eventService  = new EventService();
    private final CategoryDAO   categoryDAO   = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            // Parse filter
            EventFilterDTO filter = new EventFilterDTO();
            filter.setPageSize(10); // Admin xem 10 per page

            String keyword = req.getParameter("keyword");
            if (keyword != null && !keyword.trim().isEmpty()) {
                filter.setKeyword(keyword.trim());
            }

            String catIdStr = req.getParameter("categoryId");
            if (catIdStr != null && !catIdStr.isEmpty()) {
                try { filter.setCategoryId(Integer.parseInt(catIdStr)); }
                catch (NumberFormatException ignored) {}
            }

            String status = req.getParameter("status");
            if (status != null && !status.isEmpty()) {
                filter.setStatus(status);
            }

            String pageStr = req.getParameter("page");
            if (pageStr != null) {
                try { filter.setPage(Integer.parseInt(pageStr)); }
                catch (NumberFormatException ignored) {}
            }

            List<Event>    events     = eventService.getEventsForAdmin(filter);
            PaginationDTO  pagination = eventService.getPaginationForAdmin(filter);
            List<Category> categories = categoryDAO.findAll();

            req.setAttribute("events",     events);
            req.setAttribute("pagination", pagination);
            req.setAttribute("categories", categories);
            req.setAttribute("filter",     filter);

            req.getRequestDispatcher("/WEB-INF/views/admin/event-list.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", "Lỗi tải danh sách sự kiện.");
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }
}