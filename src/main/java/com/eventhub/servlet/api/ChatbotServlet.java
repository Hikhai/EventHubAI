package com.eventhub.servlet.api;

import com.eventhub.model.ChatMessage;
import com.eventhub.model.User;
import com.eventhub.service.ChatbotService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * API chatbot EventHub AI.
 *
 * GET    /api/chatbot?conversationId=...  -> đọc lịch sử
 * POST   /api/chatbot                    -> gửi tin nhắn
 * DELETE /api/chatbot?conversationId=... -> xóa lịch sử cuộc trò chuyện
 *
 * Body POST hỗ trợ:
 * - form: message=...&conversationId=...
 * - JSON: {"message":"...", "conversationId":"..."}
 *
 * POST chỉ xếp job và trả về nhanh; Gemini được xử lý ở worker nền nên
 * việc chuyển trang không hủy quá trình tạo câu trả lời.
 * Người dùng phải đăng nhập. Lịch sử được phân quyền theo user lấy từ
 * session server, không tin userId do client gửi lên.
 */
@WebServlet("/api/chatbot")
public class ChatbotServlet extends HttpServlet {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final ChatbotService chatbotService = new ChatbotService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        HttpSession session = req.getSession(false);
        User user = getLoggedInUser(session);
        if (user == null) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    "Vui lòng đăng nhập để xem lịch sử trò chuyện.");
            return;
        }

        String conversationId = chatbotService.resolveConversationId(
                req.getParameter("conversationId"), session);
        String requestedJobId = req.getParameter("jobId");

        // Polling status chỉ cần trạng thái/reply; không cần tải lại cả lịch sử.
        if (!isBlank(requestedJobId)) {
            ChatbotService.ChatJobStatus status = chatbotService.getJobStatus(
                    session, user, conversationId, requestedJobId);
            writeJobStatus(resp, status);
            return;
        }

        List<ChatMessage> history = chatbotService.getHistory(
                session, user, conversationId);
        ChatbotService.ChatJobStatus status = chatbotService.getJobStatus(
                session, user, conversationId, null);

        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("conversationId", conversationId);
        addJobFields(json, status);

        JsonArray messages = new JsonArray();
        for (ChatMessage message : history) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.getRole());
            item.addProperty("content", message.getContent());
            if (message.getCreatedAt() != null) {
                item.addProperty("timestamp",
                        message.getCreatedAt().format(TIME_FORMAT));
            }
            messages.add(item);
        }
        json.add("messages", messages);
        resp.getWriter().write(json.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        HttpSession session = req.getSession(false);
        User user = getLoggedInUser(session);
        if (user == null) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    "Vui lòng đăng nhập để sử dụng trợ lý AI.");
            return;
        }

        // Lấy message/conversationId từ form trước; nếu không có thì đọc JSON.
        String message = req.getParameter("message");
        String conversationId = req.getParameter("conversationId");
        if (isBlank(message) || isBlank(conversationId)) {
            try {
                String body = req.getReader().lines()
                        .reduce("", (a, b) -> a + b);
                if (!body.isBlank()) {
                    JsonObject bodyJson = JsonParser.parseString(body).getAsJsonObject();
                    if (isBlank(message) && bodyJson.has("message")
                            && !bodyJson.get("message").isJsonNull()) {
                        message = bodyJson.get("message").getAsString();
                    }
                    if (isBlank(conversationId) && bodyJson.has("conversationId")
                            && !bodyJson.get("conversationId").isJsonNull()) {
                        conversationId = bodyJson.get("conversationId").getAsString();
                    }
                }
            } catch (Exception ignored) {
                // Validation bên dưới sẽ trả lỗi thân thiện cho request không hợp lệ.
            }
        }

        if (isBlank(message)) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Tin nhắn không được để trống.");
            return;
        }

        String resolvedConversationId = chatbotService.resolveConversationId(
                conversationId, session);
        ChatbotService.AsyncChatResult result = chatbotService.startAsyncMessage(
                message, session, user, resolvedConversationId);

        if (!result.accepted()) {
            writeError(resp, HttpServletResponse.SC_CONFLICT,
                    result.message() == null
                            ? "Câu hỏi chưa được tiếp nhận. Bạn vui lòng thử lại."
                            : result.message());
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("pending", true);
        json.addProperty("status", "PENDING");
        json.addProperty("jobId", result.jobId());
        json.addProperty("conversationId", resolvedConversationId);
        json.addProperty("timestamp", LocalTime.now().format(TIME_FORMAT));
        resp.getWriter().write(json.toString());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        HttpSession session = req.getSession(false);
        User user = getLoggedInUser(session);
        if (user == null) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    "Vui lòng đăng nhập để xóa lịch sử trò chuyện.");
            return;
        }

        String conversationId = chatbotService.resolveConversationId(
                req.getParameter("conversationId"), session);
        chatbotService.clearHistory(session, user, conversationId);

        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        resp.getWriter().write(json.toString());
    }

    private void writeJobStatus(HttpServletResponse resp,
                                ChatbotService.ChatJobStatus status)
            throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        addJobFields(json, status);
        resp.getWriter().write(json.toString());
    }

    private void addJobFields(JsonObject json,
                              ChatbotService.ChatJobStatus status) {
        if (status == null) {
            json.addProperty("status", "IDLE");
            json.addProperty("pending", false);
            return;
        }

        json.addProperty("status", status.status());
        json.addProperty("pending", status.pending());
        json.addProperty("jobId", status.jobId());
        if (status.reply() != null) {
            json.addProperty("reply", status.reply());
        }
        if (status.error() != null) {
            json.addProperty("message", status.error());
        }
        if (status.completedAt() != null) {
            json.addProperty("timestamp",
                    status.completedAt().format(TIME_FORMAT));
        }
    }

    private User getLoggedInUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("loggedInUser");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void writeError(HttpServletResponse resp, int status, String message)
            throws IOException {
        resp.setStatus(status);
        JsonObject err = new JsonObject();
        err.addProperty("success", false);
        err.addProperty("message", message);
        resp.getWriter().write(err.toString());
    }
}
