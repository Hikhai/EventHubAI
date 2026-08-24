package com.eventhub.service;

import com.eventhub.config.DBConnection;
import com.eventhub.dao.EventDAO;
import com.eventhub.dao.RegistrationDAO;
import com.eventhub.exception.EventException;
import com.eventhub.exception.RegistrationException;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý đăng ký và hủy đăng ký sự kiện.
 * Dùng transaction + SELECT FOR UPDATE để tránh race condition.
 */
public class RegistrationService {

    private final RegistrationDAO registrationDAO = new RegistrationDAO();
    private final EventDAO eventDAO = new EventDAO();

    public void registerEvent(int userId, int eventId)
            throws EventException, RegistrationException, SQLException {

        try {
            DBConnection.inTransaction(conn -> {
                Event event = eventDAO.findByIdForUpdate(eventId, conn);

                if (event == null) {
                    throw new EventException("Sự kiện không tồn tại.");
                }
                if (!"PUBLISHED".equals(event.getStatus())) {
                    throw new EventException("Sự kiện này hiện không nhận đăng ký.");
                }
                if (!event.isFree()) {
                    throw new RegistrationException(
                            "Sự kiện có phí, vui lòng sử dụng nút Thanh toán để mua vé."
                    );
                }
                if (event.isEnded()) {
                    throw new RegistrationException("Sự kiện đã kết thúc, không thể đăng ký.");
                }
                if (!event.isUpcoming()) {
                    throw new RegistrationException("Sự kiện đã bắt đầu, không thể đăng ký.");
                }
                if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
                    throw new RegistrationException("Đã hết hạn đăng ký sự kiện này.");
                }
                if (event.isFull()) {
                    throw new RegistrationException("Sự kiện đã đủ số lượng người tham gia.");
                }

                Registration existing = registrationDAO.findByUserAndEvent(userId, eventId, conn);

                if (existing != null && "REGISTERED".equals(existing.getStatus())) {
                    throw new RegistrationException("Bạn đã đăng ký sự kiện này rồi.");
                }

                if (existing != null && "CANCELLED".equals(existing.getStatus())) {
                    registrationDAO.reactivate(userId, eventId, conn);
                } else {
                    registrationDAO.insert(userId, eventId, conn);
                }

                eventDAO.incrementRegistered(eventId, conn);
            });
        } catch (EventException | RegistrationException | SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public void cancelRegistration(int userId, int eventId)
            throws RegistrationException, EventException, SQLException {

        try {
            DBConnection.inTransaction(conn -> {
                Registration registration =
                        registrationDAO.findByUserAndEvent(userId, eventId, conn);

                if (registration == null) {
                    throw new RegistrationException("Bạn chưa đăng ký sự kiện này.");
                }
                if ("CANCELLED".equals(registration.getStatus())) {
                    throw new RegistrationException("Đăng ký này đã được hủy trước đó.");
                }
                if ("PENDING_PAYMENT".equals(registration.getStatus())) {
                    throw new RegistrationException(
                            "Đăng ký đang chờ thanh toán; hãy hủy từ trang thanh toán."
                    );
                }

                Event event = eventDAO.findByIdForUpdate(eventId, conn);
                if (event == null) {
                    throw new EventException("Sự kiện không tồn tại.");
                }
                if (!event.isUpcoming()) {
                    throw new RegistrationException(
                            "Không thể hủy đăng ký sự kiện đã bắt đầu hoặc kết thúc."
                    );
                }
                if (!event.isFree()) {
                    throw new RegistrationException(
                            "Vé đã thanh toán không thể tự hủy. Vui lòng liên hệ ban tổ chức để được hỗ trợ hoàn tiền."
                    );
                }

                registrationDAO.cancel(userId, eventId, conn);
                eventDAO.decrementRegistered(eventId, conn);
            });
        } catch (RegistrationException | EventException | SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public List<Registration> getUserRegistrations(int userId) throws SQLException {
        return registrationDAO.findAllByUser(userId);
    }

    public List<Registration> getEventRegistrations(int eventId) throws SQLException {
        return registrationDAO.findAllByEvent(eventId);
    }

    public Registration getUserRegistration(int userId, int eventId) throws SQLException {
        return registrationDAO.findByUserAndEvent(userId, eventId);
    }
}
