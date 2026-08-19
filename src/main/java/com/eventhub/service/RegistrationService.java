package com.eventhub.service;

import com.eventhub.config.DBConnection;
import com.eventhub.dao.EventDAO;
import com.eventhub.dao.RegistrationDAO;
import com.eventhub.exception.EventException;
import com.eventhub.exception.RegistrationException;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý đăng ký và hủy đăng ký sự kiện.
 *
 * QUAN TRỌNG: Dùng transaction + SELECT FOR UPDATE để tránh race condition
 * (2 user cùng đăng ký 1 chỗ cuối cùng còn lại).
 */
public class RegistrationService {

    private final RegistrationDAO registrationDAO = new RegistrationDAO();
    private final EventDAO eventDAO = new EventDAO();

    // =====================================================
    // ĐĂNG KÝ THAM GIA SỰ KIỆN
    // =====================================================

    /**
     * Đăng ký User tham gia Event.
     * Kiểm tra 5 điều kiện theo thứ tự, dùng transaction để đảm bảo nhất quán.
     *
     * @throws EventException        nếu sự kiện không hợp lệ
     * @throws RegistrationException nếu vi phạm rule đăng ký
     */
    public void registerEvent(int userId, int eventId)
            throws EventException, RegistrationException, SQLException {

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);  // Bắt đầu transaction

            // --- ĐIỀU KIỆN 1+2+3+4: Kiểm tra event (dùng FOR UPDATE để lock row) ---
            Event event = eventDAO.findByIdForUpdate(eventId, conn);

            if (event == null) {
                throw new EventException("Sự kiện không tồn tại.");
            }
            if (!"PUBLISHED".equals(event.getStatus())) {
                throw new EventException(
                        "Sự kiện này hiện không nhận đăng ký."
                );
            }
            if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
                throw new RegistrationException(
                        "Đã hết hạn đăng ký sự kiện này."
                );
            }
            if (event.isFull()) {
                throw new RegistrationException(
                        "Sự kiện đã đủ số lượng người tham gia."
                );
            }

            // --- ĐIỀU KIỆN 5: Kiểm tra user đã đăng ký chưa ---
            // (query này cũng trong cùng transaction)
            Registration existing = registrationDAO.findByUserAndEvent(userId, eventId);

            if (existing != null && "REGISTERED".equals(existing.getStatus())) {
                throw new RegistrationException("Bạn đã đăng ký sự kiện này rồi.");
            }

            // --- THỰC HIỆN ĐĂNG KÝ ---
            if (existing != null && "CANCELLED".equals(existing.getStatus())) {
                // Đã hủy trước đó → UPDATE thay vì INSERT (giữ nguyên PK)
                registrationDAO.reactivate(userId, eventId, conn);
            } else {
                // Lần đầu đăng ký → INSERT mới
                registrationDAO.insert(userId, eventId, conn);
            }

            // --- CẬP NHẬT SỐ NGƯỜI ĐĂNG KÝ (+1) ---
            eventDAO.incrementRegistered(eventId, conn);

            conn.commit();  // Tất cả OK → commit

        } catch (EventException | RegistrationException e) {
            // Business exception → rollback và re-throw
            if (conn != null) conn.rollback();
            throw e;

        } catch (SQLException e) {
            // DB exception → rollback và re-throw
            if (conn != null) conn.rollback();
            throw e;

        } finally {
            // Luôn restore autoCommit và đóng connection
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    // =====================================================
    // HỦY ĐĂNG KÝ
    // =====================================================

    /**
     * Hủy đăng ký của User khỏi Event.
     * Điều kiện: sự kiện chưa bắt đầu và đang ở trạng thái REGISTERED.
     *
     * @throws RegistrationException nếu không thể hủy
     */
    public void cancelRegistration(int userId, int eventId)
            throws RegistrationException, EventException, SQLException {

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // --- Kiểm tra bản ghi đăng ký ---
            Registration registration =
                    registrationDAO.findByUserAndEvent(userId, eventId);

            if (registration == null) {
                throw new RegistrationException("Bạn chưa đăng ký sự kiện này.");
            }
            if ("CANCELLED".equals(registration.getStatus())) {
                throw new RegistrationException("Đăng ký này đã được hủy trước đó.");
            }

            // --- Kiểm tra sự kiện chưa bắt đầu ---
            Event event = eventDAO.findByIdForUpdate(eventId, conn);
            if (event == null) {
                throw new EventException("Sự kiện không tồn tại.");
            }
            if (!event.isUpcoming()) {
                throw new RegistrationException(
                        "Không thể hủy đăng ký sự kiện đã bắt đầu hoặc kết thúc."
                );
            }

            // --- Thực hiện hủy ---
            registrationDAO.cancel(userId, eventId, conn);

            // --- Giảm số đăng ký (-1) ---
            eventDAO.decrementRegistered(eventId, conn);

            conn.commit();

        } catch (RegistrationException | EventException e) {
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

    // =====================================================
    // LẤY DANH SÁCH
    // =====================================================

    /**
     * Lấy tất cả đăng ký của 1 user (cho trang My Events).
     */
    public List<Registration> getUserRegistrations(int userId) throws SQLException {
        return registrationDAO.findAllByUser(userId);
    }

    /**
     * Lấy tất cả đăng ký của 1 event (cho Admin xem).
     */
    public List<Registration> getEventRegistrations(int eventId) throws SQLException {
        return registrationDAO.findAllByEvent(eventId);
    }

    /**
     * Kiểm tra user có đang đăng ký (REGISTERED) event này không.
     * Dùng để hiển thị đúng button trong trang chi tiết sự kiện.
     */
    public Registration getUserRegistration(int userId,
                                            int eventId) throws SQLException {
        return registrationDAO.findByUserAndEvent(userId, eventId);
    }
}