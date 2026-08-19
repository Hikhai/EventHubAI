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
 * Xử lý hủy đăng ký sự kiện.
 * POST /user/cancel-event
 * Param: eventId
 */
@WebServlet("/user/cancel-event")
public class CancelEventServlet extends HttpServlet {

    private final RegistrationService registrationService = new RegistrationService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = (User) req.getSession().getAttribute("loggedInUser");

        String eventIdStr = req.getParameter("eventId");
        int eventId;
        try {
            eventId = Integer.parseInt(eventIdStr);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/user/my-events");
            return;
        }

        try {
            registrationService.cancelRegistration(user.getUserId(), eventId);

            req.getSession().setAttribute("successMsg",
                    "Đã hủy đăng ký thành công.");

        } catch (EventException | RegistrationException e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi hệ thống, vui lòng thử lại.");
        }

        resp.sendRedirect(req.getContextPath() + "/user/my-events");
    }
}