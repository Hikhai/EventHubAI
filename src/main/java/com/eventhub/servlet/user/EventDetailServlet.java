package com.eventhub.servlet.user;

import com.eventhub.config.PaymentConfig;
import com.eventhub.exception.EventException;
import com.eventhub.model.Event;
import com.eventhub.model.Payment;
import com.eventhub.model.Registration;
import com.eventhub.model.Review;
import com.eventhub.model.User;
import com.eventhub.service.EventService;
import com.eventhub.service.PaymentService;
import com.eventhub.service.RegistrationService;
import com.eventhub.service.ReviewService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

/**
 * Hiển thị chi tiết 1 sự kiện.
 * GET /events/detail?id={eventId}
 */
@WebServlet("/events/detail")
public class EventDetailServlet extends HttpServlet {

    private final EventService        eventService        = new EventService();
    private final RegistrationService registrationService = new RegistrationService();
    private final ReviewService       reviewService       = new ReviewService();
    private final PaymentService      paymentService      = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // --- Parse eventId ---
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/events");
            return;
        }

        int eventId;
        try {
            eventId = Integer.parseInt(idStr.trim());
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/events");
            return;
        }

        try {
            // --- Lấy thông tin sự kiện ---
            Event event = eventService.getEventById(eventId);

            // User thường chỉ xem PUBLISHED
            User user = (User) req.getSession().getAttribute("loggedInUser");
            boolean isAdmin = (user != null && user.isAdmin());

            if (!isAdmin
                    && !"PUBLISHED".equals(event.getStatus())
                    && !"COMPLETED".equals(event.getStatus())) {
                resp.sendRedirect(req.getContextPath() + "/events");
                return;
            }

            // --- Trạng thái đăng ký của user hiện tại ---
            Registration userRegistration = null;
            Review userReview = null;
            Payment pendingPayment = null;

            if (user != null && !user.isAdmin()) {
                userRegistration = registrationService
                        .getUserRegistration(user.getUserId(), eventId);
                if (userRegistration != null
                        && "PENDING_PAYMENT".equals(userRegistration.getStatus())) {
                    pendingPayment = paymentService.getPendingPayment(user.getUserId(), eventId);
                    if (pendingPayment != null && "MOCK".equals(pendingPayment.getProvider())
                            && (pendingPayment.getCheckoutUrl() == null
                            || pendingPayment.getCheckoutUrl().isBlank())) {
                        pendingPayment.setCheckoutUrl(req.getContextPath()
                                + "/user/payments/mock?code=" + pendingPayment.getPaymentCode());
                    }
                }

                // Lấy review nếu event đã kết thúc
                if (event.isEnded()) {
                    userReview = reviewService
                            .getUserReview(user.getUserId(), eventId);
                }
            }

            // --- Danh sách reviews ---
            List<Review> reviews = reviewService.getReviewsByEvent(eventId);

            // --- Sự kiện tương tự (gợi ý) ---
            List<Event> similarEvents = eventService
                    .getSimilarEvents(event.getCategoryId(), eventId);

            // --- Set attributes ---
            req.setAttribute("event",            event);
            req.setAttribute("userRegistration", userRegistration);
            req.setAttribute("pendingPayment",    pendingPayment);
            req.setAttribute("paymentProviderLabel", PaymentConfig.getProviderLabel());
            req.setAttribute("paymentHoldMinutes", PaymentConfig.getHoldMinutes());
            req.setAttribute("userReview",        userReview);
            req.setAttribute("reviews",           reviews);
            req.setAttribute("similarEvents",     similarEvents);

            req.getRequestDispatcher("/WEB-INF/views/user/event-detail.jsp")
                    .forward(req, resp);

        } catch (EventException e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/events");

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi tải thông tin sự kiện.");
            resp.sendRedirect(req.getContextPath() + "/events");
        }
    }
}