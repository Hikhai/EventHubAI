-- =====================================================================
-- EventHub AI - Dữ liệu mẫu kiểm thử sự kiện có phí và thanh toán
-- Yêu cầu: đã chạy eventhub_db.sql và migrate_add_payments.sql
-- MySQL 8.x
--
-- Script tạo 10 sự kiện test, 19 đăng ký và 18 lượt thanh toán, bao phủ:
--   - Sự kiện miễn phí / có phí
--   - Còn chỗ / hết chỗ do có người đang giữ vé
--   - Hết hạn đăng ký / nháp / hoàn thành / bị hủy
--   - PENDING / PAID / FAILED / CANCELLED / EXPIRED
--   - REFUND_PENDING / REFUNDED
--   - Một đăng ký có lần thanh toán đầu thất bại, lần sau thành công
--
-- Có thể chạy lại script. Phần đầu chỉ xóa dữ liệu mang tiền tố
-- "TEST PAYMENT -" đã được tạo bởi chính script này.
-- =====================================================================

USE eventhub_db;

-- ---------------------------------------------------------------------
-- 0. Dọn dữ liệu test cũ để script có thể chạy lại.
-- Không xóa sự kiện thật dù vô tình trùng ID nếu tiêu đề không có prefix.
-- ---------------------------------------------------------------------
DELETE p
FROM payments p
JOIN registrations r ON r.registration_id = p.registration_id
JOIN events e ON e.event_id = r.event_id
WHERE e.title LIKE 'TEST PAYMENT - %';

DELETE rv
FROM reviews rv
JOIN events e ON e.event_id = rv.event_id
WHERE e.title LIKE 'TEST PAYMENT - %';

DELETE r
FROM registrations r
JOIN events e ON e.event_id = r.event_id
WHERE e.title LIKE 'TEST PAYMENT - %';

DELETE FROM events
WHERE title LIKE 'TEST PAYMENT - %';

-- ---------------------------------------------------------------------
-- 1. Sự kiện test.
-- Dùng thời gian tương đối theo NOW() để dữ liệu không bị lỗi thời.
-- ---------------------------------------------------------------------
INSERT INTO events (
    event_id,
    title,
    description,
    summary_ai,
    image_path,
    image_source,
    location,
    start_time,
    end_time,
    registration_deadline,
    max_participants,
    current_registered,
    ticket_price,
    currency,
    avg_rating,
    total_reviews,
    status,
    category_id,
    created_by,
    created_at,
    updated_at
) VALUES
-- 100: Có phí, còn nhiều chỗ; chứa cả PAID, PENDING, FAILED và retry.
(100,
 'TEST PAYMENT - Workshop Java nâng cao có phí',
 'Workshop thực hành Servlet, JSP, JDBC transaction, HMAC và tích hợp cổng thanh toán. Đây là sự kiện mẫu dùng để kiểm thử đầy đủ luồng mua vé.',
 'Kiểm thử luồng mua vé Java nâng cao từ giữ chỗ đến callback thanh toán.',
 'default_workshop.jpg', 'DEFAULT', 'Phòng Lab Java A1',
 DATE_ADD(NOW(), INTERVAL 7 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 3 HOUR),
 DATE_ADD(NOW(), INTERVAL 5 DAY),
 30, 3, 120000, 'VND', 0.0, 0, 'PUBLISHED', 2, 1,
 DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),

-- 101: Đủ chỗ vì 4 vé đã trả tiền và 1 vé đang được giữ.
(101,
 'TEST PAYMENT - Seminar AI gần hết vé',
 'Seminar giới thiệu cách xây dựng ứng dụng AI an toàn, có bốn người đã thanh toán và một người đang giữ vé cuối cùng.',
 'Sự kiện mẫu kiểm thử trường hợp hết vé do giao dịch đang chờ.',
 'default_hoithao.jpg', 'DEFAULT', 'Hội trường TEST B',
 DATE_ADD(NOW(), INTERVAL 10 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 10 DAY), INTERVAL 2 HOUR),
 DATE_ADD(NOW(), INTERVAL 8 DAY),
 5, 5, 200000, 'VND', 0.0, 0, 'PUBLISHED', 1, 1,
 DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),

