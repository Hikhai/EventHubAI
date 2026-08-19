package com.eventhub.service;

import com.eventhub.config.DBConnection;
import com.eventhub.dao.EventDAO;
import com.eventhub.dao.RegistrationDAO;
import com.eventhub.dao.ReviewDAO;
import com.eventhub.exception.ReviewException;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.model.Review;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service xử lý đánh giá sự kiện.
 * Kiểm tra đủ 5 điều kiện trước khi cho phép đánh giá.
 */
public class ReviewService {

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final RegistrationDAO registrationDAO = new RegistrationDAO();
    private final EventDAO eventDAO = new EventDAO();

    // =====================================================
    // SUBMIT ĐÁNH GIÁ
    // =====================================================

    /**
     * Submit đánh giá sự kiện.
     *
     * 5 điều kiện bắt buộc:
     *   1. User đã đăng nhập (đã check ở Filter/Servlet)
     *   2. Sự kiện đã kết thúc (end_time < NOW)
     *   3. User có bản ghi registration với event này
     *   4. registration.status = REGISTERED (không phải CANCELLED)
     *   5. User chưa đánh giá event này
     *
     * @throws ReviewException nếu vi phạm điều kiện
     */
    public void submitReview(int userId, int eventId,
                             int rating, String comment)
            throws ReviewException, SQLException {

        // --- Validate input trước ---
        validateReviewInput(rating, comment);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // --- ĐIỀU KIỆN 2: Sự kiện đã kết thúc ---
            Event event = eventDAO.findById(eventId);
            if (event == null) {
                throw new ReviewException("Sự kiện không tồn tại.");
            }
            if (!event.isEnded()) {
                throw new ReviewException(
                        "Sự kiện chưa kết thúc. Bạn chỉ có thể đánh giá sau khi sự kiện kết thúc."
                );
            }

            // --- ĐIỀU KIỆN 3 & 4: Có đăng ký REGISTERED ---
            Registration registration =
                    registrationDAO.findByUserAndEvent(userId, eventId);

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

            // --- ĐIỀU KIỆN 5: Chưa đánh giá ---
            Review existingReview =
                    reviewDAO.findByUserAndEvent(userId, eventId);
            if (existingReview != null) {
                throw new ReviewException("Bạn đã đánh giá sự kiện này rồi.");
            }

            // --- Chuẩn hóa comment ---
            // Nếu comment rỗng sau trim → lưu NULL
            String cleanComment = (comment != null && !comment.trim().isEmpty())
                    ? comment.trim() : null;

            // --- Insert review ---
            reviewDAO.insert(userId, eventId, rating, cleanComment, conn);

            // --- Cập nhật avg_rating và total_reviews của event ---
            eventDAO.updateRating(eventId, conn);

            conn.commit();

        } catch (ReviewException e) {
            if (conn != null) conn.rollback();
            throw e;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Validate rating và comment.
     */
    private void validateReviewInput(int rating, String comment)
            throws ReviewException {

        // Rating phải từ 1-5
        if (rating < 1 || rating > 5) {
            throw new ReviewException("Đánh giá phải từ 1 đến 5 sao.");
        }

        // Comment: nếu có nội dung thì phải đủ độ dài
        if (comment != null && !comment.trim().isEmpty()) {
            if (comment.trim().length() < 10) {
                throw new ReviewException(
                        "Nhận xét phải có ít nhất 10 ký tự."
                );
            }
            if (comment.trim().length() > 500) {
                throw new ReviewException(
                        "Nhận xét không được vượt quá 500 ký tự."
                );
            }
        }
    }

    // =====================================================
    // LẤY DANH SÁCH
    // =====================================================

    /**
     * Lấy tất cả reviews của 1 sự kiện.
     */
    public List<Review> getReviewsByEvent(int eventId) throws SQLException {
        return reviewDAO.findAllByEvent(eventId);
    }

    /**
     * Lấy review của 1 user cho 1 sự kiện (để kiểm tra đã review chưa).
     */
    public Review getUserReview(int userId, int eventId) throws SQLException {
        return reviewDAO.findByUserAndEvent(userId, eventId);
    }
}