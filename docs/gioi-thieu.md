# EventHub AI — Giới thiệu ứng dụng

## 1. EventHub AI là gì?

**EventHub AI** là web quản lý sự kiện trong trường đại học. Sinh viên xem lịch, đăng ký, hủy đăng ký và đánh giá sau khi sự kiện kết thúc. Ban tổ chức (Admin) tạo / sửa sự kiện, theo dõi người tham gia và xem thống kê.

Điểm khác so với form đăng ký thông thường: hệ thống **gọi Google Gemini** để tóm tắt mô tả, tạo ảnh poster, và trả lời câu hỏi qua chatbot (ví dụ: “Có workshop Git nào tuần này không?”).

Ứng dụng chạy trên **Apache Tomcat**, viết bằng **Java Servlet + JSP**, dữ liệu lưu **MySQL**.

---

## 2. Người dùng

| Vai trò | Việc chính |
|---|---|
| **Khách** | Xem danh sách / chi tiết sự kiện, hỏi chatbot |
| **Sinh viên (USER)** | Đăng ký tài khoản, đăng ký / hủy sự kiện, xem “Sự kiện của tôi”, đánh giá 1–5 sao |
| **Quản trị (ADMIN)** | Dashboard, CRUD sự kiện & danh mục, xem danh sách đăng ký, dùng AI tóm tắt / tạo ảnh |

Tài khoản demo (sau khi import database mẫu):

- Admin: `admin@eventhub.com` / `Admin@123`
- Sinh viên: `an@example.com` / `User@123`

---

## 3. Chức năng

### 3.1. Phía sinh viên

- Tìm sự kiện theo từ khóa, danh mục, phân trang
- Xem chi tiết: thời gian, hạn đăng ký, số chỗ, tóm tắt AI, đánh giá
- Đăng ký / hủy đăng ký (có kiểm tra hạn, sức chứa, sự kiện đã bắt đầu)
- Trang **Sự kiện của tôi**: tab Sắp diễn ra / Đã tham gia / Đã hủy
- Đánh giá sau khi sự kiện kết thúc (mỗi người 1 lần)
- Chatbot hỏi đáp về sự kiện đang mở

### 3.2. Phía quản trị

- Dashboard: số sự kiện, lượt đăng ký, điểm trung bình, sự kiện sắp tới
- Tạo / sửa sự kiện (Draft hoặc Published)
- Ảnh sự kiện: upload tay, **AI tạo poster**, hoặc ảnh mặc định theo danh mục
- Không sửa sự kiện đã kết thúc / đã hủy; sự kiện đang diễn ra không đổi giờ bắt đầu
- Xóa sự kiện trống, hoặc **hủy** nếu đã có người đăng ký
- Quản lý danh mục (Hội thảo, Workshop, Cuộc thi, …)
- Xem danh sách người đã đăng ký từng sự kiện

### 3.3. Trí tuệ nhân tạo (Gemini)

| Tính năng | Khi nào dùng | Nếu AI lỗi |
|---|---|---|
| Tóm tắt 2 câu | Admin bấm nút trên form sự kiện | Form vẫn lưu, chỉ thiếu tóm tắt |
| Ảnh poster 16:9 | Tạo mới không upload; hoặc sửa + tick “tạo ảnh AI” | Tạo mới → ảnh mặc định theo danh mục; Sửa → giữ ảnh cũ |
| Chatbot | Widget góc phải mọi trang user | Trả lời thông báo tạm không dùng được |

Không có API key thì web **vẫn chạy**, chỉ tắt phần AI.

---

## 4. Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Web | Jakarta Servlet 6, JSP, JSTL, Bootstrap 5 |
| Máy chủ | Apache Tomcat 10.1 |
| CSDL | MySQL 8, connection pool **HikariCP** |
| Bảo mật mật khẩu | BCrypt |
| AI | Google Gemini HTTP API (`java.net.http.HttpClient` + Gson) |
| Build | Maven (`pom.xml`), artifact WAR `eventhub` |

Kiến trúc 4 lớp, không dùng Spring:

```
JSP (giao diện)
  → Servlet (nhận request)
    → Service (nghiệp vụ, validate)
      → DAO (SQL)
        → MySQL
```

---

## 5. Dữ liệu chính

| Bảng | Ý nghĩa |
|---|---|
| `users` | Tài khoản, role `ADMIN` / `USER` |
| `categories` | Danh mục sự kiện |
| `events` | Thông tin sự kiện, ảnh, trạng thái, số chỗ, điểm TB |
| `registrations` | Đăng ký (`REGISTERED` / `CANCELLED`), mỗi user–event một dòng |
| `reviews` | Điểm 1–5 + nhận xét, mỗi user–event một lần |
| `chat_logs` | Bảng sẵn cho lịch sử chat (chatbot hiện lưu chủ yếu trong session) |

Trạng thái sự kiện: `DRAFT` → `PUBLISHED` → `COMPLETED` hoặc `CANCELLED`.

Ảnh: `UPLOADED` (file admin chọn), `AI_GENERATED` (Gemini), `DEFAULT` (ảnh sẵn theo danh mục trong `src/main/webapp/uploads/defaults/`).

---

## 6. Cấu trúc thư mục (rút gọn)

```
EventHubAI/
├── pom.xml
├── sql/eventhub_db.sql          # dữ liệu mẫu
├── docs/                        # tài liệu này
└── src/main/
    ├── java/com/eventhub/
    │   ├── config/              # DB, upload, listener
    │   ├── filter/              # UTF-8, đăng nhập, quyền admin
    │   ├── servlet/             # auth, user, admin, api
    │   ├── service/             # nghiệp vụ
    │   ├── dao/                 # SQL
    │   ├── model/               # đối tượng
    │   ├── dto/  util/  exception/
    └── webapp/
        ├── WEB-INF/views/       # JSP
        ├── assets/              # CSS, JS
        └── uploads/             # ảnh mặc định + ảnh sự kiện
```

---

## 7. Đọc tiếp

- Thuyết trình luồng và chỉ đúng file code: [luong-hoat-dong.md](luong-hoat-dong.md)
- Cài MySQL, Tomcat, biến môi trường: [huong-dan-cai-dat.md](huong-dan-cai-dat.md)