-- 102: Miễn phí để kiểm tra luồng đăng ký cũ không tạo payment.
(102,
 'TEST PAYMENT - Giao lưu sinh viên miễn phí',
 'Buổi giao lưu dành cho sinh viên, không thu phí và vẫn sử dụng luồng đăng ký trực tiếp như phiên bản cũ.',
 'Sự kiện miễn phí dùng để kiểm tra tính tương thích ngược.',
 'default_ngoaikhoa.jpg', 'DEFAULT', 'Sân trường khu TEST',
 DATE_ADD(NOW(), INTERVAL 4 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 4 DAY), INTERVAL 2 HOUR),
 DATE_ADD(NOW(), INTERVAL 3 DAY),
 80, 2, 0, 'VND', 0.0, 0, 'PUBLISHED', 4, 1,
 DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

-- 103: Chưa diễn ra nhưng đã qua hạn đăng ký.
(103,
 'TEST PAYMENT - Talkshow đã hết hạn đăng ký',
 'Sự kiện có phí vẫn chưa bắt đầu nhưng hạn đăng ký đã qua, dùng để kiểm tra hệ thống không cho tạo giao dịch mới.',
 'Kiểm thử điều kiện hết hạn đăng ký trước khi tạo payment.',
 'default_hoithao.jpg', 'DEFAULT', 'Phòng hội thảo TEST C',
 DATE_ADD(NOW(), INTERVAL 2 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 2 DAY), INTERVAL 2 HOUR),
 DATE_SUB(NOW(), INTERVAL 1 HOUR),
 40, 1, 80000, 'VND', 0.0, 0, 'PUBLISHED', 1, 1,
 DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),

-- 104: Có phí, chưa có đăng ký; phù hợp để login và test checkout mới.
(104,
 'TEST PAYMENT - Vé mới chưa có người mua',
 'Sự kiện mẫu sạch chưa có registration hoặc payment, phù hợp để kiểm tra toàn bộ luồng tạo giao dịch từ đầu.',
 'Đăng nhập bằng tài khoản bất kỳ để thử mua vé mới.',
 'default_other.jpg', 'DEFAULT', 'Nhà đa năng TEST',
 DATE_ADD(NOW(), INTERVAL 12 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 12 DAY), INTERVAL 4 HOUR),
 DATE_ADD(NOW(), INTERVAL 10 DAY),
 60, 0, 75000, 'VND', 0.0, 0, 'PUBLISHED', 6, 1,
 NOW(), NOW()),

-- 105: Bản nháp, user không được nhìn thấy hoặc mua vé.
(105,
 'TEST PAYMENT - Sự kiện có phí đang ở bản nháp',
 'Bản nháp dùng để kiểm tra việc người dùng không thể tạo thanh toán cho sự kiện chưa được xuất bản.',
 'Sự kiện nháp dành cho kiểm thử quyền truy cập.',
 'default_cuocthi.jpg', 'DEFAULT', 'Khu TEST chưa công bố',
 DATE_ADD(NOW(), INTERVAL 15 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 15 DAY), INTERVAL 5 HOUR),
 DATE_ADD(NOW(), INTERVAL 13 DAY),
 100, 0, 300000, 'VND', 0.0, 0, 'DRAFT', 5, 1,
 NOW(), NULL),

-- 106: Đã hoàn thành, có lịch sử thanh toán thành công.
(106,
 'TEST PAYMENT - Khóa học đã hoàn thành',
 'Khóa học có phí đã diễn ra và có hai học viên thanh toán thành công, dùng để kiểm tra lịch sử giao dịch.',
 'Dữ liệu lịch sử cho payment đã thanh toán.',
 'default_workshop.jpg', 'DEFAULT', 'Phòng TEST lịch sử',
 DATE_SUB(NOW(), INTERVAL 10 DAY),
 DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 3 HOUR),
 DATE_SUB(NOW(), INTERVAL 12 DAY),
 25, 2, 180000, 'VND', 0.0, 0, 'COMPLETED', 2, 1,
 DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),

