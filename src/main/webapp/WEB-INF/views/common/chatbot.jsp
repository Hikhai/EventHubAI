<%@ page contentType="text/html;charset=UTF-8" %>
<%--
    Chatbot Widget - Hiển thị ở mọi trang
    Tự động include trong footer
--%>

<%-- Floating button (nút mở chatbot) --%>
<button id="chatbotToggle" class="chatbot-toggle" title="Hỏi EventHub AI">
    <i class="bi bi-chat-dots-fill"></i>
</button>

<%-- Chatbot window (ẩn ban đầu) --%>
<div id="chatbotWindow" class="chatbot-window hidden">

    <%-- Header --%>
    <div class="chatbot-header">
        <h5><i class="bi bi-robot"></i> EventHub AI</h5>
        <button id="chatbotClose" class="btn-close-chat">
            <i class="bi bi-x-lg"></i>
        </button>
    </div>

    <%-- Messages area --%>
    <div id="chatbotMessages" class="chatbot-messages">
        <%-- Tin nhắn chào mừng --%>
        <div class="chat-message assistant">
            <div class="chat-bubble">
                Xin chào! 👋 Tôi là trợ lý AI của EventHub.<br>
                Bạn có thể hỏi tôi về các sự kiện, cách đăng ký, hoặc bất kỳ điều gì!
            </div>
        </div>
    </div>

    <%-- Quick reply suggestions --%>
    <div class="quick-replies">
        <button class="quick-reply-btn">Sự kiện nào đang mở?</button>
        <button class="quick-reply-btn">Cách đăng ký sự kiện?</button>
    </div>

    <%-- Input area --%>
    <div class="chatbot-input">
        <input id="chatbotInput" type="text"
               placeholder="Nhập tin nhắn..."
               maxlength="500" autocomplete="off">
        <button id="chatbotSend" type="button">
            <i class="bi bi-send-fill"></i>
        </button>
    </div>
</div>