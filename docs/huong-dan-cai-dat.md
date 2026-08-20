# EventHub AI — Chạy ứng dụng lần đầu

Làm lần lượt 6 bước. Xong thì mở `http://localhost:8080/eventhub/`.

---

## Cần cài sẵn

| Phần mềm | Ghi chú |
|---|---|
| **JDK 21** | Project compile Java 21 (`pom.xml`). Trong IntelliJ chọn SDK 21, không dùng JDK 26 nếu chưa khớp. |
| **Apache Maven** | IntelliJ đã có Maven bundled thì không cần cài thêm. |
| **MySQL 8** | Tạo database `eventhub_db`. |
| **Apache Tomcat 10.1** | Bắt buộc bản 10.x (Jakarta EE). Ví dụ Tomcat 10.1.59. Không dùng Tomcat 9. |
| **IntelliJ IDEA** | Ultimate hoặc Community + plugin hỗ trợ Tomcat. |
| **Google Gemini API key** | Không bắt buộc. Thiếu key thì web vẫn chạy, chatbot / ảnh AI / tóm tắt sẽ tắt. |

---

## Bước 1 — Tạo database và import dữ liệu mẫu

Mở MySQL (Workbench, CLI, hoặc tab Database trong IntelliJ):

```sql
CREATE DATABASE eventhub_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Import file:

```
sql/eventhub_db.sql
```

Cách nhanh bằng dòng lệnh (sửa user/mật khẩu cho đúng máy):

```bash
mysql -u root -p eventhub_db < sql/eventhub_db.sql
```

File dump **tắt kiểm tra khóa ngoại** lúc import, nên chạy được dù thứ tự bảng trong file không theo FK.

Kiểm tra: phải có bảng `users`, `categories`, `events`, `registrations`, `reviews`.

---

## Bước 2 — Mở project trong IntelliJ

1. **File → Open** → chọn thư mục `EventHubAI` (thư mục có `pom.xml`).
2. Đợi Maven tải dependency (góc dưới IntelliJ).
3. **File → Project Structure → Project**
   - SDK: **21**
   - Language level: **21**
4. **File → Settings → Build, Execution, Deployment → Compiler → Java Compiler**  
   Target bytecode version: **21**.

Nạp lại Maven: chuột phải `pom.xml` → **Maven → Reload project**.

---

## Bước 3 — Artifact WAR exploded

IntelliJ cần artifact để Tomcat deploy.

1. **File → Project Structure → Artifacts**
2. `+` → **Web Application: Exploded** → **From Modules...** → chọn module `EventHubAI`
3. Đặt tên gợi ý: `EventHubAI:war exploded`
4. Output directory mặc định dạng: `out/artifacts/EventHubAI_war_exploded`
5. OK

`pom.xml` đặt `finalName` = `eventhub` → context path nên là **`/eventhub`**.

---

## Bước 4 — Run Configuration Tomcat + biến môi trường

1. **Run → Edit Configurations...** → `+` → **Tomcat Server → Local**
2. Tab **Server**
   - Application server: trỏ tới thư mục Tomcat 10.1 (ví dụ `C:\...\apache-tomcat-10.1.59`)
   - HTTP port: `8080`
   - JRE: JDK 21
3. Tab **Deployment**
   - `+` → Artifact `EventHubAI:war exploded`
   - **Application context:** `/eventhub`
4. Tab **Server** (hoặc **Startup/Connection** tùy bản IntelliJ) → **Environment variables**  
   Thêm đúng 3 biến bắt buộc (và 1 biến AI nếu có):

| Tên biến | Ví dụ | Bắt buộc |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/eventhub_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8` | Có |
| `DB_USERNAME` | `root` | Có |
| `DB_PASSWORD` | mật khẩu MySQL của máy bạn | Có |
| `GEMINI_API_KEY` | key từ [Google AI Studio](https://aistudio.google.com/apikey) | Không |
| `UPLOAD_BASE_DIR` | đường dẫn tuyệt đối nếu muốn đổi chỗ lưu ảnh | Không |

`DB_PASSWORD` vẫn phải khai báo dù mật khẩu rỗng (hiếm). App **không** đọc user/pass từ file `.properties` — chỉ đọc biến môi trường (`DBConnection.java`).

5. Apply → OK.

### Lấy Gemini API key (tùy chọn)

1. Vào Google AI Studio, tạo API key.
2. Dán vào `GEMINI_API_KEY` của Tomcat.
3. Restart Tomcat sau khi đổi biến (biến được đọc lúc JVM start).

Không commit key lên Git.

---

## Bước 5 — Chạy

1. Chọn configuration Tomcat vừa tạo.
2. Bấm **Run** (hoặc Debug).
3. Đợi log dạng `Server startup in ... ms`.
4. Mở trình duyệt:

```
http://localhost:8080/eventhub/
```

Sẽ chuyển tới danh sách sự kiện (`/events`) hoặc dashboard nếu đã login admin.

### Tài khoản mẫu

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Admin | `admin@eventhub.com` | `Admin@123` |
| Sinh viên | `an@example.com` | `User@123` |

Các user khác trong dump (`binh@example.com`, …) dùng chung mật khẩu `User@123`.

---

## Bước 6 — Kiểm tra nhanh

1. Trang `/events` hiện card sự kiện + ảnh.  
2. Login user → Đăng ký một sự kiện sắp tới → vào **Sự kiện của tôi**.  
3. Login admin → **Tạo sự kiện** (giờ bắt đầu phải sau hiện tại ít nhất 1 giờ).  
4. Nếu có `GEMINI_API_KEY`: bấm tóm tắt AI, chatbot góc phải, tạo sự kiện không upload ảnh.

Ảnh mặc định nằm tại `src/main/webapp/uploads/defaults/`. Ảnh admin/AI upload ghi vào `uploads/events/` (thư mục này không đưa file thật lên Git, chỉ giữ `.gitkeep`).

---

## Lỗi thường gặp

| Hiện tượng | Cách xử | File liên quan |
|---|---|---|
| `Database config chưa được cấu hình` | Chưa set `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` trên **Tomcat run config**, rồi restart | `config/DBConnection.java` |
| `Communications link failure` / timeout | MySQL chưa chạy, sai port 3306, sai tên database | MySQL service |
| `Access denied for user` | Sai user/mật khẩu MySQL | biến `DB_*` |
| Trang trắng / 404 `/eventhub` | Sai Application context hoặc chưa deploy artifact exploded | Tomcat Deployment |
| `jakarta.servlet` / cannot find symbol | Đang dùng Tomcat 9 hoặc JDK quá cũ | Tomcat 10.1 + JDK 21 |
| Tiếng Việt form bị lỗi font | Encoding; filter UTF-8 đã có, kiểm tra MySQL `utf8mb4` | `CharacterEncodingFilter` |
| Chatbot / ảnh AI im lặng | Thiếu `GEMINI_API_KEY`, hoặc quota 429. App vẫn chạy phần còn lại | `GeminiService.java` |
| Ảnh sự kiện 404 | Chưa có thư mục uploads; restart để `AppContextListener` tạo | `UploadConfig.java` |
| Đăng nhập đúng pass vẫn sai | Chưa import dump, hoặc gõ nhầm hash tay vào DB | dùng đúng tài khoản mẫu |

Xem log Tomcat (cửa sổ Run của IntelliJ): `[AppContextListener]`, `[UploadConfig] Upload dir:`, Hikari `EventHubPool`.

---

## Ghi chú Git

Không đưa lên Git:

- `.idea/workspace.xml` (có thể chứa env / đường dẫn máy)
- `GEMINI_API_KEY`
- Ảnh trong `src/main/webapp/uploads/events/` (đã ignore)

Nên đưa:

- `docs/`
- `sql/eventhub_db.sql`
- source Java / JSP / CSS
- `uploads/defaults/` (ảnh mặc định)

Chi tiết chức năng: [gioi-thieu.md](gioi-thieu.md) · Luồng + file code: [luong-hoat-dong.md](luong-hoat-dong.md).
