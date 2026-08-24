package com.eventhub.service;

import com.eventhub.config.DBConnection;
import com.eventhub.config.PaymentConfig;
import com.eventhub.dao.EventDAO;
import com.eventhub.dao.PaymentDAO;
import com.eventhub.dao.RegistrationDAO;
import com.eventhub.exception.EventException;
import com.eventhub.exception.PaymentException;
import com.eventhub.exception.RegistrationException;
import com.eventhub.model.Event;
import com.eventhub.model.Payment;
import com.eventhub.model.Registration;
import com.eventhub.payment.PaymentGateway;
import com.eventhub.payment.PaymentGatewayFactory;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Điều phối giữ chỗ, checkout và cập nhật trạng thái thanh toán. */
public class PaymentService {
    private static final Set<String> ADMIN_STATUSES = Set.of(
            "PENDING", "PAID", "FAILED", "CANCELLED", "EXPIRED",
            "REFUND_PENDING", "REFUNDED"
    );
    private static final DateTimeFormatter CODE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final RegistrationDAO registrationDAO = new RegistrationDAO();
    private final EventDAO eventDAO = new EventDAO();

    public Payment createCheckout(int userId, int eventId, HttpServletRequest request)
            throws PaymentException, EventException, RegistrationException, SQLException {
        PaymentGateway gateway = PaymentGatewayFactory.createConfiguredGateway();
        Payment payment;

        try {
            payment = DBConnection.inTransaction(conn -> {
                Event event = eventDAO.findByIdForUpdate(eventId, conn);
                validatePaidEvent(event);

                Registration registration = registrationDAO
                        .findByUserAndEventForUpdate(userId, eventId, conn);

                if (registration != null && "REGISTERED".equals(registration.getStatus())) {
                    throw new RegistrationException("Bạn đã có vé của sự kiện này.");
                }

                if (registration != null && "PENDING_PAYMENT".equals(registration.getStatus())) {
                    Payment pending = paymentDAO.findLatestPendingByRegistrationForUpdate(
                            registration.getRegistrationId(), conn);
                    if (pending != null && pending.isPendingAndActive()
                            && gateway.getProviderCode().equals(pending.getProvider())) {
                        return pending;
                    }
                    if (pending != null && pending.isPending()) {
                        String terminalStatus = pending.isPendingAndActive()
                                ? "CANCELLED" : "EXPIRED";
                        paymentDAO.markTerminal(pending.getPaymentId(), terminalStatus, null,
                                "Đóng giao dịch cũ trước khi tạo checkout mới", conn);
                    }
                    registrationDAO.cancelById(registration.getRegistrationId(), conn);
                    eventDAO.decrementRegistered(eventId, conn);
                    event.setCurrentRegistered(Math.max(event.getCurrentRegistered() - 1, 0));
                    registration.setStatus("CANCELLED");
                }

                if (event.isFull()) {
                    throw new RegistrationException("Sự kiện đã đủ số lượng người tham gia.");
                }

                int registrationId;
                if (registration == null) {
                    registrationId = registrationDAO.insertPending(userId, eventId, conn);
                } else {
                    registrationId = registration.getRegistrationId();
                    registrationDAO.markPending(registrationId, conn);
                }
                eventDAO.incrementRegistered(eventId, conn);

                Payment created = new Payment();
                created.setPaymentCode(generatePaymentCode());
                created.setRegistrationId(registrationId);
                created.setUserId(userId);
                created.setEventId(eventId);
                created.setAmount(event.getTicketPrice());
                created.setCurrency(event.getCurrency());
                created.setProvider(gateway.getProviderCode());
                created.setStatus("PENDING");
                created.setExpiresAt(LocalDateTime.now()
                        .plusMinutes(PaymentConfig.getHoldMinutes()));
                created.setPaymentId(paymentDAO.insert(created, conn));
                return created;
            });
        } catch (EventException | RegistrationException | PaymentException | SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Không thể tạo giao dịch thanh toán.", e);
        }

        if (payment.getCheckoutUrl() != null && !payment.getCheckoutUrl().isBlank()) {
            return payment;
        }

        try {
            String checkoutUrl = gateway.createCheckoutUrl(payment, request);
            paymentDAO.updateCheckoutUrl(payment.getPaymentId(), checkoutUrl);
            payment.setCheckoutUrl(checkoutUrl);
            return payment;
        } catch (PaymentException | RuntimeException e) {
            try {
                terminatePayment(payment.getPaymentCode(), null, "FAILED", null,
                        "Không tạo được trang thanh toán: " + e.getMessage());
            } catch (Exception cleanupError) {
                e.addSuppressed(cleanupError);
            }
            if (e instanceof PaymentException paymentException) throw paymentException;
            throw new PaymentException("Không thể kết nối cổng thanh toán.");
        }
    }