-- 107: Sự kiện bị hủy, có giao dịch chờ hoàn và đã hoàn tiền.
(107,
 'TEST PAYMENT - Sự kiện bị hủy cần hoàn tiền',
 'Sự kiện bị hủy sau khi đã bán vé, dùng để kiểm tra REFUND_PENDING và REFUNDED trên trang quản trị thanh toán.',
 'Kiểm thử quy trình hoàn tiền khi admin hủy sự kiện.',
 'default_other.jpg', 'DEFAULT', 'Địa điểm TEST đã hủy',
 DATE_ADD(NOW(), INTERVAL 9 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 9 DAY), INTERVAL 2 HOUR),
 DATE_ADD(NOW(), INTERVAL 7 DAY),
 50, 0, 250000, 'VND', 0.0, 0, 'CANCELLED', 6, 1,
 DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 108: Nhiều giao dịch lỗi/hủy/hết hạn; user có thể thanh toán lại.
(108,
 'TEST PAYMENT - Thử lại giao dịch thất bại',
 'Sự kiện chứa các đăng ký đã hủy do payment FAILED, CANCELLED và EXPIRED, dùng để kiểm tra chức năng đăng ký và thanh toán lại.',
 'Kiểm thử retry sau khi giao dịch không thành công.',
 'default_cuocthi.jpg', 'DEFAULT', 'Trung tâm TEST giao dịch',
 DATE_ADD(NOW(), INTERVAL 14 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 14 DAY), INTERVAL 6 HOUR),
 DATE_ADD(NOW(), INTERVAL 12 DAY),
 70, 0, 99000, 'VND', 0.0, 0, 'PUBLISHED', 5, 1,
 DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),

-- 109: Miễn phí và chưa ai đăng ký.
(109,
 'TEST PAYMENT - Workshop miễn phí chưa có đăng ký',
 'Sự kiện miễn phí chưa có người đăng ký, dùng để so sánh trực tiếp với nút mua vé của sự kiện có phí.',
 'Kiểm thử đăng ký miễn phí từ trạng thái hoàn toàn mới.',
 'default_workshop.jpg', 'DEFAULT', 'Phòng Lab TEST miễn phí',
 DATE_ADD(NOW(), INTERVAL 6 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 6 DAY), INTERVAL 3 HOUR),
 DATE_ADD(NOW(), INTERVAL 4 DAY),
 35, 0, 0, 'VND', 0.0, 0, 'PUBLISHED', 2, 1,
 NOW(), NULL);

