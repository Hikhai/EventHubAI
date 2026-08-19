package com.eventhub.servlet.user;

import com.eventhub.dao.CategoryDAO;
import com.eventhub.dto.EventFilterDTO;
import com.eventhub.dto.PaginationDTO;
import com.eventhub.model.Category;
import com.eventhub.model.Event;
import com.eventhub.model.User;
import com.eventhub.service.EventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

/**
 * Hiển thị danh sách sự kiện cho User/Guest.
 * GET /events
 * Params: keyword, categoryId, page
 */
@WebServlet("/events")
public class EventListServlet extends HttpServlet {

    private final EventService  eventService  = new EventService();
    private final CategoryDAO   categoryDAO   = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            // --- Parse filter params từ URL ---
            EventFilterDTO filter = new EventFilterDTO();

            String keyword = req.getParameter("keyword");
            if (keyword != null && !keyword.trim().isEmpty()) {
                filter.setKeyword(keyword.trim());
            }

            String categoryIdStr = req.getParameter("categoryId");
            if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
                try {
                    filter.setCategoryId(Integer.parseInt(categoryIdStr));
                } catch (NumberFormatException ignored) {}
            }

            String pageStr = req.getParameter("page");
            if (pageStr != null) {
                try {
                    filter.setPage(Integer.parseInt(pageStr));
                } catch (NumberFormatException ignored) {}
            }

            // --- Lấy dữ liệu ---
            List<Event> events         = eventService.getEventsForUser(filter);
            PaginationDTO pagination   = eventService.getPaginationForUser(filter);
            List<Category> categories  = categoryDAO.findAll();

            // --- Gợi ý sự kiện ---
            User user = (User) req.getSession().getAttribute("loggedInUser");
            Integer userId = (user != null) ? user.getUserId() : null;
            List<Event> recommendations = eventService.getRecommendations(userId);

            // --- Set attributes cho JSP ---
            req.setAttribute("events",          events);
            req.setAttribute("pagination",       pagination);
            req.setAttribute("categories",       categories);
            req.setAttribute("recommendations",  recommendations);
            req.setAttribute("filter",           filter);

            // Giữ giá trị filter để hiển thị lại trên form
            req.setAttribute("keyword",    filter.getKeyword());
            req.setAttribute("categoryId", filter.getCategoryId());

            req.getRequestDispatcher("/WEB-INF/views/user/event-list.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi tải danh sách sự kiện: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/");
        }
    }
}