    public Payment completeMockPayment(String paymentCode, int userId)
            throws PaymentException, SQLException {
        return completePayment(paymentCode, "MOCK", userId,
                "MOCK-" + UUID.randomUUID(), "00", "MOCK_BANK");
    }

    public Payment completeVnPayPayment(String paymentCode, String providerTransactionId,
                                        String responseCode, String bankCode)
            throws PaymentException, SQLException {
        return completePayment(paymentCode, "VNPAY", null,
                providerTransactionId, responseCode, bankCode);
    }

    private Payment completePayment(String paymentCode, String expectedProvider, Integer ownerId,
                                    String providerTransactionId, String responseCode,
                                    String bankCode)
            throws PaymentException, SQLException {
        Payment snapshot = paymentDAO.findByCode(paymentCode);
        if (snapshot == null) throw new PaymentException("Không tìm thấy giao dịch.");
        try {
            return DBConnection.inTransaction(conn -> {
                // Thứ tự khóa thống nhất: event -> registration -> payment.
                Event event = eventDAO.findByIdForUpdate(snapshot.getEventId(), conn);
                Registration registration = registrationDAO.findByIdForUpdate(
                        snapshot.getRegistrationId(), conn);
                Payment payment = paymentDAO.findByCodeForUpdate(paymentCode, conn);
                if (payment == null) throw new PaymentException("Không tìm thấy giao dịch.");
                if (!expectedProvider.equals(payment.getProvider())) {
                    throw new PaymentException("Cổng thanh toán không khớp.");
                }
                if (ownerId != null && payment.getUserId() != ownerId) {
                    throw new PaymentException("Bạn không có quyền xử lý giao dịch này.");
                }
                if (payment.isPaid()) return payment; // callback lặp vẫn an toàn
                if (!payment.isPending()) {
                    throw new PaymentException("Giao dịch không còn ở trạng thái chờ.");
                }
                if (!payment.isPendingAndActive()) {
                    throw new PaymentException("Giao dịch đã hết thời gian giữ chỗ.");
                }
                if (registration == null || !"PENDING_PAYMENT".equals(registration.getStatus())) {
                    throw new PaymentException("Đăng ký không còn chờ thanh toán.");
                }
                if (event == null || !"PUBLISHED".equals(event.getStatus())) {
                    throw new PaymentException("Sự kiện không còn nhận thanh toán.");
                }

                paymentDAO.markPaid(payment.getPaymentId(), providerTransactionId,
                        responseCode, bankCode, conn);
                registrationDAO.markRegistered(registration.getRegistrationId(), conn);
                payment.setStatus("PAID");
                payment.setProviderTransactionId(providerTransactionId);
                payment.setProviderResponseCode(responseCode);
                payment.setBankCode(bankCode);
                payment.setPaidAt(LocalDateTime.now());
                return payment;
            });
        } catch (PaymentException | SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Không thể xác nhận thanh toán.", e);
        }
    }

    public Payment cancelMockPayment(String paymentCode, int userId, boolean failed)
            throws PaymentException, SQLException {
        return terminatePayment(paymentCode, userId, failed ? "FAILED" : "CANCELLED",
                failed ? "99" : "24",
                failed ? "Giao dịch mô phỏng thất bại" : "Người dùng hủy thanh toán");
    }

    public Payment cancelPendingReservation(int userId, int eventId)
            throws PaymentException, SQLException {
        Payment payment = paymentDAO.findPendingByUserAndEvent(userId, eventId);
        if (payment == null) {
            throw new PaymentException("Không tìm thấy giao dịch đang chờ để hủy.");
        }
        return terminatePayment(payment.getPaymentCode(), userId, "CANCELLED", "24",
                "Người dùng hủy giữ chỗ");
    }

    public Payment recordVnPayFailure(String paymentCode, String responseCode)
            throws PaymentException, SQLException {
        return terminatePayment(paymentCode, null, "FAILED", responseCode,
                "VNPAY từ chối hoặc không hoàn tất giao dịch");
    }

