package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.model.Payment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** JDBC DAO cho bảng payments và các JOIN phục vụ lịch sử giao dịch. */
public class PaymentDAO {
    private static final String BASE_SELECT =
            "SELECT p.*, r.user_id, r.event_id, u.full_name AS user_full_name, " +
                    "u.email AS user_email, e.title AS event_title " +
                    "FROM payments p " +
                    "JOIN registrations r ON r.registration_id=p.registration_id " +
                    "JOIN users u ON u.user_id=r.user_id " +
                    "JOIN events e ON e.event_id=r.event_id ";

    public long insert(Payment payment, Connection conn) throws SQLException {
        String sql = "INSERT INTO payments (payment_code, registration_id, amount, currency, " +
                "provider, status, expires_at) VALUES (?, ?, ?, ?, ?, 'PENDING', ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, payment.getPaymentCode());
            stmt.setInt(2, payment.getRegistrationId());
            stmt.setBigDecimal(3, payment.getAmount());
            stmt.setString(4, payment.getCurrency());
            stmt.setString(5, payment.getProvider());
            stmt.setTimestamp(6, Timestamp.valueOf(payment.getExpiresAt()));
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Không thể lấy ID giao dịch sau khi insert.");
    }

    public Payment findByCode(String paymentCode) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findOne(conn, BASE_SELECT + "WHERE p.payment_code=?", paymentCode);
        }
    }

    public Payment findByCodeForUpdate(String paymentCode, Connection conn) throws SQLException {
        return findOne(conn, BASE_SELECT + "WHERE p.payment_code=? FOR UPDATE", paymentCode);
    }

    public Payment findLatestPendingByRegistrationForUpdate(int registrationId, Connection conn)
            throws SQLException {
        String sql = BASE_SELECT + "WHERE p.registration_id=? AND p.status='PENDING' " +
                "ORDER BY p.created_at DESC, p.payment_id DESC LIMIT 1 FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, registrationId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Payment findPendingByUserAndEvent(int userId, int eventId) throws SQLException {
        String sql = BASE_SELECT + "WHERE r.user_id=? AND r.event_id=? AND p.status='PENDING' " +
                "ORDER BY p.created_at DESC, p.payment_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Payment> findAllByUser(int userId) throws SQLException {
        String sql = BASE_SELECT + "WHERE r.user_id=? ORDER BY p.created_at DESC, p.payment_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return mapList(stmt);
        }
    }

    public List<Payment> findAll(String status) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        if (status != null && !status.isBlank()) sql.append("WHERE p.status=? ");
        sql.append("ORDER BY p.created_at DESC, p.payment_id DESC");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (status != null && !status.isBlank()) stmt.setString(1, status);
            return mapList(stmt);
        }
    }

    public List<String> findExpiredPendingCodes(int limit) throws SQLException {
        String sql = "SELECT payment_code FROM payments " +
                "WHERE status='PENDING' AND expires_at <= NOW() ORDER BY expires_at LIMIT ?";
        List<String> codes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) codes.add(rs.getString(1));
            }
        }
        return codes;
    }

    public int countByEvent(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payments p JOIN registrations r " +
                "ON r.registration_id=p.registration_id WHERE r.event_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void updateCheckoutUrl(long paymentId, String checkoutUrl) throws SQLException {
        String sql = "UPDATE payments SET checkout_url=? WHERE payment_id=? AND status='PENDING'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, checkoutUrl);
            stmt.setLong(2, paymentId);
            stmt.executeUpdate();
        }
    }

    public void markPaid(long paymentId, String providerTransactionId,
                         String responseCode, String bankCode, Connection conn) throws SQLException {
        String sql = "UPDATE payments SET status='PAID', provider_transaction_id=?, " +
                "provider_response_code=?, bank_code=?, paid_at=NOW(), failure_reason=NULL " +
                "WHERE payment_id=? AND status='PENDING'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, providerTransactionId);
            stmt.setString(2, responseCode);
            stmt.setString(3, bankCode);
            stmt.setLong(4, paymentId);
            stmt.executeUpdate();
        }
    }

    public void markTerminal(long paymentId, String status, String responseCode,
                             String failureReason, Connection conn) throws SQLException {
        String sql = "UPDATE payments SET status=?, provider_response_code=?, failure_reason=? " +
                "WHERE payment_id=? AND status='PENDING'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, responseCode);
            stmt.setString(3, failureReason);
            stmt.setLong(4, paymentId);
            stmt.executeUpdate();
        }
    }

    public void markRefunded(long paymentId, Connection conn) throws SQLException {
        String sql = "UPDATE payments SET status='REFUNDED', refunded_at=NOW(), " +
                "failure_reason=NULL WHERE payment_id=? AND status='REFUND_PENDING'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, paymentId);
            stmt.executeUpdate();
        }
    }

    private Payment findOne(Connection conn, String sql, String paymentCode) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paymentCode);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private List<Payment> mapList(PreparedStatement stmt) throws SQLException {
        List<Payment> list = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getLong("payment_id"));
        payment.setPaymentCode(rs.getString("payment_code"));
        payment.setRegistrationId(rs.getInt("registration_id"));
        payment.setUserId(rs.getInt("user_id"));
        payment.setEventId(rs.getInt("event_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setCurrency(rs.getString("currency"));
        payment.setProvider(rs.getString("provider"));
        payment.setStatus(rs.getString("status"));
        payment.setProviderTransactionId(rs.getString("provider_transaction_id"));
        payment.setProviderResponseCode(rs.getString("provider_response_code"));
        payment.setBankCode(rs.getString("bank_code"));
        payment.setCheckoutUrl(rs.getString("checkout_url"));
        payment.setFailureReason(rs.getString("failure_reason"));
        payment.setUserFullName(rs.getString("user_full_name"));
        payment.setUserEmail(rs.getString("user_email"));
        payment.setEventTitle(rs.getString("event_title"));
        payment.setExpiresAt(toLocalDateTime(rs.getTimestamp("expires_at")));
        payment.setPaidAt(toLocalDateTime(rs.getTimestamp("paid_at")));
        payment.setRefundedAt(toLocalDateTime(rs.getTimestamp("refunded_at")));
        payment.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        payment.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return payment;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
