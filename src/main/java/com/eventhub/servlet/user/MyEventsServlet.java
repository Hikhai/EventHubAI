package com.eventhub.servlet.user;

import com.eventhub.model.Registration;
import com.eventhub.model.User;
import com.eventhub.service.RegistrationService;
import com.eventhub.service.ReviewService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị sự kiện của User (3 tab).
 * GET /user/my-events
 */
@WebServlet("/user/my-events")
public class MyEventsServlet extends HttpServlet {

    private final RegistrationService registrationService = new RegistrationService();
    private final ReviewService       reviewService       = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = (User) req.getSession().getAttribute("loggedInUser");

        try {
            // Lấy tất cả đăng ký của user
            List<Registration> allRegs =
                    registrationService.getUserRegistrations(user.getUserId());

            // Phân loại vào 3 tab
            List<Registration> upcoming   = new ArrayList<>(); // Sắp diễn ra
            List<Registration> attended   = new ArrayList<>(); // Đã tham gia
            List<Registration> cancelled  = new ArrayList<>(); // Đã hủy

            for (Registration reg : allRegs) {
                if ("CANCELLED".equals(reg.getStatus())) {
                    cancelled.add(reg);
                } else if ("REGISTERED".equals(reg.getStatus())) {
                    if (reg.isEventEnded()) {
                        attended.add(reg);   // Đã kết thúc
                    } else {
                        upcoming.add(reg);   // Chưa bắt đầu hoặc đang diễn ra
                    }
                }
            }

            // Với tab "Đã tham gia", lấy thêm review (nếu có) cho từng event
            // Lưu vào Map: eventId → Review
            java.util.Map<Integer, com.eventhub.model.Review> reviewMap =
                    new java.util.HashMap<>();

            for (Registration reg : attended) {
                com.eventhub.model.Review review = reviewService
                        .getUserReview(user.getUserId(), reg.getEventId());
                if (review != null) {
                    reviewMap.put(reg.getEventId(), review);
                }
            }

            req.setAttribute("upcoming",   upcoming);
            req.setAttribute("attended",   attended);
            req.setAttribute("cancelled",  cancelled);
            req.setAttribute("reviewMap",  reviewMap);

            req.getRequestDispatcher("/WEB-INF/views/user/my-events.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi tải danh sách sự kiện của bạn.");
            resp.sendRedirect(req.getContextPath() + "/events");
        }
    }
}