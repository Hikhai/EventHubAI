package com.eventhub.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Servlet phụ trách đọc ảnh từ đĩa cứng (UPLOAD_BASE_DIR)
 * và trả trực tiếp về cho Browser khi gọi URL /uploads/*
 */
@WebServlet("/uploads/*")
public class UploadServlet extends HttpServlet {

    private static final String UPLOAD_BASE_DIR = System.getenv("UPLOAD_BASE_DIR");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Lấy đường dẫn sau /uploads (ví dụ: "/events/event_123.jpg" hoặc "/defaults/default_cuocthi.jpg")
        String requestedFile = req.getPathInfo();

        if (requestedFile == null || requestedFile.equals("/")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 1. Tìm file trong thư mục ngoài đĩa cứng UPLOAD_BASE_DIR
        File file = new File(UPLOAD_BASE_DIR, requestedFile);

        // 2. Nếu không thấy trên đĩa, tìm thử trong thư mục webapp của project
        if (!file.exists() || file.isDirectory()) {
            String realPath = getServletContext().getRealPath("/uploads" + requestedFile);
            if (realPath != null) {
                file = new File(realPath);
            }
        }

        // 3. Nếu vẫn không thấy file -> Báo 404
        if (!file.exists() || file.isDirectory()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Lấy định dạng Content-Type (image/jpeg, image/png, ...)
        String contentType = getServletContext().getMimeType(file.getName());
        if (contentType == null) {
            contentType = Files.probeContentType(file.toPath());
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        resp.setContentType(contentType);
        resp.setContentLengthLong(file.length());

        // Đọc byte từ đĩa và ghi ra response cho Browser
        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}