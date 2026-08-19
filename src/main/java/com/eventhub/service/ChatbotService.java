package com.eventhub.service;

import com.eventhub.dao.EventDAO;
import com.eventhub.dao.RegistrationDAO;
import com.eventhub.dto.EventFilterDTO;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.model.User;
import jakarta.servlet.http.HttpSession;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service xử lý chatbot EventHub AI.
 *
 * Luồng:
 *   1. Lấy lịch sử chat từ session
 *   2. Lấy context (sự kiện + user) từ DB
 *   3. Build system prompt
 *   4. Gọi GeminiService.chat()
 *   5. Lưu lịch sử vào session
 *   6. Trả về reply
 */
public class ChatbotService {

    private final GeminiService geminiService = new GeminiService();
    private final EventDAO eventDAO = new EventDAO();
    private final RegistrationDAO registrationDAO = new RegistrationDAO();

    // Key lưu trong session
    private static final String SESSION_HISTORY_KEY = "chatHistory";
    private static final String SESSION_COUNT_KEY   = "chatCount";

    // Giới hạn
    private static final int MAX_TURNS  = 50;   // Tối đa 50 lượt hỏi/session
    private static final int MAX_HISTORY = 20;  // Giữ 20 entry lịch sử (10 cặp)
    private static final int MAX_MSG_LEN = 500; // Tối đa 500 ký tự/tin nhắn

    // Format thời gian hiển thị trong chat
    private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Xử lý 1 lượt chat.
     *
     * @param message Tin nhắn của user
     * @param session HTTP session (để lưu/lấy lịch sử)
     * @param user    User hiện tại (null nếu guest)
     * @return Câu trả lời của AI
     */
    public String processMessage(String message,
                                 HttpSession session,
                                 User user) {

        // --- Validate ---
        if (message == null || message.trim().isEmpty()) {
            return "Bạn chưa nhập tin nhắn. Hãy hỏi tôi điều gì đó!";
        }
        if (message.length() > MAX_MSG_LEN) {
            return "Tin nhắn quá dài (tối đa " + MAX_MSG_LEN + " ký tự). " +
                    "Vui lòng rút gọn câu hỏi.";
        }

        // --- Kiểm tra giới hạn lượt/session ---
        int chatCount = getSessionCount(session);
        if (chatCount >= MAX_TURNS) {
            return "Bạn đã dùng hết " + MAX_TURNS + " lượt chat trong phiên này. " +
                    "Vui lòng tải lại trang để bắt đầu phiên mới!";
        }

        try {
            // --- Lấy lịch sử từ session ---
            List<Map<String, String>> history = getHistory(session);

            // --- Lấy context từ DB ---
            String eventContext  = buildEventContext();
            String userContext   = buildUserContext(user);

            // --- Build system prompt ---
            String systemPrompt = buildSystemPrompt(user, eventContext, userContext);

            // --- Thêm tin nhắn user vào history ---
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role",    "user");
            userMsg.put("content", message.trim());
            history.add(userMsg);

            // --- Gọi Gemini ---
            String reply = geminiService.chat(systemPrompt, history);

            // --- Thêm reply vào history ---
            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role",    "model");  // Gemini dùng "model"
            assistantMsg.put("content", reply);
            history.add(assistantMsg);

            // --- Trim history nếu quá dài ---
            // Xóa 2 entry cũ nhất (1 cặp user+model)
            while (history.size() > MAX_HISTORY) {
                history.remove(0);
                if (!history.isEmpty()) history.remove(0);
            }

            // --- Lưu lại vào session ---
            session.setAttribute(SESSION_HISTORY_KEY, history);
            session.setAttribute(SESSION_COUNT_KEY, chatCount + 1);

            return reply;

        } catch (SQLException e) {
            System.err.println("[ChatbotService] Lỗi DB: " + e.getMessage());
            // Vẫn trả lời nhưng không có context sự kiện
            return geminiService.chat(
                    buildSystemPrompt(user, "Hiện không lấy được dữ liệu sự kiện.", ""),
                    getHistory(session)
            );
        }
    }

