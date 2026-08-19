package com.eventhub.service;

import com.eventhub.dao.CategoryDAO;
import com.eventhub.dao.EventDAO;
import com.eventhub.dto.EventFilterDTO;
import com.eventhub.dto.PaginationDTO;
import com.eventhub.exception.EventException;
import com.eventhub.model.Category;
import com.eventhub.model.Event;
import jakarta.servlet.http.Part;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý nghiệp vụ liên quan đến sự kiện.
 * CRUD + validate + xử lý ảnh.
 */
public class EventService {

    private final EventDAO eventDAO = new EventDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ImageService imageService = new ImageService();

    // =====================================================
    // LẤY DANH SÁCH SỰ KIỆN
    // =====================================================

    /**
     * Lấy danh sách sự kiện cho User (chỉ PUBLISHED).
     * Trả về cả PaginationDTO để JSP render phân trang.
     */
    public List<Event> getEventsForUser(EventFilterDTO filter) throws SQLException {
        return eventDAO.findAllForUser(filter);
    }

    public PaginationDTO getPaginationForUser(EventFilterDTO filter) throws SQLException {
        int total = eventDAO.countForUser(filter);
        return new PaginationDTO(filter.getPage(), filter.getPageSize(), total);
    }

    /**
     * Lấy danh sách sự kiện cho Admin (tất cả trạng thái).
     */
    public List<Event> getEventsForAdmin(EventFilterDTO filter) throws SQLException {
        return eventDAO.findAllForAdmin(filter);
    }

    public PaginationDTO getPaginationForAdmin(EventFilterDTO filter) throws SQLException {
        int total = eventDAO.countForAdmin(filter);
        return new PaginationDTO(filter.getPage(), filter.getPageSize(), total);
    }

    /**
     * Lấy chi tiết 1 sự kiện.
     * @throws EventException nếu không tìm thấy
     */
    public Event getEventById(int eventId) throws EventException, SQLException {
        Event event = eventDAO.findById(eventId);
        if (event == null) {
            throw new EventException("Sự kiện không tồn tại hoặc đã bị xóa.");
        }
        return event;
    }

    /**
     * Gợi ý sự kiện cho User dựa trên lịch sử đăng ký.
     * Nếu chưa có lịch sử → lấy sự kiện mới nhất.
     */
    public List<Event> getRecommendations(Integer userId) throws SQLException {
        if (userId != null) {
            List<Event> recommended = eventDAO.findRecommendedForUser(userId, 5);
            if (!recommended.isEmpty()) return recommended;
        }
        // Fallback: lấy sự kiện mới nhất cho guest hoặc user chưa có lịch sử
        return eventDAO.findRecommendedFallback(5);
    }

    /**
     * Lấy 3 sự kiện tương tự (cùng danh mục, dùng trong trang chi tiết).
     */
    public List<Event> getSimilarEvents(int categoryId,
                                        int excludeEventId) throws SQLException {
        return eventDAO.findSimilar(categoryId, excludeEventId);
    }

    // =====================================================
    // TẠO SỰ KIỆN MỚI
    // =====================================================

    /**
     * Tạo sự kiện mới:
     *   1. Validate input
     *   2. Insert vào DB
     *   3. Xử lý ảnh (upload / AI gen / default)
     *   4. Tùy chọn: AI tóm tắt (nếu summaryAi null)
     *
     * @return eventId vừa tạo
     */
    public int createEvent(Event event, Part imagePart, int adminId)
            throws EventException, SQLException {

        // --- Validate ---
        validateEventInput(event, true);  // true = đang tạo mới

        // --- Set người tạo ---
        event.setCreatedBy(adminId);

        // --- Insert vào DB ---
        int eventId = eventDAO.insert(event);
        event.setEventId(eventId);

        // --- Lấy categoryName để xử lý ảnh ---
        // (cần categoryName cho prompt ảnh + fallback image name)
        Category category = categoryDAO.findById(event.getCategoryId());
        if (category != null) {
            event.setCategoryName(category.getCategoryName());
        }

        // --- Xử lý ảnh (không throw exception) ---
        imageService.processImage(event, imagePart);

        return eventId;
    }

