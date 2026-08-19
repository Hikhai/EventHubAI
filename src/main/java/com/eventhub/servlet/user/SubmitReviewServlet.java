package com.eventhub.servlet.user;

import com.eventhub.exception.ReviewException;
import com.eventhub.model.User;
import com.eventhub.service.ReviewService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Xử lý submit đánh giá sự kiện.
 * POST /user/submit-review
 * Params: eventId, rating, comment
 */
@WebServlet("/user/submit-review")
public class SubmitReviewServlet extends HttpServlet {

    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        User user = (User) req.getSession().getAttribute("loggedInUser");

        // Parse params
        String eventIdStr = req.getParameter("eventId");
        String ratingStr  = req.getParameter("rating");
        String comment    = req.getParameter("comment");

        int eventId = 0;
        try {
            eventId = Integer.parseInt(eventIdStr);
            int rating = Integer.parseInt(ratingStr);

            reviewService.submitReview(user.getUserId(), eventId, rating, comment);

            req.getSession().setAttribute("successMsg",
                    "Cảm ơn bạn đã đánh giá sự kiện!");

        } catch (NumberFormatException e) {
            req.getSession().setAttribute("errorMsg",
                    "Dữ liệu đánh giá không hợp lệ.");

        } catch (ReviewException e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi hệ thống, vui lòng thử lại.");
        }

        // Redirect về trang chi tiết sự kiện
        if (eventId > 0) {
            resp.sendRedirect(req.getContextPath()
                    + "/events/detail?id=" + eventId);
        } else {
            resp.sendRedirect(req.getContextPath() + "/user/my-events");
        }
    }
}