    private Payment terminatePayment(String paymentCode, Integer ownerId, String terminalStatus,
                                     String responseCode, String reason)
            throws PaymentException, SQLException {
        Payment snapshot = paymentDAO.findByCode(paymentCode);
        if (snapshot == null) throw new PaymentException("Không tìm thấy giao dịch.");
        try {
            return DBConnection.inTransaction(conn -> {
                Event event = eventDAO.findByIdForUpdate(snapshot.getEventId(), conn);
                Registration registration = registrationDAO.findByIdForUpdate(
                        snapshot.getRegistrationId(), conn);
                Payment payment = paymentDAO.findByCodeForUpdate(paymentCode, conn);
                if (payment == null) throw new PaymentException("Không tìm thấy giao dịch.");
                if (ownerId != null && payment.getUserId() != ownerId) {
                    throw new PaymentException("Bạn không có quyền xử lý giao dịch này.");
                }
                if (!payment.isPending()) return payment; // idempotent

                if (registration != null && "PENDING_PAYMENT".equals(registration.getStatus())) {
                    registrationDAO.cancelById(registration.getRegistrationId(), conn);
                    if (event != null) eventDAO.decrementRegistered(event.getEventId(), conn);
                }
                paymentDAO.markTerminal(payment.getPaymentId(), terminalStatus,
                        responseCode, reason, conn);
                payment.setStatus(terminalStatus);
                payment.setFailureReason(reason);
                return payment;
            });
        } catch (PaymentException | SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Không thể đóng giao dịch.", e);
        }
    }

    public int expirePendingPayments() throws SQLException {
        int expired = 0;
        for (String code : paymentDAO.findExpiredPendingCodes(100)) {
            try {
                terminatePayment(code, null, "EXPIRED", null, "Hết thời gian giữ chỗ");
                expired++;
            } catch (PaymentException e) {
                System.err.println("[PaymentExpiry] " + code + ": " + e.getMessage());
            }
        }
        return expired;
    }

    public Payment getPayment(String paymentCode) throws SQLException {
        return paymentDAO.findByCode(paymentCode);
    }

    public Payment getPaymentForUser(String paymentCode, int userId)
            throws PaymentException, SQLException {
        Payment payment = paymentDAO.findByCode(paymentCode);
        if (payment == null || payment.getUserId() != userId) {
            throw new PaymentException("Không tìm thấy giao dịch của bạn.");
        }
        return payment;
    }

    public Payment getPendingPayment(int userId, int eventId) throws SQLException {
        Payment payment = paymentDAO.findPendingByUserAndEvent(userId, eventId);
        return payment != null && payment.isPendingAndActive() ? payment : null;
    }

    public List<Payment> getUserPayments(int userId) throws SQLException {
        return paymentDAO.findAllByUser(userId);
    }

    public List<Payment> getAdminPayments(String status) throws SQLException {
        String validStatus = status != null && ADMIN_STATUSES.contains(status) ? status : null;
        return paymentDAO.findAll(validStatus);
    }

    /** Admin xác nhận đã hoàn tiền thủ công; không gọi API tiền thật. */
    public void confirmManualRefund(String paymentCode)
            throws PaymentException, SQLException {
        try {
            DBConnection.inTransaction(conn -> {
                Payment payment = paymentDAO.findByCodeForUpdate(paymentCode, conn);
                if (payment == null) throw new PaymentException("Không tìm thấy giao dịch.");
                if ("REFUNDED".equals(payment.getStatus())) return;
                if (!"REFUND_PENDING".equals(payment.getStatus())) {
                    throw new PaymentException("Giao dịch không ở trạng thái chờ hoàn tiền.");
                }
                paymentDAO.markRefunded(payment.getPaymentId(), conn);
            });
        } catch (PaymentException | SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Không thể xác nhận hoàn tiền.", e);
        }
    }

    public BigDecimal calculatePaidRevenue(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> "PAID".equals(payment.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validatePaidEvent(Event event)
            throws EventException, RegistrationException, PaymentException {
        if (event == null) throw new EventException("Sự kiện không tồn tại.");
        if (event.isFree()) throw new PaymentException("Sự kiện này không cần thanh toán.");
        if (!"PUBLISHED".equals(event.getStatus())) {
            throw new EventException("Sự kiện này hiện không nhận đăng ký.");
        }
        if (event.isEnded() || !event.isUpcoming()) {
            throw new RegistrationException("Sự kiện đã bắt đầu hoặc kết thúc.");
        }
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new RegistrationException("Đã hết hạn đăng ký sự kiện này.");
        }
    }

    private String generatePaymentCode() {
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase();
        return "EH" + LocalDateTime.now().format(CODE_TIME) + random;
    }
}