    // =====================================================
    // CẬP NHẬT SỰ KIỆN
    // =====================================================

    /**
     * Cập nhật thông tin sự kiện.
     */
    public void updateEvent(Event event, Part imagePart)
            throws EventException, SQLException {

        // --- Kiểm tra sự kiện tồn tại ---
        Event existing = eventDAO.findById(event.getEventId());
        if (existing == null) {
            throw new EventException("Sự kiện không tồn tại.");
        }

        // --- Không sửa sự kiện đã COMPLETED hoặc CANCELLED ---
        if ("COMPLETED".equals(existing.getStatus())) {
            throw new EventException(
                    "Không thể chỉnh sửa sự kiện đã hoàn thành (COMPLETED)."
            );
        }
        if ("CANCELLED".equals(existing.getStatus())) {
            throw new EventException(
                    "Không thể chỉnh sửa sự kiện đã hủy (CANCELLED)."
            );
        }

        // --- Validate input ---
        validateEventInput(event, false);

        // --- Không giảm max_participants dưới current_registered ---
        if (event.getMaxParticipants() < existing.getCurrentRegistered()) {
            throw new EventException(
                    "Số người tối đa không thể nhỏ hơn số người đã đăng ký ("
                            + existing.getCurrentRegistered() + " người)."
            );
        }

        // --- Update DB (các trường text, không đụng đến ảnh) ---
        eventDAO.update(event);

        // --- Xử lý ảnh nếu có upload mới ---
        // Giữ nguyên thông tin ảnh cũ vào event object để deleteOldImage biết file nào xóa
        event.setImagePath(existing.getImagePath());
        event.setImageSource(existing.getImageSource());

        // Lấy categoryName cho xử lý ảnh
        Category category = categoryDAO.findById(event.getCategoryId());
        if (category != null) {
            event.setCategoryName(category.getCategoryName());
        }

        // Kiểm tra có file upload mới không
        // In log để debug
        boolean hasNewUpload = false;
        if (imagePart != null) {
            long size = imagePart.getSize();
            String fileName = imagePart.getSubmittedFileName();

            System.out.println("[EventService] Update event " + event.getEventId()
                    + " - imagePart size: " + size
                    + ", fileName: '" + fileName + "'");

            hasNewUpload = size > 0
                    && fileName != null
                    && !fileName.trim().isEmpty();
        } else {
            System.out.println("[EventService] Update event - imagePart is null");
        }

        if (hasNewUpload) {
            System.out.println("[EventService] Có upload mới → xử lý ảnh");
            // Gọi handleUpload trực tiếp (không gọi processImage vì nó có nhánh AI)
            try {
                imageService.handleUploadDirect(event, imagePart);
            } catch (Exception e) {
                System.err.println("[EventService] Lỗi xử lý ảnh khi update: "
                        + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[EventService] Không có upload mới → giữ ảnh cũ");
        }
    }

    // =====================================================
    // XÓA / HỦY SỰ KIỆN
    // =====================================================

    /**
     * Xóa hoặc hủy sự kiện:
     *   - Nếu không có đăng ký ACTIVE → xóa cứng (hard delete)
     *   - Nếu có đăng ký ACTIVE → set CANCELLED (soft delete)
     *
     * @return "DELETED" hoặc "CANCELLED" (để Servlet hiển thị message phù hợp)
     */
    public String deleteEvent(int eventId) throws EventException, SQLException {
        // --- Kiểm tra tồn tại ---
        Event event = eventDAO.findById(eventId);
        if (event == null) {
            throw new EventException("Sự kiện không tồn tại.");
        }

        // --- Đếm đăng ký đang active ---
        int activeRegistrations = eventDAO.countRegistered(eventId);

        if (activeRegistrations == 0) {
            // Hard delete: xóa khỏi DB hoàn toàn
            eventDAO.delete(eventId);
            return "DELETED";

        } else {
            // Soft delete: chuyển sang CANCELLED
            // eventDAO.delete() đã xử lý cascade CANCELLED cho registrations
            // Nhưng ở đây ta chỉ muốn CANCEL event, không xóa
            // → Gọi riêng để update status
            cancelEventWithRegistrations(eventId);
            return "CANCELLED";
        }
    }

    /**
     * Hủy sự kiện và tất cả đăng ký liên quan trong 1 transaction.
     */
    private void cancelEventWithRegistrations(int eventId) throws SQLException {
        java.sql.Connection conn = null;
        try {
            conn = com.eventhub.config.DBConnection.getConnection();
            conn.setAutoCommit(false);  // Bắt đầu transaction

            // Hủy tất cả đăng ký REGISTERED của event này
            String cancelRegs =
                    "UPDATE registrations SET status='CANCELLED', cancelled_at=NOW() " +
                            "WHERE event_id=? AND status='REGISTERED'";
            java.sql.PreparedStatement s1 = conn.prepareStatement(cancelRegs);
            s1.setInt(1, eventId);
            s1.executeUpdate();

            // Chuyển event sang CANCELLED
            String cancelEvent =
                    "UPDATE events SET status='CANCELLED' WHERE event_id=?";
            java.sql.PreparedStatement s2 = conn.prepareStatement(cancelEvent);
            s2.setInt(1, eventId);
            s2.executeUpdate();

            conn.commit();  // Commit cả 2 thao tác

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
    // VALIDATE INPUT
    // =====================================================

    /**
     * Validate tất cả trường của sự kiện.
     *
     * @param isCreating true = đang tạo mới (có thêm rule về thời gian)
     */
    private void validateEventInput(Event event, boolean isCreating)
            throws EventException {

        // Tiêu đề
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new EventException("Vui lòng nhập tên sự kiện.");
        }
        if (event.getTitle().trim().length() < 5
                || event.getTitle().trim().length() > 200) {
            throw new EventException("Tên sự kiện phải từ 5 đến 200 ký tự.");
        }

        // Mô tả
        if (event.getDescription() == null
                || event.getDescription().trim().isEmpty()) {
            throw new EventException("Vui lòng nhập mô tả sự kiện.");
        }
        if (event.getDescription().trim().length() < 20) {
            throw new EventException("Mô tả sự kiện phải có ít nhất 20 ký tự.");
        }

        // Địa điểm
        if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
            throw new EventException("Vui lòng nhập địa điểm.");
        }

        // Thời gian
        if (event.getStartTime() == null) {
            throw new EventException("Vui lòng chọn thời gian bắt đầu.");
        }
        if (event.getEndTime() == null) {
            throw new EventException("Vui lòng chọn thời gian kết thúc.");
        }
        if (event.getRegistrationDeadline() == null) {
            throw new EventException("Vui lòng chọn hạn đăng ký.");
        }

        // end_time phải sau start_time
        if (!event.getEndTime().isAfter(event.getStartTime())) {
            throw new EventException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu."
            );
        }

        // deadline phải trước hoặc bằng start_time
        if (event.getRegistrationDeadline().isAfter(event.getStartTime())) {
            throw new EventException(
                    "Hạn đăng ký phải trước hoặc bằng thời gian bắt đầu."
            );
        }

        // Khi tạo mới: start_time phải sau hiện tại ít nhất 1 giờ
        if (isCreating
                && !event.getStartTime().isAfter(LocalDateTime.now().plusHours(1))) {
            throw new EventException(
                    "Thời gian bắt đầu phải sau hiện tại ít nhất 1 giờ."
            );
        }

        // Số người tối đa
        if (event.getMaxParticipants() <= 0) {
            throw new EventException("Số người tham gia tối đa phải lớn hơn 0.");
        }
        if (event.getMaxParticipants() > 10000) {
            throw new EventException("Số người tham gia tối đa không được quá 10.000.");
        }

        // Danh mục
        if (event.getCategoryId() <= 0) {
            throw new EventException("Vui lòng chọn danh mục sự kiện.");
        }

        // Status chỉ cho phép DRAFT hoặc PUBLISHED qua form
        String status = event.getStatus();
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status)) {
            throw new EventException("Trạng thái không hợp lệ.");
        }
    }
}