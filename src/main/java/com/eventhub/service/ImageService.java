package com.eventhub.service;

import com.eventhub.dao.EventDAO;
import com.eventhub.exception.FileException;
import com.eventhub.model.Event;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;

/**
 * Service xử lý ảnh sự kiện:
 *   - Upload ảnh từ Admin
 *   - Tạo ảnh bằng Imagen AI
 *   - Fallback về ảnh mặc định theo danh mục
 */
public class ImageService {

    private final EventDAO eventDAO = new EventDAO();
    private final GeminiService geminiService = new GeminiService();

    // Thư mục gốc chứa uploads (đọc từ env)
    private static final String UPLOAD_BASE_DIR =
            System.getenv("UPLOAD_BASE_DIR");

    // Các định dạng file ảnh được chấp nhận (kiểm tra Content-Type, không phải extension)
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    // Kích thước tối đa: 5MB
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    // Map tên danh mục → tên file ảnh mặc định
    private static final Map<String, String> DEFAULT_IMAGE_MAP = Map.of(
            "Hội thảo",             "default_hoithao.jpg",
            "Workshop",             "default_workshop.jpg",
            "Buổi họp",             "default_hoihop.jpg",
            "Hoạt động ngoại khóa", "default_ngoaikhoa.jpg",
            "Cuộc thi",             "default_cuocthi.jpg",
            "Khác",                 "default_other.jpg"
    );

    // =====================================================
    // PHƯƠNG THỨC CHÍNH: XỬ LÝ ẢNH SAU KHI TẠO/SỬA SỰ KIỆN
    // =====================================================

    /**
     * Xử lý ảnh cho sự kiện theo thứ tự ưu tiên:
     *   1. Nếu admin upload ảnh → dùng ảnh upload
     *   2. Nếu không upload     → gọi Imagen AI tạo ảnh
     *   3. Nếu AI thất bại      → dùng ảnh mặc định theo danh mục
     *
     * Không throw exception → luồng lưu sự kiện không bị gián đoạn.
     *
     * @param event    Sự kiện vừa được lưu (có eventId)
     * @param imagePart File upload từ form (có thể null)
     */
    public void processImage(Event event, Part imagePart) {
        try {
            // Kiểm tra có file upload không
            boolean hasUpload = imagePart != null
                    && imagePart.getSize() > 0
                    && imagePart.getSubmittedFileName() != null
                    && !imagePart.getSubmittedFileName().isBlank();

            if (hasUpload) {
                // --- CASE 1: Admin đã upload ảnh ---
                handleUpload(event, imagePart);

            } else {
                // --- CASE 2: Không upload → thử AI ---
                handleAutoImage(event);
            }

        } catch (Exception e) {
            // Lỗi ảnh KHÔNG crash luồng chính
            System.err.println("[ImageService] Lỗi xử lý ảnh: " + e.getMessage());
            // Thử set ảnh default nếu chưa có
            try {
                if (event.getImagePath() == null) {
                    setDefaultImage(event);
                }
            } catch (Exception ignored) {}
        }
    }

    // =====================================================
    // XỬ LÝ UPLOAD ẢNH THỦ CÔNG
    // =====================================================

    /**
     * Validate và lưu file ảnh upload từ Admin.
     *
     * @throws FileException nếu file không hợp lệ
     */
    private void handleUpload(Event event, Part imagePart)
            throws FileException, IOException, SQLException {

        // --- Validate Content-Type (không tin extension) ---
        String contentType = imagePart.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new FileException(
                    "Chỉ chấp nhận file ảnh JPG, PNG, WEBP. " +
                            "File của bạn: " + contentType
            );
        }

        // --- Validate kích thước ---
        if (imagePart.getSize() > MAX_FILE_SIZE) {
            throw new FileException("Kích thước file không được vượt quá 5MB.");
        }

        // --- Xóa file ảnh cũ (nếu có và không phải ảnh default) ---
        deleteOldImage(event);

        // --- Tạo tên file mới bằng UUID (bảo mật, tránh path traversal) ---
        String extension = getExtension(contentType);
        String newFileName = "event_" + UUID.randomUUID() + "." + extension;

        // --- Lưu file ---
        Path uploadPath = Paths.get(UPLOAD_BASE_DIR, "events");
        Files.createDirectories(uploadPath);  // Tạo thư mục nếu chưa có

        Path filePath = uploadPath.resolve(newFileName);
        try (InputStream inputStream = imagePart.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // --- Cập nhật DB ---
        eventDAO.updateImage(event.getEventId(), newFileName, "UPLOADED");
        event.setImagePath(newFileName);
        event.setImageSource("UPLOADED");

        System.out.println("[ImageService] Đã upload ảnh: " + newFileName);
    }

    /**
     * Tự động tạo ảnh: thử AI trước, fallback về default.
     */
    private void handleAutoImage(Event event) {
        // Thử Imagen AI
        String aiFileName = geminiService.generateEventImage(event);

        if (aiFileName != null) {
            // AI thành công
            try {
                eventDAO.updateImage(event.getEventId(), aiFileName, "AI_GENERATED");
                event.setImagePath(aiFileName);
                event.setImageSource("AI_GENERATED");
                System.out.println("[ImageService] Dùng ảnh AI: " + aiFileName);
            } catch (Exception e) {
                System.err.println("[ImageService] Lỗi update DB sau AI: " + e.getMessage());
            }
        } else {
            // AI thất bại → dùng ảnh default
            setDefaultImage(event);
        }
    }

    /**
     * Set ảnh mặc định theo danh mục sự kiện.
     */
    private void setDefaultImage(Event event) {
        String categoryName = event.getCategoryName();
        // Tìm trong map, nếu không có thì dùng "Khác"
        String defaultFileName = DEFAULT_IMAGE_MAP.getOrDefault(
                categoryName, "default_other.jpg"
        );

        try {
            eventDAO.updateImage(event.getEventId(), defaultFileName, "DEFAULT");
            event.setImagePath(defaultFileName);
            event.setImageSource("DEFAULT");
            System.out.println("[ImageService] Dùng ảnh default: " + defaultFileName);
        } catch (Exception e) {
            System.err.println("[ImageService] Lỗi set default image: " + e.getMessage());
        }
    }

    /**
     * Xóa file ảnh cũ của sự kiện (khi upload ảnh mới).
     * KHÔNG xóa ảnh default (chúng dùng chung).
     */
    private void deleteOldImage(Event event) {
        String oldPath = event.getImagePath();
        String oldSource = event.getImageSource();

        // Chỉ xóa nếu là ảnh đã upload hoặc AI gen (không xóa default)
        if (oldPath != null
                && ("UPLOADED".equals(oldSource) || "AI_GENERATED".equals(oldSource))) {
            try {
                Path filePath = Paths.get(UPLOAD_BASE_DIR, "events", oldPath);
                Files.deleteIfExists(filePath);
                System.out.println("[ImageService] Đã xóa ảnh cũ: " + oldPath);
            } catch (Exception e) {
                // Không xóa được file cũ → log nhưng không crash
                System.err.println("[ImageService] Không xóa được file cũ: " + e.getMessage());
            }
        }
    }

    /**
     * Lấy extension từ Content-Type.
     */
    private String getExtension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "jpg";  // jpeg, jpg
        };
    }
}