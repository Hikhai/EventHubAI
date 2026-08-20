package com.eventhub.service;

import com.eventhub.config.UploadConfig;
import com.eventhub.dao.EventDAO;
import com.eventhub.exception.FileException;
import com.eventhub.model.Event;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Service xử lý ảnh sự kiện.
 * Tách biệt rõ ràng luồng Tạo mới và luồng Cập nhật.
 */
public class ImageService {

    private final EventDAO eventDAO = new EventDAO();
    private final GeminiService geminiService = new GeminiService();

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private static final Map<String, String> DEFAULT_IMAGE_MAP = Map.of(
            "Hội thảo", "default_hoithao.jpg",
            "Workshop", "default_workshop.jpg",
            "Buổi họp", "default_hoihop.jpg",
            "Hoạt động ngoại khóa", "default_ngoaikhoa.jpg",
            "Cuộc thi", "default_cuocthi.jpg",
            "Khác", "default_other.jpg"
    );

    // =====================================================
    // LUỒNG 1: DÙNG CHO TẠO SỰ KIỆN MỚI
    // =====================================================

    /**
     * Khi TẠO MỚI sự kiện:
     * - Nếu có upload file -> Dùng file upload
     * - Nếu KHÔNG upload   -> Thử Gemini AI -> Thất bại -> Dùng Default
     */
    public void processImageForCreate(Event event, Part imagePart) {
        if (hasUploadedFile(imagePart)) {
            try {
                saveUploadedImage(event, imagePart);
            } catch (Exception e) {
                System.err.println("[ImageService] Lỗi upload ảnh tạo mới: " + e.getMessage());
                setDefaultImage(event);
            }
        } else {
            tryAIOrSetDefault(event);
        }
    }

    // =====================================================
    // LUỒNG 2: DÙNG CHO CẬP NHẬT SỰ KIỆN
    // =====================================================

    /**
     * Khi CẬP NHẬT sự kiện:
     * 1. Có file upload → dùng file mới
     * 2. Tick "tạo ảnh AI" → gen AI thay ảnh cũ (thất bại thì giữ ảnh cũ)
     * 3. Không chọn gì → giữ ảnh cũ
     */
    public String processImageForUpdate(Event event, Part imagePart, boolean regenerateAi) {
        if (hasUploadedFile(imagePart)) {
            try {
                saveUploadedImage(event, imagePart);
                return "UPLOADED";
            } catch (Exception e) {
                System.err.println("[ImageService] Lỗi upload ảnh cập nhật: " + e.getMessage());
                return "UPLOAD_FAILED";
            }
        }

        if (regenerateAi) {
            return tryReplaceWithAiImage(event) ? "AI_OK" : "AI_FAILED";
        }

        return "UNCHANGED";
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    /**
     * Lưu file upload thủ công vào đĩa và update DB.
     */
    private void saveUploadedImage(Event event, Part imagePart)
            throws FileException, IOException, Exception {

        String contentType = imagePart.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new FileException("Chỉ chấp nhận file ảnh JPG, PNG, WEBP.");
        }

        if (imagePart.getSize() > MAX_FILE_SIZE) {
            throw new FileException("Kích thước file không được vượt quá 5MB.");
        }

        // Xóa file ảnh cũ trên đĩa nếu có (chỉ xóa file UPLOADED hoặc AI_GENERATED)
        deleteOldImageFile(event);

        // Tạo tên file mới UUID
        String extension = getExtension(contentType);
        String newFileName = "event_" + UUID.randomUUID().toString() + "." + extension;

        // Lưu file vào thư mục uploads/events
        Path uploadPath = UploadConfig.getBaseDir().resolve("events");
        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(newFileName);
        try (InputStream inputStream = imagePart.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Cập nhật Database
        eventDAO.updateImage(event.getEventId(), newFileName, "UPLOADED");
        event.setImagePath(newFileName);
        event.setImageSource("UPLOADED");

        System.out.println("[ImageService] ✅ Upload ảnh thành công: " + newFileName);
    }

    /**
     * Thử gọi AI gen ảnh, nếu hỏng thì lấy ảnh Default theo danh mục.
     */
    private void tryAIOrSetDefault(Event event) {
        System.out.println("[ImageService] Không có file upload -> Thử tạo ảnh bằng Gemini AI...");
        String aiFileName = geminiService.generateEventImage(event);

        if (aiFileName != null) {
            try {
                eventDAO.updateImage(event.getEventId(), aiFileName, "AI_GENERATED");
                event.setImagePath(aiFileName);
                event.setImageSource("AI_GENERATED");
                System.out.println("[ImageService] Tao anh AI thanh cong: " + aiFileName);
                return;
            } catch (Exception e) {
                System.err.println("[ImageService] Lỗi lưu DB ảnh AI: " + e.getMessage());
            }
        }

        setDefaultImage(event);
    }

    /**
     * Tạo ảnh AI và thay ảnh cũ. Không đổi sang default nếu AI thất bại.
     */
    private boolean tryReplaceWithAiImage(Event event) {
        System.out.println("[ImageService] Admin yeu cau tao lai anh AI...");
        String aiFileName = geminiService.generateEventImage(event);
        if (aiFileName == null) {
            return false;
        }
        try {
            deleteOldImageFile(event);
            eventDAO.updateImage(event.getEventId(), aiFileName, "AI_GENERATED");
            event.setImagePath(aiFileName);
            event.setImageSource("AI_GENERATED");
            System.out.println("[ImageService] Da thay anh bang AI: " + aiFileName);
            return true;
        } catch (Exception e) {
            System.err.println("[ImageService] Loi luu anh AI khi sua: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gán ảnh mặc định theo danh mục.
     */
    private void setDefaultImage(Event event) {
        String categoryName = event.getCategoryName();
        String defaultFileName = DEFAULT_IMAGE_MAP.getOrDefault(categoryName, "default_other.jpg");

        try {
            eventDAO.updateImage(event.getEventId(), defaultFileName, "DEFAULT");
            event.setImagePath(defaultFileName);
            event.setImageSource("DEFAULT");
            System.out.println("[ImageService] ℹ️ Sử dụng ảnh mặc định: " + defaultFileName);
        } catch (Exception e) {
            System.err.println("[ImageService] Lỗi set default image: " + e.getMessage());
        }
    }

    /**
     * Xóa file ảnh cũ khỏi đĩa cứng (không xóa ảnh mặc định).
     */
    private void deleteOldImageFile(Event event) {
        String oldPath = event.getImagePath();
        String oldSource = event.getImageSource();

        if (oldPath != null && ("UPLOADED".equals(oldSource) || "AI_GENERATED".equals(oldSource))) {
            try {
                Path filePath = UploadConfig.getBaseDir().resolve("events").resolve(oldPath);
                Files.deleteIfExists(filePath);
                System.out.println("[ImageService] 🗑️ Đã xóa file cũ: " + oldPath);
            } catch (Exception e) {
                System.err.println("[ImageService] Không thể xóa file cũ: " + e.getMessage());
            }
        }
    }

    private boolean hasUploadedFile(Part imagePart) {
        return imagePart != null
                && imagePart.getSize() > 0
                && imagePart.getSubmittedFileName() != null
                && !imagePart.getSubmittedFileName().trim().isEmpty();
    }

    private String getExtension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}