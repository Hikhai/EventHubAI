-- =============================================================
-- EventHub AI - Migration: sự kiện có phí và thanh toán
-- MySQL 8.x
--
-- Cách dùng:
--   1. Sao lưu database hiện tại.
--   2. Chọn đúng database eventhub_db.
--   3. Chạy toàn bộ file này MỘT LẦN sau eventhub_db.sql.
--
-- Lưu ý: DDL của MySQL tự động COMMIT, vì vậy không bọc migration
-- này trong START TRANSACTION/ROLLBACK.
-- =============================================================

USE eventhub_db;

-- 1. Giá vé của sự kiện.
-- ticket_price = 0 nghĩa là sự kiện miễn phí.
-- Các sự kiện hiện có tự động giữ nguyên là miễn phí nhờ DEFAULT 0.
ALTER TABLE events
    ADD COLUMN ticket_price DECIMAL(12, 0) NOT NULL DEFAULT 0
        AFTER current_registered,
    ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'VND'
        AFTER ticket_price,
    ADD CONSTRAINT chk_event_ticket_price_non_negative
        CHECK (ticket_price >= 0);

-- 2. PENDING_PAYMENT là đăng ký đang giữ chỗ và chờ thanh toán.
-- Khi thanh toán thành công, ứng dụng chuyển trạng thái sang REGISTERED.
ALTER TABLE registrations
    MODIFY COLUMN status ENUM(
        'PENDING_PAYMENT',
        'REGISTERED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'REGISTERED';

-- 3. Mỗi dòng là một lần thử thanh toán.
-- Một registration có thể có nhiều payment nếu người dùng thanh toán lại
-- sau khi giao dịch cũ thất bại/hết hạn.
CREATE TABLE payments (
    payment_id BIGINT NOT NULL AUTO_INCREMENT,
    payment_code VARCHAR(50) NOT NULL,
    registration_id INT NOT NULL,

    -- Snapshot số tiền tại lúc tạo giao dịch; callback phải đối chiếu
    -- với cột này thay vì đọc lại giá hiện tại trong events.
    amount DECIMAL(12, 0) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    provider VARCHAR(20) NOT NULL,

    status ENUM(
        'PENDING',
        'PAID',
        'FAILED',
        'CANCELLED',
        'EXPIRED',
        'REFUND_PENDING',
        'REFUNDED'
    ) NOT NULL DEFAULT 'PENDING',

    provider_transaction_id VARCHAR(100) DEFAULT NULL,
    provider_response_code VARCHAR(20) DEFAULT NULL,
    bank_code VARCHAR(50) DEFAULT NULL,
    checkout_url VARCHAR(1000) DEFAULT NULL,
    failure_reason VARCHAR(500) DEFAULT NULL,

    -- expires_at là hạn giữ chỗ, ví dụ NOW() + INTERVAL 15 MINUTE.
    expires_at DATETIME NOT NULL,
    paid_at DATETIME DEFAULT NULL,
    refunded_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (payment_id),
    UNIQUE KEY uq_payment_code (payment_code),
    UNIQUE KEY uq_payment_provider_transaction (
        provider,
        provider_transaction_id
    ),
    KEY idx_payment_registration (registration_id),
    KEY idx_payment_status_expiry (status, expires_at),
    KEY idx_payment_created_at (created_at),

    CONSTRAINT fk_payment_registration
        FOREIGN KEY (registration_id)
        REFERENCES registrations (registration_id),
    CONSTRAINT chk_payment_amount_positive
        CHECK (amount > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- DỮ LIỆU DEMO TÙY CHỌN
-- Bỏ dấu -- ở các lệnh dưới nếu muốn chuyển một số sự kiện sang có phí.
-- Không chạy phần này nếu muốn tự nhập giá trên trang Admin sau khi code
-- giao diện quản lý giá vé đã hoàn thành.
-- =============================================================

-- UPDATE events SET ticket_price = 100000 WHERE event_id = 2;
-- UPDATE events SET ticket_price = 150000 WHERE event_id = 3;
-- UPDATE events SET ticket_price = 50000  WHERE event_id = 9;

-- Kiểm tra schema và dữ liệu sau migration:
SELECT event_id, title, ticket_price, currency
FROM events
ORDER BY event_id;

SHOW COLUMNS FROM registrations LIKE 'status';
SHOW CREATE TABLE payments;
