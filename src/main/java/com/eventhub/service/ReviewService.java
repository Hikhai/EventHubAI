package com.eventhub.service;

import com.eventhub.config.DBConnection;
import com.eventhub.dao.EventDAO;
import com.eventhub.dao.RegistrationDAO;
import com.eventhub.dao.ReviewDAO;
import com.eventhub.exception.ReviewException;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.model.Review;

import java.sql.SQLException;
import java.util.List;

/**
 * Service xử lý đánh giá sự kiện.
 */
public class ReviewService {

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final RegistrationDAO registrationDAO = new RegistrationDAO();
    private final EventDAO eventDAO = new EventDAO();

    public void submitReview(int userId, int eventId, int rating, String comment)
            throws ReviewException, SQLException {

        validateReviewInput(rating, comment);

        try {
            DBConnection.inTransaction(conn -> {
                Event event = eventDAO.findById(eventId, conn);
                if (event == null) {
                    throw new ReviewException("Sự kiện không tồn tại.");
                }
                if (event.isCancelled()) {
                    throw new ReviewException("Sự kiện đã bị hủy, không thể đánh giá.");
                }
                if (!event.isEnded()) {
                    throw new ReviewException(
                            "Sự kiện chưa kết thúc. Bạn chỉ có thể đánh giá sau khi sự kiện kết thúc."
                    );
                }

                Registration registration =
                        registrationDAO.findByUserAndEvent(userId, eventId, conn);

                if (registration == null) {
                    throw new ReviewException(
                            "Bạn chưa đăng ký sự kiện này, không thể đánh giá."
                    );
                }
                if ("CANCELLED".equals(registration.getStatus())) {
                    throw new ReviewException(
                            "Bạn đã hủy đăng ký sự kiện này, không thể đánh giá."
                    );
                }

                Review existingReview = reviewDAO.findByUserAndEvent(userId, eventId, conn);
                if (existingReview != null) {
                    throw new ReviewException("Bạn đã đánh giá sự kiện này rồi.");
                }

                String cleanComment = (comment != null && !comment.trim().isEmpty())
                        ? comment.trim() : null;

                reviewDAO.insert(userId, eventId, rating, cleanComment, conn);
                eventDAO.updateRating(eventId, conn);
            });
        } catch (ReviewException | SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private void validateReviewInput(int rating, String comment) throws ReviewException {
        if (rating < 1 || rating > 5) {
            throw new ReviewException("Đánh giá phải từ 1 đến 5 sao.");
        }

        if (comment != null && !comment.trim().isEmpty()) {
            int len = comment.trim().length();
            if (len < 10) {
                throw new ReviewException("Nhận xét phải có ít nhất 10 ký tự.");
            }
            if (len > 500) {
                throw new ReviewException("Nhận xét không được vượt quá 500 ký tự.");
            }
        }
    }

    public List<Review> getReviewsByEvent(int eventId) throws SQLException {
        return reviewDAO.findAllByEvent(eventId);
    }

    public Review getUserReview(int userId, int eventId) throws SQLException {
        return reviewDAO.findByUserAndEvent(userId, eventId);
    }
}
