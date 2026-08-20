package com.eventhub.servlet.api;

import com.eventhub.model.User;
import com.eventhub.service.ChatbotService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * API chatbot.
 * POST /api/chatbot
 * Body: JSON {"message": "..."}  hoặc form param "message"
 * Response: JSON {success, reply, timestamp}
 *
 * Không yêu cầu đăng nhập (Guest cũng dùng được).
 */
@WebServlet("/api/chatbot")
public class ChatbotServlet extends HttpServlet {

    private final ChatbotService chatbotService = new ChatbotService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        // Lấy message (hỗ trợ cả form param và JSON body)
        String message = req.getParameter("message");

        // Nếu không có form param → thử đọc JSON body
        if (message == null || message.trim().isEmpty()) {
            try {
                String body = req.getReader().lines().reduce("", (a, b) -> a + b);
                if (!body.isEmpty()) {
                    JsonObject bodyJson = JsonParser.parseString(body).getAsJsonObject();
                    if (bodyJson.has("message")) {
                        message = bodyJson.get("message").getAsString();
                    }
                }
            } catch (Exception ignored) {}
        }

        // Validate
        if (message == null || message.trim().isEmpty()) {
            resp.getWriter().write(
                    "{\"success\":false,\"message\":\"Tin nhắn không được rỗng.\"}");
            return;
        }

        // Lấy user từ session (null nếu guest)
        User user = (User) req.getSession().getAttribute("loggedInUser");

        // Xử lý chatbot
        String reply = chatbotService.processMessage(
                message, req.getSession(), user);

        // Build response
        JsonObject json = new JsonObject();
        json.addProperty("success",   true);
        json.addProperty("reply",     reply);
        json.addProperty("timestamp",
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        resp.getWriter().write(json.toString());
    }
}