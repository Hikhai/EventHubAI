package com.eventhub.servlet;

import com.eventhub.config.UploadConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serve ảnh từ thư mục uploads trong project khi gọi URL /uploads/*
 */
@WebServlet("/uploads/*")
public class UploadServlet extends HttpServlet {

    private static final long CACHE_SECONDS = 86_400;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String requestedFile = req.getPathInfo();
        if (requestedFile == null || requestedFile.equals("/")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path file;
        try {
            file = UploadConfig.resolveSafe(requestedFile);
        } catch (IllegalStateException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (file == null || !Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = getServletContext().getMimeType(file.getFileName().toString());
        if (contentType == null) {
            contentType = Files.probeContentType(file);
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        resp.setContentType(contentType);
        resp.setContentLengthLong(Files.size(file));
        resp.setHeader("Cache-Control", "public, max-age=" + CACHE_SECONDS);

        Files.copy(file, resp.getOutputStream());
    }
}
