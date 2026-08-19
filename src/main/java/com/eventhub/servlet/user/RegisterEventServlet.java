package com.eventhub.servlet.user;

import com.eventhub.exception.EventException;
import com.eventhub.exception.RegistrationException;
import com.eventhub.model.User;
import com.eventhub.service.RegistrationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Xử lý đăng ký tham gia sự kiện.
 * POST /user/register-event
 * Param: eventId
 */
@WebServlet("/user/register-event")
public class RegisterEventServlet extends HttpServlet {

    private final RegistrationService registrationService = new RegistrationService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = (User) req.getSession().getAttribute("loggedInUser");

        // Parse eventId
        String eventIdStr = req.getParameter("eventId");
        int eventId;
        try {
            eventId = Integer.parseInt(eventIdStr);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/events");
            return;
        }

        try {
            registrationService.registerEvent(user.getUserId(), eventId);

            req.getSession().setAttribute("successMsg", "Đăng ký tham gia thành công!");

        } catch (EventException | RegistrationException e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi hệ thống, vui lòng thử lại.");
        }

        // PRG Pattern: luôn redirect sau POST
        resp.sendRedirect(req.getContextPath()
                + "/events/detail?id=" + eventId);
    }
}