    /**
     * Xóa lịch sử chat trong session.
     */
    public void clearHistory(HttpSession session) {
        session.removeAttribute(SESSION_HISTORY_KEY);
        session.removeAttribute(SESSION_COUNT_KEY);
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    /**
     * Lấy lịch sử chat từ session (tạo mới nếu chưa có).
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getHistory(HttpSession session) {
        Object history = session.getAttribute(SESSION_HISTORY_KEY);
        if (history instanceof List) {
            return (List<Map<String, String>>) history;
        }
        return new ArrayList<>();  // Phiên mới → history rỗng
    }

    /**
     * Lấy số lượt chat đã dùng trong session.
     */
    private int getSessionCount(HttpSession session) {
        Object count = session.getAttribute(SESSION_COUNT_KEY);
        return (count instanceof Integer) ? (Integer) count : 0;
    }

    /**
     * Lấy thông tin 10 sự kiện PUBLISHED gần nhất từ DB.
     * Inject vào system prompt để Gemini trả lời chính xác.
     */
    private String buildEventContext() throws SQLException {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setPageSize(10);
        filter.setPage(1);

        List<Event> events = eventDAO.findAllForUser(filter);

        if (events.isEmpty()) {
            return "Hiện tại không có sự kiện nào đang mở đăng ký.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Danh sách sự kiện đang mở:\n");

        for (Event e : events) {
            sb.append("• ").append(e.getTitle());

            if (e.getCategoryName() != null) {
                sb.append(" | Danh mục: ").append(e.getCategoryName());
            }
            if (e.getStartTime() != null) {
                sb.append(" | Thời gian: ").append(e.getStartTime().format(DT_FORMAT));
            }
            if (e.getLocation() != null) {
                sb.append(" | Địa điểm: ").append(e.getLocation());
            }
            sb.append(" | Còn ").append(e.getAvailableSlots()).append(" chỗ");
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Lấy thông tin sự kiện user đã đăng ký (nếu đã login).
     */
    private String buildUserContext(User user) throws SQLException {
        if (user == null) return "";

        List<Registration> regs = registrationDAO.findAllByUser(user.getUserId());

        // Lọc chỉ lấy đăng ký REGISTERED và sự kiện chưa kết thúc
        StringBuilder sb = new StringBuilder();
        sb.append("Sự kiện bạn đã đăng ký:\n");

        boolean hasAny = false;
        for (Registration r : regs) {
            if ("REGISTERED".equals(r.getStatus()) && !r.isEventEnded()) {
                sb.append("• ").append(r.getEventTitle());
                if (r.getEventStartTime() != null) {
                    sb.append(" (").append(r.getFormattedEventStartTime()).append(")");
                }
                sb.append("\n");
                hasAny = true;
            }
        }

        if (!hasAny) {
            return "Bạn chưa đăng ký sự kiện nào sắp tới.";
        }
        return sb.toString();
    }

    /**
     * Build system prompt đầy đủ với context.
     */
    private String buildSystemPrompt(User user,
                                     String eventContext,
                                     String userContext) {
        String userInfo;
        if (user != null) {
            userInfo = "Đã đăng nhập: " + user.getFullName()
                    + " (" + user.getEmail() + ")";
        } else {
            userInfo = "Khách chưa đăng nhập";
        }

        return "Bạn là trợ lý AI của EventHub — hệ thống quản lý sự kiện " +
                "dành cho sinh viên và tổ chức.\n\n" +

                "Nhiệm vụ của bạn:\n" +
                "1. Giúp người dùng tìm kiếm và hiểu thông tin sự kiện\n" +
                "2. Hướng dẫn cách đăng ký, hủy đăng ký sự kiện\n" +
                "3. Trả lời câu hỏi về hệ thống EventHub\n\n" +

                "Thông tin người dùng hiện tại:\n" +
                "Trạng thái: " + userInfo + "\n\n" +

                eventContext + "\n\n" +
                userContext + "\n\n" +

                "Quy tắc:\n" +
                "- Luôn trả lời bằng tiếng Việt\n" +
                "- Ngắn gọn, thân thiện, rõ ràng\n" +
                "- Không bịa thông tin sự kiện ngoài danh sách trên\n" +
                "- Nếu không biết → hướng dẫn liên hệ admin@eventhub.com\n" +
                "- Nếu hỏi về sự kiện không có trong danh sách → " +
                "gợi ý tìm kiếm trên trang Events";
    }
}