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
     */
    public List<Event> getRecommendations(Integer userId) throws SQLException {
        if (userId != null) {
            List<Event> recommended = eventDAO.findRecommendedForUser(userId, 5);
            if (!recommended.isEmpty()) return recommended;
        }
        return eventDAO.findRecommendedFallback(5);
    }

    /**
     * Lấy 3 sự kiện tương tự (cùng danh mục).
     */
    public List<Event> getSimilarEvents(int categoryId, int excludeEventId) throws SQLException {
        return eventDAO.findSimilar(categoryId, excludeEventId);
    }

    // =====================================================
    // TẠO SỰ KIỆN MỚI
    // =====================================================
    public int createEvent(Event event, Part imagePart, int adminId)
            throws EventException, SQLException {

        validateEventInput(event, true);
        event.setCreatedBy(adminId);

        int eventId = eventDAO.insert(event);
        event.setEventId(eventId);

        Category category = categoryDAO.findById(event.getCategoryId());
        if (category != null) {
            event.setCategoryName(category.getCategoryName());
        }

        // Luồng ảnh cho TẠO MỚI
        imageService.processImageForCreate(event, imagePart);

        return eventId;
    }

    // =====================================================
    // CẬP NHẬT SỰ KIỆN
    // =====================================================
    public void updateEvent(Event event, Part imagePart)
            throws EventException, SQLException {

        Event existing = eventDAO.findById(event.getEventId());
        if (existing == null) {
            throw new EventException("Sự kiện không tồn tại.");
        }

        if ("COMPLETED".equals(existing.getStatus())) {
            throw new EventException("Không thể chỉnh sửa sự kiện đã hoàn thành.");
        }
        if ("CANCELLED".equals(existing.getStatus())) {
            throw new EventException("Không thể chỉnh sửa sự kiện đã hủy.");
        }

        validateEventInput(event, false);

        if (event.getMaxParticipants() < existing.getCurrentRegistered()) {
            throw new EventException("Số người tối đa không thể nhỏ hơn số người đã đăng ký ("
                    + existing.getCurrentRegistered() + " người).");
        }

        // Giữ lại thông tin ảnh cũ để ImageService xóa đĩa nếu có upload mới
        event.setImagePath(existing.getImagePath());
        event.setImageSource(existing.getImageSource());

        // Update thông tin text trong DB
        eventDAO.update(event);

        Category category = categoryDAO.findById(event.getCategoryId());
        if (category != null) {
            event.setCategoryName(category.getCategoryName());
        }

        // Luồng ảnh cho CẬP NHẬT (Chỉ đổi nếu chọn file mới)
        imageService.processImageForUpdate(event, imagePart);
    }

    // =====================================================
    // XÓA / HỦY SỰ KIỆN
    // =====================================================
    public String deleteEvent(int eventId) throws EventException, SQLException {
        Event event = eventDAO.findById(eventId);
        if (event == null) {
            throw new EventException("Sự kiện không tồn tại.");
        }

        int activeRegistrations = eventDAO.countRegistered(eventId);

        if (activeRegistrations == 0) {
            eventDAO.delete(eventId);
            return "DELETED";
        } else {
            cancelEventWithRegistrations(eventId);
            return "CANCELLED";
        }
    }

    private void cancelEventWithRegistrations(int eventId) throws SQLException {
        java.sql.Connection conn = null;
        try {
            conn = com.eventhub.config.DBConnection.getConnection();
            conn.setAutoCommit(false);

            String cancelRegs =
                    "UPDATE registrations SET status='CANCELLED', cancelled_at=NOW() " +
                            "WHERE event_id=? AND status='REGISTERED'";
            java.sql.PreparedStatement s1 = conn.prepareStatement(cancelRegs);
            s1.setInt(1, eventId);
            s1.executeUpdate();

            String cancelEvent =
                    "UPDATE events SET status='CANCELLED' WHERE event_id=?";
            java.sql.PreparedStatement s2 = conn.prepareStatement(cancelEvent);
            s2.setInt(1, eventId);
            s2.executeUpdate();

            conn.commit();
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
    private void validateEventInput(Event event, boolean isCreating)
            throws EventException {

        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new EventException("Vui lòng nhập tên sự kiện.");
        }
        if (event.getTitle().trim().length() < 5 || event.getTitle().trim().length() > 200) {
            throw new EventException("Tên sự kiện phải từ 5 đến 200 ký tự.");
        }

        if (event.getDescription() == null || event.getDescription().trim().isEmpty()) {
            throw new EventException("Vui lòng nhập mô tả sự kiện.");
        }
        if (event.getDescription().trim().length() < 20) {
            throw new EventException("Mô tả sự kiện phải có ít nhất 20 ký tự.");
        }

        if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
            throw new EventException("Vui lòng nhập địa điểm.");
        }

        if (event.getStartTime() == null) {
            throw new EventException("Vui lòng chọn thời gian bắt đầu.");
        }
        if (event.getEndTime() == null) {
            throw new EventException("Vui lòng chọn thời gian kết thúc.");
        }
        if (event.getRegistrationDeadline() == null) {
            throw new EventException("Vui lòng chọn hạn đăng ký.");
        }

        if (!event.getEndTime().isAfter(event.getStartTime())) {
            throw new EventException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        if (event.getRegistrationDeadline().isAfter(event.getStartTime())) {
            throw new EventException("Hạn đăng ký phải trước hoặc bằng thời gian bắt đầu.");
        }

        if (isCreating && !event.getStartTime().isAfter(LocalDateTime.now().plusHours(1))) {
            throw new EventException("Thời gian bắt đầu phải sau hiện tại ít nhất 1 giờ.");
        }

        if (event.getMaxParticipants() <= 0) {
            throw new EventException("Số người tham gia tối đa phải lớn hơn 0.");
        }
        if (event.getMaxParticipants() > 10000) {
            throw new EventException("Số người tham gia tối đa không được quá 10.000.");
        }

        if (event.getCategoryId() <= 0) {
            throw new EventException("Vui lòng chọn danh mục sự kiện.");
        }

        String status = event.getStatus();
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status)) {
            throw new EventException("Trạng thái không hợp lệ.");
        }
    }
}