-- ---------------------------------------------------------------------
-- 2. Đăng ký mẫu.
-- current_registered của event được tính bằng REGISTERED + PENDING_PAYMENT.
-- ---------------------------------------------------------------------
INSERT INTO registrations (
    registration_id, user_id, event_id, status, registered_at, cancelled_at
) VALUES
-- Event 100: 2 confirmed + 1 pending = current_registered 3; 1 cancelled.
(1000, 2, 100, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
(1001, 3, 100, 'PENDING_PAYMENT', NOW(),                              NULL),
(1002, 4, 100, 'CANCELLED',       DATE_SUB(NOW(), INTERVAL 1 DAY),    DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(1003, 5, 100, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 3 DAY),    NULL),

-- Event 101: 4 confirmed + 1 pending = đủ 5 chỗ.
(1004, 6, 101, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 4 DAY), NULL),
(1005, 7, 101, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 4 DAY), NULL),
(1006, 8, 101, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(1007, 9, 101, 'REGISTERED',      DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
(1008, 10, 101, 'PENDING_PAYMENT', NOW(),                          NULL),

-- Event 102: đăng ký miễn phí, không có payment.
(1009, 11, 102, 'REGISTERED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(1010, 12, 102, 'REGISTERED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),

-- Event 103: đã thanh toán trước khi hết hạn đăng ký.
(1011, 13, 103, 'REGISTERED', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),

-- Event 106: lịch sử sự kiện đã hoàn thành.
(1012, 14, 106, 'REGISTERED', DATE_SUB(NOW(), INTERVAL 20 DAY), NULL),
(1013, 15, 106, 'REGISTERED', DATE_SUB(NOW(), INTERVAL 19 DAY), NULL),

-- Event 107: hủy đăng ký vì event bị hủy, chờ/đã hoàn tiền.
(1014, 2, 107, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1015, 3, 107, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Event 108: ba kiểu giao dịch không thành công, đều đã nhả chỗ.
(1016, 4, 108, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1017, 5, 108, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1018, 6, 108, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------------------------------------------------------------
-- 3. Thanh toán mẫu.
-- ---------------------------------------------------------------------
INSERT INTO payments (
    payment_id,
    payment_code,
    registration_id,
    amount,
    currency,
    provider,
    status,
    provider_transaction_id,
    provider_response_code,
    bank_code,
    checkout_url,
    failure_reason,
    expires_at,
    paid_at,
    refunded_at,
    created_at,
    updated_at
) VALUES
-- Registration 1000: lần đầu lỗi, lần thứ hai thành công (test retry).
(1, 'EH_TEST_1000_A1', 1000, 120000, 'VND', 'VNPAY', 'FAILED',
 'VNP_TEST_FAIL_1000', '99', NULL, NULL, 'Lỗi giả lập từ cổng thanh toán',
 DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 49 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR)),

(2, 'EH_TEST_1000_A2', 1000, 120000, 'VND', 'VNPAY', 'PAID',
 'VNP_TEST_PAID_1000', '00', 'NCB', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR)),

-- Event 100: một pending, một failed, một paid bằng Mock.
(3, 'EH_TEST_1001_A1', 1001, 120000, 'VND', 'MOCK', 'PENDING',
 NULL, NULL, NULL, 'mock://checkout/EH_TEST_1001_A1', NULL,
 DATE_ADD(NOW(), INTERVAL 15 MINUTE), NULL, NULL, NOW(), NULL),

(4, 'EH_TEST_1002_A1', 1002, 120000, 'VND', 'VNPAY', 'FAILED',
 'VNP_TEST_FAIL_1002', '24', 'NCB', NULL, 'Khách hàng hủy thao tác tại cổng',
 DATE_SUB(NOW(), INTERVAL 23 HOUR), NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 23 HOUR)),

(5, 'EH_TEST_1003_A1', 1003, 120000, 'VND', 'MOCK', 'PAID',
 'MOCK_PAID_1003', '00', 'MOCK_BANK', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 71 HOUR), DATE_SUB(NOW(), INTERVAL 71 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 71 HOUR)),

-- Event 101: bốn payment thành công và một payment đang giữ vé cuối.
(6, 'EH_TEST_1004_A1', 1004, 200000, 'VND', 'VNPAY', 'PAID',
 'VNP_TEST_PAID_1004', '00', 'NCB', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 95 HOUR), DATE_SUB(NOW(), INTERVAL 95 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 95 HOUR)),

(7, 'EH_TEST_1005_A1', 1005, 200000, 'VND', 'VNPAY', 'PAID',
 'VNP_TEST_PAID_1005', '00', 'VCB', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 95 HOUR), DATE_SUB(NOW(), INTERVAL 95 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 95 HOUR)),

(8, 'EH_TEST_1006_A1', 1006, 200000, 'VND', 'MOCK', 'PAID',
 'MOCK_PAID_1006', '00', 'MOCK_BANK', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 71 HOUR), DATE_SUB(NOW(), INTERVAL 71 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 71 HOUR)),

(9, 'EH_TEST_1007_A1', 1007, 200000, 'VND', 'VNPAY', 'PAID',
 'VNP_TEST_PAID_1007', '00', 'TCB', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 47 HOUR)),

(10, 'EH_TEST_1008_A1', 1008, 200000, 'VND', 'MOCK', 'PENDING',
 NULL, NULL, NULL, 'mock://checkout/EH_TEST_1008_A1', NULL,
 DATE_ADD(NOW(), INTERVAL 10 MINUTE), NULL, NULL, NOW(), NULL),

-- Event 103: đã trả tiền trước khi deadline hết hạn.
(11, 'EH_TEST_1011_A1', 1011, 80000, 'VND', 'VNPAY', 'PAID',
 'VNP_TEST_PAID_1011', '00', 'BIDV', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 71 HOUR), DATE_SUB(NOW(), INTERVAL 71 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 71 HOUR)),

