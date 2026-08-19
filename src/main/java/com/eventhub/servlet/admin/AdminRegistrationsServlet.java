package com.eventhub.servlet.admin;

import com.eventhub.exception.EventException;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.service.EventService;
import com.eventhub.service.RegistrationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

/**
 * Xem danh sách người đăng ký của 1 sự kiện.
 * GET /admin/events/registrations?eventId={id}
 */
@WebServlet("/admin/events/registrations")
public class AdminRegistrationsServlet extends HttpServlet {

    private final RegistrationService registrationService = new RegistrationService();
    private final EventService        eventService        = new EventService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idStr = req.getParameter("eventId");
        int eventId;
        try {
            eventId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/admin/events");
            return;
        }

        try {
            Event event = eventService.getEventById(eventId);
            List<Registration> registrations =
                    registrationService.getEventRegistrations(eventId);

            req.setAttribute("event",         event);
            req.setAttribute("registrations", registrations);

            req.getRequestDispatcher("/WEB-INF/views/admin/registrations.jsp")
                    .forward(req, resp);

        } catch (EventException e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/events");

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi tải danh sách đăng ký.");
            resp.sendRedirect(req.getContextPath() + "/admin/events");
        }
    }
}