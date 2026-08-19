package com.eventhub.servlet.admin;

import com.eventhub.service.EventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Xóa hoặc hủy sự kiện.
 * POST /admin/events/delete
 * Param: eventId
 */
@WebServlet("/admin/events/delete")
public class AdminEventDeleteServlet extends HttpServlet {

    private final EventService eventService = new EventService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String eventIdStr = req.getParameter("eventId");
        int eventId;
        try {
            eventId = Integer.parseInt(eventIdStr);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/admin/events");
            return;
        }

        try {
            // deleteEvent trả về "DELETED" hoặc "CANCELLED"
            String result = eventService.deleteEvent(eventId);

            if ("DELETED".equals(result)) {
                req.getSession().setAttribute("successMsg",
                        "Đã xóa sự kiện thành công.");
            } else {
                req.getSession().setAttribute("successMsg",
                        "Sự kiện đã được hủy (còn người đăng ký). " +
                                "Tất cả đăng ký liên quan cũng bị hủy.");
            }

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/admin/events");
    }
}