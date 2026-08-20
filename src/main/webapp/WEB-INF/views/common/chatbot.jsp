<%@ page contentType="text/html;charset=UTF-8" %>

<button id="chatbotToggle" class="chatbot-toggle" title="Hỏi EventHub AI" type="button">
    <i class="bi bi-chat-dots-fill"></i>
</button>

<div id="chatbotWindow" class="chatbot-window hidden">
    <div class="chatbot-header">
        <div>
            <h5><i class="bi bi-robot"></i> EventHub AI</h5>
            <div class="chatbot-status">Trực tuyến · sẵn sàng hỗ trợ</div>
        </div>
        <button id="chatbotClose" class="btn-close-chat" type="button" aria-label="Đóng">
            <i class="bi bi-x-lg"></i>
        </button>
    </div>

    <div id="chatbotMessages" class="chatbot-messages">
        <div class="chat-message assistant">
            <div class="chat-bubble">
                Xin chào! Mình là trợ lý EventHub. Bạn có thể hỏi về sự kiện đang mở, cách đăng ký, hoặc gợi ý phù hợp với bạn.
            </div>
        </div>
    </div>

    <div class="quick-replies">
        <button class="quick-reply-btn" type="button">Sự kiện nào đang mở?</button>
        <button class="quick-reply-btn" type="button">Cách đăng ký sự kiện?</button>
    </div>

    <div class="chatbot-input">
        <input id="chatbotInput" type="text"
               placeholder="Nhập tin nhắn..."
               maxlength="500" autocomplete="off">
        <button id="chatbotSend" type="button" aria-label="Gửi">
            <i class="bi bi-send-fill"></i>
        </button>
    </div>
</div>
