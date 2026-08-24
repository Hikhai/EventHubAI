-- =====================================================================
-- EventHub AI - Full database schema + sample data
-- MySQL 8.x
--
-- Import duy nhất file này. Script tự tạo lại toàn bộ database, bảng,
-- khóa ngoại và dữ liệu mẫu cho sự kiện miễn phí/có phí/thanh toán.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS eventhub_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE eventhub_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS chat_logs;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS registrations;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 1. USERS
-- =====================================================================
CREATE TABLE users (
    user_id INT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO users (user_id, full_name, email, password, role, is_active, created_at) VALUES
(1,  'Quản trị viên', 'admin@eventhub.com', '$2a$10$jwncVjqEzp0IvqGARqzXJOTUHwdfWVnKSkxNMrjiDDcc/TUjJdvzO', 'ADMIN', 1, DATE_SUB(NOW(), INTERVAL 120 DAY)),
(2,  'Nguyễn Văn An', 'an@example.com',      '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 100 DAY)),
(3,  'Trần Thị Bình', 'binh@example.com',    '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 98 DAY)),
(4,  'Lê Văn Cường', 'cuong@example.com',    '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 95 DAY)),
(5,  'Phạm Minh Đức', 'duc@example.com',     '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 90 DAY)),
(6,  'Hoàng Thị Em', 'em@example.com',       '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 88 DAY)),
(7,  'Vũ Quốc Phong', 'phong@example.com',   '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 85 DAY)),
(8,  'Đặng Thu Hà', 'ha@example.com',        '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 82 DAY)),
(9,  'Bùi Gia Huy', 'huy@example.com',       '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 80 DAY)),
(10, 'Ngô Bảo Ngọc', 'ngoc@example.com',     '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 78 DAY)),
(11, 'Đỗ Thanh Tùng', 'tung@example.com',    '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 75 DAY)),
(12, 'Lý Mỹ Linh', 'linh@example.com',       '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 72 DAY)),
(13, 'Trịnh Văn Khoa', 'khoa@example.com',   '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 70 DAY)),
(14, 'Mai Anh Thư', 'thu@example.com',       '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 68 DAY)),
(15, 'Phan Đức Thịnh', 'thinh@example.com',  '$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK', 'USER', 1, DATE_SUB(NOW(), INTERVAL 65 DAY));

-- =====================================================================
-- 2. CATEGORIES
-- =====================================================================
CREATE TABLE categories (
    category_id INT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO categories (category_id, category_name, description) VALUES
(1, 'Hội thảo', 'Các buổi hội thảo chuyên đề, seminar, talkshow'),
(2, 'Workshop', 'Buổi thực hành kỹ năng, hands-on training'),
(3, 'Buổi họp', 'Họp câu lạc bộ, sinh hoạt định kỳ'),
(4, 'Hoạt động ngoại khóa', 'Picnic, teambuilding, thiện nguyện'),
(5, 'Cuộc thi', 'Hackathon, cuộc thi học thuật và sáng tạo'),
(6, 'Khác', 'Các sự kiện khác');

-- =====================================================================
-- 3. EVENTS
-- ticket_price = 0: miễn phí; > 0: phải thanh toán mới xác nhận vé.
-- current_registered tính cả REGISTERED và PENDING_PAYMENT đang giữ chỗ.
-- =====================================================================
CREATE TABLE events (
    event_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    summary_ai TEXT DEFAULT NULL,
    image_path VARCHAR(500) DEFAULT NULL,
    image_source ENUM('UPLOADED', 'AI_GENERATED', 'DEFAULT') NOT NULL DEFAULT 'DEFAULT',
    location VARCHAR(300) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    registration_deadline DATETIME NOT NULL,
    max_participants INT NOT NULL,
    current_registered INT NOT NULL DEFAULT 0,
    ticket_price DECIMAL(12,0) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    avg_rating DECIMAL(3,1) NOT NULL DEFAULT 0.0,
    total_reviews INT NOT NULL DEFAULT 0,
    status ENUM('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'DRAFT',
    category_id INT NOT NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    KEY idx_events_status_time (status, start_time),
    KEY idx_events_category (category_id),
    KEY idx_events_created_by (created_by),
    CONSTRAINT fk_event_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
    CONSTRAINT fk_event_creator FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT chk_event_time CHECK (end_time > start_time),
    CONSTRAINT chk_event_deadline CHECK (registration_deadline <= start_time),
    CONSTRAINT chk_event_capacity CHECK (max_participants > 0),
    CONSTRAINT chk_event_registered CHECK (
        current_registered >= 0 AND current_registered <= max_participants
    ),
    CONSTRAINT chk_event_ticket_price CHECK (ticket_price >= 0),
    CONSTRAINT chk_event_rating CHECK (avg_rating BETWEEN 0.0 AND 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO events (
    event_id, title, description, summary_ai, image_path, image_source,
    location, start_time, end_time, registration_deadline,
    max_participants, current_registered, ticket_price, currency,
    avg_rating, total_reviews, status, category_id, created_by, created_at, updated_at
) VALUES
(1, 'Workshop Java nâng cao và Payment Gateway',
 'Workshop thực hành Servlet, JSP, JDBC transaction, HMAC và tích hợp cổng thanh toán. Sinh viên xây dựng trọn vẹn luồng giữ chỗ, callback và lịch sử giao dịch.',
 'Xây dựng luồng mua vé an toàn bằng Java Servlet, JDBC transaction và HMAC.',
 'default_workshop.jpg', 'DEFAULT', 'Phòng Lab Java A1',
 DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 5 DAY),
 30, 3, 120000, 'VND', 0.0, 0, 'PUBLISHED', 2, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),

(2, 'Seminar AI ứng dụng - Vé giới hạn',
 'Seminar giới thiệu cách xây dựng ứng dụng AI an toàn. Bốn vé đã thanh toán và một vé đang được giữ để kiểm thử trường hợp hết chỗ.',
 'Sự kiện gần hết vé để kiểm thử đồng thời và giữ chỗ.',
 'default_hoithao.jpg', 'DEFAULT', 'Hội trường B',
 DATE_ADD(NOW(), INTERVAL 10 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 10 DAY), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 8 DAY),
 5, 5, 200000, 'VND', 0.0, 0, 'PUBLISHED', 1, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),

(3, 'Ngày hội giao lưu sinh viên miễn phí',
 'Ngày hội giao lưu, trò chơi nhóm và kết nối câu lạc bộ dành cho toàn bộ sinh viên. Sự kiện hoàn toàn miễn phí.',
 'Đăng ký miễn phí và tham gia ngày hội kết nối sinh viên.',
 'default_ngoaikhoa.jpg', 'DEFAULT', 'Sân trường khu A',
 DATE_ADD(NOW(), INTERVAL 4 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 4 DAY), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY),
 80, 2, 0, 'VND', 0.0, 0, 'PUBLISHED', 4, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

(4, 'Talkshow nghề nghiệp đã hết hạn đăng ký',
 'Talkshow có phí vẫn chưa bắt đầu nhưng hạn đăng ký đã qua, dùng để kiểm thử hệ thống không cho tạo giao dịch mới.',
 'Kiểm thử quy tắc hạn đăng ký trước khi thanh toán.',
 'default_hoithao.jpg', 'DEFAULT', 'Phòng hội thảo C',
 DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 2 DAY), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR),
 40, 1, 80000, 'VND', 0.0, 0, 'PUBLISHED', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),

(5, 'Đêm nhạc Acoustic - Mở bán vé',
 'Sự kiện có phí chưa có người mua, phù hợp để đăng nhập và kiểm thử toàn bộ luồng checkout từ đầu bằng MockPay hoặc VNPAY Sandbox.',
 'Sự kiện sạch để thử mua vé mới.',
 'default_other.jpg', 'DEFAULT', 'Sân khấu ngoài trời',
 DATE_ADD(NOW(), INTERVAL 12 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 12 DAY), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 10 DAY),
 60, 0, 75000, 'VND', 0.0, 0, 'PUBLISHED', 6, 1, NOW(), NOW()),

(6, 'Khóa học Spring và REST API đã hoàn thành',
 'Khóa học có phí đã diễn ra, có hai học viên thanh toán thành công và để lại đánh giá. Dùng để kiểm thử lịch sử giao dịch.',
 'Dữ liệu lịch sử thanh toán và đánh giá sau sự kiện.',
 'default_workshop.jpg', 'DEFAULT', 'Phòng Lab Backend',
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 12 DAY),
 25, 2, 180000, 'VND', 4.5, 2, 'COMPLETED', 2, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),

(7, 'Sự kiện bị hủy cần hoàn tiền',
 'Sự kiện bị hủy sau khi bán vé, có giao dịch chờ hoàn và đã hoàn tiền để admin kiểm tra quy trình đối soát.',
 'Kiểm thử REFUND_PENDING và REFUNDED.',
 'default_other.jpg', 'DEFAULT', 'Địa điểm đã hủy',
 DATE_ADD(NOW(), INTERVAL 9 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 9 DAY), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY),
 50, 0, 250000, 'VND', 0.0, 0, 'CANCELLED', 6, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

(8, 'Cuộc thi lập trình - Thử lại thanh toán',
 'Sự kiện chứa các lượt thanh toán thất bại, bị hủy và hết hạn để kiểm thử chức năng mua vé lại.',
 'Kiểm thử retry sau khi giao dịch không thành công.',
 'default_cuocthi.jpg', 'DEFAULT', 'Trung tâm CNTT',
 DATE_ADD(NOW(), INTERVAL 14 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 14 DAY), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 12 DAY),
 70, 0, 99000, 'VND', 0.0, 0, 'PUBLISHED', 5, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),

(9, 'Workshop Git và GitHub miễn phí',
 'Thực hành Git, branch, pull request và xử lý conflict trong dự án nhóm. Không thu phí tham dự.',
 'Làm chủ quy trình Git/GitHub cho dự án nhóm.',
 'default_workshop.jpg', 'DEFAULT', 'Phòng Lab 1',
 DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 6 DAY), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 4 DAY),
 35, 0, 0, 'VND', 0.0, 0, 'PUBLISHED', 2, 1, NOW(), NULL),

(10, 'Teambuilding Xanh - Green Campus Day',
 'Hoạt động ngoại khóa kết hợp trồng cây, dọn vệ sinh và trò chơi gắn kết. Người tham gia được cấp chứng nhận tình nguyện.',
 'Ngày hội xanh miễn phí, năng động và ý nghĩa.',
 'default_ngoaikhoa.jpg', 'DEFAULT', 'Sân vận động trường',
 DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 8 DAY), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 6 DAY),
 80, 3, 0, 'VND', 0.0, 0, 'PUBLISHED', 4, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),

(11, 'Hackathon AI 24h - Bản nháp',
 'Cuộc thi lập trình có phí đang hoàn thiện thể lệ và chưa được công bố cho người dùng.',
 'Sự kiện nháp để kiểm tra quyền truy cập.',
 'default_cuocthi.jpg', 'DEFAULT', 'Khu công nghệ',
 DATE_ADD(NOW(), INTERVAL 15 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 15 DAY), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 13 DAY),
 100, 0, 300000, 'VND', 0.0, 0, 'DRAFT', 5, 1, NOW(), NULL),

(12, 'Sinh hoạt CLB Tiếng Anh đã hoàn thành',
 'Buổi sinh hoạt luyện thuyết trình tiếng Anh, phản biện và xử lý câu hỏi dành cho thành viên câu lạc bộ.',
 'Sự kiện miễn phí đã hoàn thành.',
 'default_hoihop.jpg', 'DEFAULT', 'Phòng D101',
 DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 22 DAY),
 30, 3, 0, 'VND', 0.0, 0, 'COMPLETED', 3, 1, DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),

(13, 'Workshop Cloud đã hủy',
 'Workshop miễn phí bị hủy do diễn giả thay đổi lịch đột xuất.',
 'Sự kiện miễn phí đã hủy.',
 'default_workshop.jpg', 'DEFAULT', 'Phòng Lab Cloud',
 DATE_ADD(NOW(), INTERVAL 11 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 11 DAY), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 9 DAY),
 45, 0, 0, 'VND', 0.0, 0, 'CANCELLED', 2, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),

(14, 'Design Sprint 12h - Vé sớm',
 'Cuộc thi thiết kế sản phẩm số theo đội, bao gồm research, wireframe, giao diện hoàn chỉnh và pitching.',
 'Mở bán vé sớm cho cuộc thi Design Sprint.',
 'default_cuocthi.jpg', 'DEFAULT', 'Studio Design',
 DATE_ADD(NOW(), INTERVAL 20 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 20 DAY), INTERVAL 12 HOUR), DATE_ADD(NOW(), INTERVAL 17 DAY),
 60, 0, 150000, 'VND', 0.0, 0, 'PUBLISHED', 5, 1, NOW(), NULL);

-- =====================================================================
-- 4. REGISTRATIONS
-- =====================================================================
CREATE TABLE registrations (
    registration_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    status ENUM('PENDING_PAYMENT', 'REGISTERED', 'CANCELLED') NOT NULL DEFAULT 'REGISTERED',
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at DATETIME DEFAULT NULL,
    PRIMARY KEY (registration_id),
    UNIQUE KEY uq_registration_user_event (user_id, event_id),
    KEY idx_registration_user (user_id),
    KEY idx_registration_event_status (event_id, status),
    CONSTRAINT fk_registration_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_registration_event FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO registrations (registration_id, user_id, event_id, status, registered_at, cancelled_at) VALUES
(1,  2, 1,  'REGISTERED',      DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
(2,  3, 1,  'PENDING_PAYMENT', NOW(), NULL),
(3,  4, 1,  'CANCELLED',       DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(4,  5, 1,  'REGISTERED',      DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(5,  6, 2,  'REGISTERED',      DATE_SUB(NOW(), INTERVAL 4 DAY), NULL),
(6,  7, 2,  'REGISTERED',      DATE_SUB(NOW(), INTERVAL 4 DAY), NULL),
(7,  8, 2,  'REGISTERED',      DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(8,  9, 2,  'REGISTERED',      DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
(9, 10, 2,  'PENDING_PAYMENT', NOW(), NULL),
(10, 11, 3, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(11, 12, 3, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(12, 13, 4, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(13, 2, 6,  'REGISTERED',      DATE_SUB(NOW(), INTERVAL 20 DAY), NULL),
(14, 3, 6,  'REGISTERED',      DATE_SUB(NOW(), INTERVAL 19 DAY), NULL),
(15, 2, 7,  'CANCELLED',       DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(16, 3, 7,  'CANCELLED',       DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17, 4, 8,  'CANCELLED',       DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(18, 5, 8,  'CANCELLED',       DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(19, 6, 8,  'CANCELLED',       DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(20, 7, 10, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(21, 8, 10, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
(22, 9, 10, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(23, 10, 12, 'REGISTERED',     DATE_SUB(NOW(), INTERVAL 28 DAY), NULL),
(24, 11, 12, 'REGISTERED',     DATE_SUB(NOW(), INTERVAL 27 DAY), NULL),
(25, 12, 12, 'REGISTERED',     DATE_SUB(NOW(), INTERVAL 26 DAY), NULL);

-- =====================================================================
-- 5. PAYMENTS
-- Một registration có thể có nhiều lần thử thanh toán.
-- =====================================================================
CREATE TABLE payments (
    payment_id BIGINT NOT NULL AUTO_INCREMENT,
    payment_code VARCHAR(50) NOT NULL,
    registration_id INT NOT NULL,
    amount DECIMAL(12,0) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    provider VARCHAR(20) NOT NULL,
    status ENUM(
        'PENDING', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED',
        'REFUND_PENDING', 'REFUNDED'
    ) NOT NULL DEFAULT 'PENDING',
    provider_transaction_id VARCHAR(100) DEFAULT NULL,
    provider_response_code VARCHAR(20) DEFAULT NULL,
    bank_code VARCHAR(50) DEFAULT NULL,
    checkout_url VARCHAR(1000) DEFAULT NULL,
    failure_reason VARCHAR(500) DEFAULT NULL,
    expires_at DATETIME NOT NULL,
    paid_at DATETIME DEFAULT NULL,
    refunded_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uq_payment_code (payment_code),
    UNIQUE KEY uq_payment_provider_transaction (provider, provider_transaction_id),
    KEY idx_payment_registration (registration_id),
    KEY idx_payment_status_expiry (status, expires_at),
    KEY idx_payment_created_at (created_at),
    CONSTRAINT fk_payment_registration FOREIGN KEY (registration_id)
        REFERENCES registrations(registration_id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO payments (
    payment_id, payment_code, registration_id, amount, currency, provider, status,
    provider_transaction_id, provider_response_code, bank_code, checkout_url,
    failure_reason, expires_at, paid_at, refunded_at, created_at, updated_at
) VALUES
(1, 'EH_DEMO_0001_A1', 1, 120000, 'VND', 'VNPAY', 'FAILED', 'VNP_FAIL_0001', '99', NULL, NULL,
 'Lỗi giả lập từ cổng thanh toán', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 49 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(2, 'EH_DEMO_0001_A2', 1, 120000, 'VND', 'VNPAY', 'PAID', 'VNP_PAID_0001', '00', 'NCB', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR)),
(3, 'EH_DEMO_0002_A1', 2, 120000, 'VND', 'MOCK', 'PENDING', NULL, NULL, NULL, NULL,
 NULL, DATE_ADD(NOW(), INTERVAL 15 MINUTE), NULL, NULL, NOW(), NULL),
(4, 'EH_DEMO_0003_A1', 3, 120000, 'VND', 'VNPAY', 'FAILED', 'VNP_FAIL_0003', '24', 'NCB', NULL,
 'Khách hàng hủy tại cổng thanh toán', DATE_SUB(NOW(), INTERVAL 23 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(5, 'EH_DEMO_0004_A1', 4, 120000, 'VND', 'MOCK', 'PAID', 'MOCK_PAID_0004', '00', 'MOCK_BANK', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 71 HOUR), DATE_SUB(NOW(), INTERVAL 71 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 71 HOUR)),
(6, 'EH_DEMO_0005_A1', 5, 200000, 'VND', 'VNPAY', 'PAID', 'VNP_PAID_0005', '00', 'NCB', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 95 HOUR), DATE_SUB(NOW(), INTERVAL 95 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 95 HOUR)),
(7, 'EH_DEMO_0006_A1', 6, 200000, 'VND', 'VNPAY', 'PAID', 'VNP_PAID_0006', '00', 'VCB', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 95 HOUR), DATE_SUB(NOW(), INTERVAL 95 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 95 HOUR)),
(8, 'EH_DEMO_0007_A1', 7, 200000, 'VND', 'MOCK', 'PAID', 'MOCK_PAID_0007', '00', 'MOCK_BANK', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 71 HOUR), DATE_SUB(NOW(), INTERVAL 71 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 71 HOUR)),
(9, 'EH_DEMO_0008_A1', 8, 200000, 'VND', 'VNPAY', 'PAID', 'VNP_PAID_0008', '00', 'TCB', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 47 HOUR)),
(10, 'EH_DEMO_0009_A1', 9, 200000, 'VND', 'MOCK', 'PENDING', NULL, NULL, NULL, NULL,
 NULL, DATE_ADD(NOW(), INTERVAL 10 MINUTE), NULL, NULL, NOW(), NULL),
(11, 'EH_DEMO_0012_A1', 12, 80000, 'VND', 'VNPAY', 'PAID', 'VNP_PAID_0012', '00', 'BIDV', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 71 HOUR), DATE_SUB(NOW(), INTERVAL 71 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 71 HOUR)),
(12, 'EH_DEMO_0013_A1', 13, 180000, 'VND', 'VNPAY', 'PAID', 'VNP_PAID_0013', '00', 'VCB', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(13, 'EH_DEMO_0014_A1', 14, 180000, 'VND', 'MOCK', 'PAID', 'MOCK_PAID_0014', '00', 'MOCK_BANK', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY), NULL, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),
(14, 'EH_DEMO_0015_A1', 15, 250000, 'VND', 'VNPAY', 'REFUND_PENDING', 'VNP_PAID_0015', '00', 'NCB', NULL,
 'Sự kiện bị hủy, đang chờ hoàn tiền', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), NULL, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(15, 'EH_DEMO_0016_A1', 16, 250000, 'VND', 'VNPAY', 'REFUNDED', 'VNP_PAID_0016', '00', 'VCB', NULL,
 NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(16, 'EH_DEMO_0017_A1', 17, 99000, 'VND', 'MOCK', 'EXPIRED', NULL, NULL, NULL, NULL,
 'Hết 15 phút giữ chỗ', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL, DATE_SUB(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(17, 'EH_DEMO_0018_A1', 18, 99000, 'VND', 'VNPAY', 'CANCELLED', 'VNP_CANCEL_0018', '24', 'NCB', NULL,
 'Người dùng chủ động hủy giao dịch', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, DATE_SUB(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(18, 'EH_DEMO_0019_A1', 19, 99000, 'VND', 'VNPAY', 'FAILED', 'VNP_FAIL_0019', '99', NULL, NULL,
 'Cổng thanh toán trả lỗi giả lập', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =====================================================================
-- 6. REVIEWS
-- =====================================================================
CREATE TABLE reviews (
    review_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    UNIQUE KEY uq_review_user_event (user_id, event_id),
    KEY idx_review_event (event_id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_review_event FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO reviews (review_id, user_id, event_id, rating, comment, created_at) VALUES
(1, 2, 6, 5, 'Nội dung thực tế, phần transaction và REST API rất hữu ích.', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(2, 3, 6, 4, 'Mentor hỗ trợ tốt, mong có thêm thời gian thực hành.', DATE_SUB(NOW(), INTERVAL 9 DAY));

-- =====================================================================
-- 7. CHAT LOGS
-- =====================================================================
CREATE TABLE chat_logs (
    log_id INT NOT NULL AUTO_INCREMENT,
    user_id INT DEFAULT NULL,
    session_id VARCHAR(100) NOT NULL,
    role ENUM('user', 'assistant') NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_chat_user_session_time (user_id, session_id, created_at, log_id),
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 8. IMPORT CHECKS
-- Các kết quả cuối giúp kiểm tra import và bộ đếm chỗ.
-- Truy vấn mismatch phải trả về 0 dòng.
-- =====================================================================
SELECT 'users' AS table_name, COUNT(*) AS total FROM users
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'events', COUNT(*) FROM events
UNION ALL SELECT 'registrations', COUNT(*) FROM registrations
UNION ALL SELECT 'payments', COUNT(*) FROM payments
UNION ALL SELECT 'reviews', COUNT(*) FROM reviews;

SELECT
    e.event_id,
    e.title,
    e.current_registered AS stored_count,
    SUM(CASE WHEN r.status IN ('REGISTERED', 'PENDING_PAYMENT') THEN 1 ELSE 0 END) AS calculated_count
FROM events e
LEFT JOIN registrations r ON r.event_id = e.event_id
GROUP BY e.event_id, e.title, e.current_registered
HAVING stored_count <> calculated_count;