-- Event 106: payment lịch sử.
(12, 'EH_TEST_1012_A1', 1012, 180000, 'VND', 'VNPAY', 'PAID',
 'VNP_TEST_PAID_1012', '00', 'VCB', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL,
 DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),

(13, 'EH_TEST_1013_A1', 1013, 180000, 'VND', 'MOCK', 'PAID',
 'MOCK_PAID_1013', '00', 'MOCK_BANK', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY), NULL,
 DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),

-- Event 107: quy trình hoàn tiền.
(14, 'EH_TEST_1014_A1', 1014, 250000, 'VND', 'VNPAY', 'REFUND_PENDING',
 'VNP_TEST_PAID_1014', '00', 'NCB', NULL, 'Sự kiện bị hủy, đang chờ admin hoàn tiền',
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), NULL,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

(15, 'EH_TEST_1015_A1', 1015, 250000, 'VND', 'VNPAY', 'REFUNDED',
 'VNP_TEST_PAID_1015', '00', 'VCB', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY),
 DATE_SUB(NOW(), INTERVAL 12 HOUR),
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 12 HOUR)),

-- Event 108: đủ ba trạng thái lỗi để test thanh toán lại.
(16, 'EH_TEST_1016_A1', 1016, 99000, 'VND', 'MOCK', 'EXPIRED',
 NULL, NULL, NULL, 'mock://checkout/EH_TEST_1016_A1', 'Hết 15 phút giữ chỗ',
 DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL,
 DATE_SUB(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 15 MINUTE),
 DATE_SUB(NOW(), INTERVAL 3 DAY)),

(17, 'EH_TEST_1017_A1', 1017, 99000, 'VND', 'VNPAY', 'CANCELLED',
 'VNP_TEST_CANCEL_1017', '24', 'NCB', NULL, 'Người dùng chủ động hủy giao dịch',
 DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL,
 DATE_SUB(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 10 MINUTE),
 DATE_SUB(NOW(), INTERVAL 2 DAY)),

(18, 'EH_TEST_1018_A1', 1018, 99000, 'VND', 'VNPAY', 'FAILED',
 'VNP_TEST_FAIL_1018', '99', NULL, NULL, 'Cổng thanh toán trả lỗi giả lập',
 DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL,
 DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 10 MINUTE),
 DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------------------------------------------------------------
-- 4. Báo cáo kiểm tra nhanh sau khi seed.
-- ---------------------------------------------------------------------
SELECT
    event_id,
    title,
    status,
    ticket_price,
    currency,
    current_registered,
    max_participants,
    registration_deadline,
    start_time
FROM events
WHERE title LIKE 'TEST PAYMENT - %'
ORDER BY event_id;

SELECT
    status,
    COUNT(*) AS total_payments,
    SUM(amount) AS total_amount
FROM payments
WHERE payment_code LIKE 'EH_TEST_%'
GROUP BY status
ORDER BY status;

SELECT
    p.payment_id,
    p.payment_code,
    u.full_name,
    e.title,
    p.amount,
    p.provider,
    p.status,
    p.created_at,
    p.paid_at,
    p.refunded_at
FROM payments p
JOIN registrations r ON r.registration_id = p.registration_id
JOIN users u ON u.user_id = r.user_id
JOIN events e ON e.event_id = r.event_id
WHERE p.payment_code LIKE 'EH_TEST_%'
ORDER BY p.payment_id;

-- Kết quả truy vấn này phải rỗng. Nếu có dòng nghĩa là bộ đếm chỗ sai.
SELECT
    e.event_id,
    e.title,
    e.current_registered AS stored_count,
    SUM(CASE
        WHEN r.status IN ('REGISTERED', 'PENDING_PAYMENT') THEN 1
        ELSE 0
    END) AS calculated_count
FROM events e
LEFT JOIN registrations r ON r.event_id = e.event_id
WHERE e.title LIKE 'TEST PAYMENT - %'
GROUP BY e.event_id, e.title, e.current_registered
HAVING stored_count <> calculated_count;
