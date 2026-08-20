package com.eventhub.service;

import com.eventhub.dao.EventDAO;
import com.eventhub.dao.RegistrationDAO;
import com.eventhub.dto.EventFilterDTO;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.model.User;
import jakarta.servlet.http.HttpSession;

import java.sql.SQLException;
import java.util.*;

/**
 * Service xử lý chatbot EventHub AI.
 * <p>
 * Luồng:
 * 1. Lấy lịch sử chat từ session
 * 2. Lấy context (sự kiện + user) từ DB
 * 3. Build system prompt
 * 4. Gọi GeminiService.chat()
 * 5. Lưu lịch sử vào session
 * 6. Trả về reply
 */
public class ChatbotService {

    private final GeminiService geminiService = new GeminiService();
    private final EventDAO eventDAO = new EventDAO();
    private final RegistrationDAO registrationDAO = new RegistrationDAO();

    // Key lưu trong session
    private static final String SESSION_HISTORY_KEY = "chatHistory";
    private static final String SESSION_COUNT_KEY = "chatCount";

    // Giới hạn
    private static final int MAX_TURNS = 50;
    private static final int MAX_HISTORY = 24;
    private static final int MAX_MSG_LEN = 500;
    private static final long EVENT_CONTEXT_TTL_MS = 60_000;

    private static volatile String cachedEventContext;
    private static volatile long cachedEventContextAt;

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
            String eventContext = buildEventContext();
            String userContext = buildUserContext(user);

            // --- Build system prompt ---
            String systemPrompt = buildSystemPrompt(user, eventContext, userContext);

            // --- Thêm tin nhắn user vào history ---
            Map<String, String> userMsg = Map.of(
                    "role", "user",
                    "content", message.trim()
            );
            history.add(userMsg);

            String reply = geminiService.chat(systemPrompt, history);

            Map<String, String> assistantMsg = Map.of(
                    "role", "model",
                    "content", reply
            );
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
            List<Map<String, String>> fallbackHistory = getHistory(session);
            fallbackHistory.add(Map.of("role", "user", "content", message.trim()));
            return geminiService.chat(
                    buildSystemPrompt(user, "Hiện không lấy được dữ liệu sự kiện.", ""),
                    fallbackHistory
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
        long now = System.currentTimeMillis();
        String cached = cachedEventContext;
        if (cached != null && now - cachedEventContextAt < EVENT_CONTEXT_TTL_MS) {
            return cached;
        }

        EventFilterDTO filter = new EventFilterDTO();
        filter.setPageSize(20);
        filter.setPage(1);

        List<Event> events = eventDAO.findAllForUser(filter);

        if (events.isEmpty()) {
            cachedEventContext = "Hiện tại không có sự kiện PUBLISHED nào còn diễn ra.";
            cachedEventContextAt = now;
            return cachedEventContext;
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("Dữ liệu sự kiện đang mở (chỉ dùng các mục này, không bịa thêm):\n");

        for (Event e : events) {
            sb.append("- ID ").append(e.getEventId())
                    .append(" | ").append(e.getTitle());
            if (e.getCategoryName() != null) {
                sb.append(" | Danh mục: ").append(e.getCategoryName());
            }
            sb.append(" | Bắt đầu: ").append(nullSafe(e.getFormattedStartTime()));
            sb.append(" | Kết thúc: ").append(nullSafe(e.getFormattedEndTime()));
            sb.append(" | Hạn đăng ký: ").append(nullSafe(e.getFormattedDeadline()));
            if (e.getLocation() != null) {
                sb.append(" | Địa điểm: ").append(e.getLocation());
            }
            sb.append(" | Chỗ: ").append(e.getCurrentRegistered())
                    .append('/').append(e.getMaxParticipants())
                    .append(" (còn ").append(e.getAvailableSlots()).append(')');
            sb.append(" | Đăng ký: ").append(e.isRegistrationOpen() ? "CÒN MỞ" : "ĐÃ ĐÓNG");
            String desc = e.getDescription();
            if (desc != null && !desc.isBlank()) {
                if (desc.length() > 220) {
                    desc = desc.substring(0, 220) + "...";
                }
                sb.append(" | Mô tả: ").append(desc.replace('\n', ' '));
            }
            sb.append('\n');
        }

        cachedEventContext = sb.toString();
        cachedEventContextAt = now;
        return cachedEventContext;
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

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "chưa có" : value;
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

        return "Bạn là EventHub Assistant — tư vấn sự kiện cho sinh viên, dựa trên dữ liệu thật.\n\n" +

                "Cách dùng hệ thống:\n" +
                "- Xem danh sách: trang Sự kiện (/events). Lọc theo danh mục hoặc từ khóa.\n" +
                "- Chi tiết & đăng ký: vào từng sự kiện, bấm Đăng ký (cần đăng nhập, còn hạn và còn chỗ).\n" +
                "- Sự kiện của tôi: /my-events. Hủy đăng ký khi sự kiện chưa bắt đầu.\n" +
                "- Đánh giá: sau khi sự kiện kết thúc, tại /my-events.\n" +
                "- Admin: dashboard, tạo/sửa sự kiện, xem đăng ký.\n" +
                "- Liên hệ hỗ trợ: admin@eventhub.com\n\n" +

                "Người dùng hiện tại: " + userInfo + "\n\n" +

                eventContext + "\n" +
                (userContext == null || userContext.isBlank() ? "" : userContext + "\n") +

                "Định dạng trả lời (markdown, UI sẽ render):\n" +
                "- Dùng **in đậm** cho số liệu và tên sự kiện quan trọng.\n" +
                "- Xuống dòng giữa các ý. Không viết một khối dài.\n" +
                "- Khi liệt kê TỪ 2 sự kiện trở lên: BẮT BUỘC dùng bảng markdown, không dùng danh sách đánh số.\n" +
                "  Cột đúng thứ tự: Tên | Thời gian | Địa điểm | Còn chỗ | Hạn ĐK\n" +
                "  Ví dụ:\n" +
                "  | Tên | Thời gian | Địa điểm | Còn chỗ | Hạn ĐK |\n" +
                "  |---|---|---|---|---|\n" +
                "  | Workshop Git | 25/08 10:41 | Lab 1 | 25 | 24/08 18:41 |\n" +
                "- Sau bảng: 1 câu gợi ý (đăng ký / lọc danh mục).\n" +
                "- Câu hỏi hướng dẫn (cách đăng ký, hủy): mỗi bước một dòng, dùng danh sách đánh số:\n" +
                "  1. ...\n" +
                "  2. ...\n" +
                "  Không viết dính một đoạn. Có khoảng trắng sau **in đậm**.\n" +
                "- Chỉ dùng sự kiện trong danh sách trên. Không bịa lịch, địa điểm, số chỗ.\n" +
                "- Nếu câu hỏi mơ hồ, hỏi lại 1 câu cho rõ.\n" +
                "- Không tiết lộ API key, SQL, hay chi tiết kỹ thuật nội bộ.";
    }
}