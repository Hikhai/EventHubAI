/* ============================================================
   CHATBOT WIDGET LOGIC
   ============================================================ */

document.addEventListener('DOMContentLoaded', function() {
    const toggleBtn = document.getElementById('chatbotToggle');
    const closeBtn = document.getElementById('chatbotClose');
    const chatWindow = document.getElementById('chatbotWindow');
    const messagesArea = document.getElementById('chatbotMessages');
    const input = document.getElementById('chatbotInput');
    const sendBtn = document.getElementById('chatbotSend');
    const quickReplies = document.querySelectorAll('.quick-reply-btn');

    // Nếu không có chatbot trên trang này thì thoát
    if (!toggleBtn) return;

    // Lấy context path từ meta tag
    const contextPath = document.querySelector('meta[name="context-path"]')
        ?.getAttribute('content') || '';

    // ===== TOGGLE OPEN/CLOSE =====
    toggleBtn.addEventListener('click', function() {
        chatWindow.classList.remove('hidden');
        toggleBtn.classList.add('hidden');
        input.focus();
    });

    closeBtn.addEventListener('click', function() {
        chatWindow.classList.add('hidden');
        toggleBtn.classList.remove('hidden');
    });

    // ===== SEND MESSAGE =====
    function sendMessage() {
        const message = input.value.trim();
        if (!message) return;

        // Hiển thị tin nhắn user ngay
        appendMessage('user', message);
        input.value = '';
        sendBtn.disabled = true;

        // Hiển thị typing indicator
        const typingId = showTyping();

        // Gọi API
        fetch(contextPath + '/api/chatbot', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: 'message=' + encodeURIComponent(message)
        })
            .then(res => res.json())
            .then(data => {
                removeTyping(typingId);
                if (data.success) {
                    appendMessage('assistant', data.reply, data.timestamp);
                } else {
                    appendMessage('assistant',
                        data.message || 'Xin lỗi, có lỗi xảy ra.');
                }
                sendBtn.disabled = false;
                input.focus();
            })
            .catch(err => {
                removeTyping(typingId);
                appendMessage('assistant', 'Xin lỗi, không thể kết nối tới máy chủ.');
                sendBtn.disabled = false;
                input.focus();
            });
    }

    // Enter để gửi
    input.addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    sendBtn.addEventListener('click', sendMessage);

    // ===== QUICK REPLY BUTTONS =====
    quickReplies.forEach(function(btn) {
        btn.addEventListener('click', function() {
            input.value = btn.textContent;
            sendMessage();
        });
    });

    // ===== APPEND MESSAGE TO CHAT =====
    function appendMessage(role, content, timestamp) {
        const wrapper = document.createElement('div');
        wrapper.className = 'chat-message ' + role;

        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble';
        bubble.textContent = content;

        wrapper.appendChild(bubble);

        if (timestamp) {
            const time = document.createElement('div');
            time.className = 'chat-time';
            time.textContent = timestamp;
            wrapper.appendChild(time);
        }

        messagesArea.appendChild(wrapper);
        // Auto scroll xuống cuối
        messagesArea.scrollTop = messagesArea.scrollHeight;
    }

    // ===== TYPING INDICATOR =====
    function showTyping() {
        const id = 'typing-' + Date.now();
        const wrapper = document.createElement('div');
        wrapper.id = id;
        wrapper.className = 'chat-message assistant';

        wrapper.innerHTML = `
            <div class="typing-indicator">
                <span></span><span></span><span></span>
            </div>
        `;

        messagesArea.appendChild(wrapper);
        messagesArea.scrollTop = messagesArea.scrollHeight;
        return id;
    }

    function removeTyping(id) {
        const elem = document.getElementById(id);
        if (elem) elem.remove();
    }
});