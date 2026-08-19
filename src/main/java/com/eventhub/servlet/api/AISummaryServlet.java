package com.eventhub.servlet.api;

import com.eventhub.model.User;
import com.eventhub.service.GeminiService;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * API tạo tóm tắt AI cho sự kiện.
 * POST /api/ai/summary
 * Body params: title, description
 * Response: JSON {success, summary}
 *
 * Chỉ Admin mới được gọi.
 */
@WebServlet("/api/ai/summary")
public class AISummaryServlet extends HttpServlet {

    private final GeminiService geminiService = new GeminiService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        // Response luôn là JSON
        resp.setContentType("application/json;charset=UTF-8");

        // Kiểm tra quyền Admin
        User user = (User) req.getSession().getAttribute("loggedInUser");
        if (user == null || !user.isAdmin()) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false,\"message\":\"Không có quyền.\"}");
            return;
        }

        String title       = req.getParameter("title");
        String description = req.getParameter("description");

        if (title == null || title.trim().isEmpty()
                || description == null || description.trim().isEmpty()) {
            resp.getWriter().write(
                    "{\"success\":false,\"message\":\"Thiếu title hoặc description.\"}");
            return;
        }

        // Gọi Gemini
        String summary = geminiService.generateSummary(title.trim(), description.trim());

        // Build JSON response bằng Gson (đảm bảo escape đúng)
        JsonObject json = new JsonObject();
        if (summary != null) {
            json.addProperty("success", true);
            json.addProperty("summary", summary);
        } else {
            json.addProperty("success", false);
            json.addProperty("message", "Không thể tạo tóm tắt lúc này.");
        }

        resp.getWriter().write(json.toString());
    }
}