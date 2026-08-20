package com.eventhub.config;

import jakarta.servlet.ServletContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Thư mục ảnh nằm trong project: {@code src/main/webapp/uploads}.
 * {@code UPLOAD_BASE_DIR} chỉ dùng khi muốn ghi đè (tùy chọn).
 */
public final class UploadConfig {

    private static volatile Path baseDir;

    private UploadConfig() {
    }

    public static void init(ServletContext ctx) {
        Path resolved = resolveBaseDir(ctx);
        try {
            Files.createDirectories(resolved.resolve("events"));
            Files.createDirectories(resolved.resolve("defaults"));
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục uploads: " + resolved, e);
        }
        baseDir = resolved;
        System.out.println("[UploadConfig] Upload dir: " + baseDir);
    }

    private static Path resolveBaseDir(ServletContext ctx) {
        String env = System.getenv("UPLOAD_BASE_DIR");
        if (env != null && !env.isBlank()) {
            return Paths.get(env).toAbsolutePath().normalize();
        }

        Path projectUploads = findProjectUploads(ctx);
        if (projectUploads != null) {
            return projectUploads;
        }

        String real = ctx.getRealPath("/uploads");
        if (real != null) {
            return Paths.get(real).toAbsolutePath().normalize();
        }

        throw new IllegalStateException(
                "Không xác định được thư mục uploads. Kiểm tra src/main/webapp/uploads.");
    }

    /**
     * Tìm thư mục project (có pom.xml) rồi trỏ vào src/main/webapp/uploads
     * để ảnh nằm trong source, không phụ thuộc C:\eventhub-uploads.
     */
    private static Path findProjectUploads(ServletContext ctx) {
        if (ctx != null) {
            String realRoot = ctx.getRealPath("/");
            if (realRoot != null) {
                Path found = walkToProjectUploads(Paths.get(realRoot));
                if (found != null) {
                    return found;
                }
            }
        }
        return walkToProjectUploads(Paths.get(System.getProperty("user.dir")));
    }

    private static Path walkToProjectUploads(Path start) {
        Path dir = start.toAbsolutePath().normalize();
        for (int i = 0; i < 8 && dir != null; i++) {
            Path pom = dir.resolve("pom.xml");
            Path webapp = dir.resolve("src").resolve("main").resolve("webapp");
            if (Files.isRegularFile(pom) && Files.isDirectory(webapp)) {
                return webapp.resolve("uploads").toAbsolutePath().normalize();
            }
            dir = dir.getParent();
        }
        return null;
    }

    public static Path getBaseDir() {
        Path dir = baseDir;
        if (dir == null) {
            throw new IllegalStateException("UploadConfig chưa được khởi tạo.");
        }
        return dir;
    }

    /**
     * Resolve path trong uploads, chặn path traversal.
     */
    public static Path resolveSafe(String requestedFile) {
        if (requestedFile == null || requestedFile.isBlank() || requestedFile.contains("..")) {
            return null;
        }
        Path base = getBaseDir();
        String relative = requestedFile.startsWith("/") ? requestedFile.substring(1) : requestedFile;
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base)) {
            return null;
        }
        return resolved;
    }
}
