document.addEventListener('DOMContentLoaded', function () {
    const toggleBtn = document.getElementById('chatbotToggle');
    const closeBtn = document.getElementById('chatbotClose');
    const chatWindow = document.getElementById('chatbotWindow');
    const messagesArea = document.getElementById('chatbotMessages');
    const input = document.getElementById('chatbotInput');
    const sendBtn = document.getElementById('chatbotSend');
    const quickReplies = document.querySelectorAll('.quick-reply-btn');

    if (!toggleBtn || !chatWindow) return;

    const contextPath = document.querySelector('meta[name="context-path"]')
        ?.getAttribute('content') || '';

    let sending = false;

    function openChat() {
        chatWindow.classList.remove('hidden');
        toggleBtn.classList.add('hidden');
        if (input) {
            setTimeout(function () { input.focus(); }, 100);
        }
    }

    function closeChat() {
        chatWindow.classList.add('hidden');
        toggleBtn.classList.remove('hidden');
    }

    toggleBtn.addEventListener('click', openChat);
    if (closeBtn) {
        closeBtn.addEventListener('click', closeChat);
    }

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !chatWindow.classList.contains('hidden')) {
            closeChat();
        }
    });

    function sendMessage() {
        if (!input) return;
        const message = input.value.trim();
        if (!message || sending) return;

        sending = true;
        appendMessage('user', message);
        input.value = '';
        if (sendBtn) sendBtn.disabled = true;

        const typingId = showTyping();

        fetch(contextPath + '/api/chatbot', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: 'message=' + encodeURIComponent(message)
        })
            .then(function (res) {
                if (res.status === 401) {
                    return {
                        success: false,
                        message: 'Vui lòng <a href="' + contextPath + '/auth/login">đăng nhập</a> để trò chuyện cùng trợ lý AI.'
                    };
                }
                return res.json();
            })
            .then(function (data) {
                removeTyping(typingId);
                if (data.success) {
                    appendMessage('assistant', data.reply, data.timestamp);
                } else {
                    appendMessage('assistant', data.message || 'Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.');
                }
            })
            .catch(function () {
                removeTyping(typingId);
                appendMessage('assistant', 'Xin lỗi, không thể kết nối tới máy chủ. Vui lòng kiểm tra lại kết nối!');
            })
            .finally(function () {
                sending = false;
                if (sendBtn) sendBtn.disabled = false;
                if (input) input.focus();
            });
    }

    if (input) {
        input.addEventListener('keypress', function (e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }

    if (sendBtn) {
        sendBtn.addEventListener('click', sendMessage);
    }

    quickReplies.forEach(function (btn) {
        btn.addEventListener('click', function () {
            if (!input) return;
            input.value = btn.textContent.trim();
            sendMessage();
        });
    });

    function appendMessage(role, content, timestamp) {
        const wrapper = document.createElement('div');
        wrapper.className = 'chat-message ' + role;

        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble';
        if (role === 'assistant') {
            bubble.classList.add('chat-bubble-md');
            bubble.innerHTML = formatChatHtml(content, contextPath);
            if (bubble.querySelector('table')) {
                bubble.classList.add('has-table');
            }
        } else {
            bubble.textContent = content;
        }
        wrapper.appendChild(bubble);

        if (timestamp) {
            const time = document.createElement('div');
            time.className = 'chat-time';
            time.textContent = timestamp;
            wrapper.appendChild(time);
        }

        messagesArea.appendChild(wrapper);
        messagesArea.scrollTop = messagesArea.scrollHeight;
    }

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function inlineFormat(text, ctx) {
        ctx = ctx || '';
        return String(text)
            .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
            .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
            .replace(/\*([^*]+)\*/g, '<em>$1</em>')
            .replace(/`([^`]+)`/g, '<code>$1</code>')
            .replace(/(^|\s)(\/(?:events|my-events|auth\/login|auth\/register)[^\s.,;)]*)/g, '$1<a href="' + ctx + '$2">$2</a>');
    }

    function splitTableRow(line) {
        let s = line.trim();
        if (s.startsWith('|')) s = s.slice(1);
        if (s.endsWith('|')) s = s.slice(0, -1);
        return s.split('|').map(function (cell) { return cell.trim(); });
    }

    function isTableSeparator(line) {
        return /^\s*\|?(\s*:?-+:?\s*\|)+\s*:?-+:?\s*\|?\s*$/.test(line);
    }

    function isTableRow(line) {
        const trimmed = line.trim();
        return trimmed.indexOf('|') !== -1 && splitTableRow(trimmed).length >= 2;
    }

    function isBullet(line) {
        return /^\s*[-*•]\s+/.test(line);
    }

    function isOrdered(line) {
        return /^\s*\d+[.)]\s+/.test(line);
    }

    function isHeading(line) {
        return /^\s*#{1,4}\s+/.test(line);
    }

    function formatChatHtml(raw, ctx) {
        ctx = ctx || '';
        const clean = String(raw || '').replace(/\r\n/g, '\n').trim();
        const escaped = escapeHtml(clean);
        const lines = escaped.split('\n');
        const out = [];
        let i = 0;

        while (i < lines.length) {
            const line = lines[i];

            if (line.trim() === '') {
                i++;
                continue;
            }

            // Bảng Markdown
            if (isTableRow(line) && i + 1 < lines.length && isTableSeparator(lines[i + 1])) {
                const headers = splitTableRow(line);
                i += 2;
                const rows = [];
                while (i < lines.length && isTableRow(lines[i]) && !isTableSeparator(lines[i])) {
                    rows.push(splitTableRow(lines[i]));
                    i++;
                }
                // Chỉ hiển thị bảng nếu có ít nhất 1 dòng dữ liệu
                if (rows.length > 0) {
                    out.push(renderTable(headers, rows, ctx));
                }
                continue;
            }

            // Tiêu đề Markdown (# / ## / ###)
            if (isHeading(line)) {
                const headingText = line.replace(/^\s*#{1,4}\s+/, '');
                out.push('<div class="chat-heading">' + inlineFormat(headingText, ctx) + '</div>');
                i++;
                continue;
            }

            // Danh sách không thứ tự (- / * / •)
            if (isBullet(line)) {
                const items = [];
                while (i < lines.length && isBullet(lines[i])) {
                    items.push(lines[i].replace(/^\s*[-*•]\s+/, ''));
                    i++;
                }
                out.push('<ul>' + items.map(function (item) {
                    return '<li>' + inlineFormat(item, ctx) + '</li>';
                }).join('') + '</ul>');
                continue;
            }

            // Danh sách có thứ tự (1. / 2.)
            if (isOrdered(line)) {
                const items = [];
                while (i < lines.length && isOrdered(lines[i])) {
                    items.push(lines[i].replace(/^\s*\d+[.)]\s+/, ''));
                    i++;
                }
                out.push('<ol>' + items.map(function (item) {
                    return '<li>' + inlineFormat(item, ctx) + '</li>';
                }).join('') + '</ol>');
                continue;
            }

            // Đoạn văn thông thường
            const para = [];
            while (i < lines.length
                    && lines[i].trim() !== ''
                    && !isTableRow(lines[i])
                    && !isHeading(lines[i])
                    && !isBullet(lines[i])
                    && !isOrdered(lines[i])) {
                para.push(inlineFormat(lines[i].trim(), ctx));
                i++;
            }
            if (para.length > 0) {
                out.push('<p>' + para.join('<br>') + '</p>');
            }
        }

        return out.join('') || '<p></p>';
    }

    function renderTable(headers, rows, ctx) {
        let html = '<div class="chat-md-table-wrap"><table class="chat-md-table"><thead><tr>';
        headers.forEach(function (h) {
            html += '<th>' + inlineFormat(h, ctx) + '</th>';
        });
        html += '</tr></thead><tbody>';
        rows.forEach(function (row) {
            html += '<tr>';
            headers.forEach(function (_, idx) {
                html += '<td>' + inlineFormat(row[idx] || '', ctx) + '</td>';
            });
            html += '</tr>';
        });
        html += '</tbody></table></div>';
        return html;
    }

    function showTyping() {
        const id = 'typing-' + Date.now();
        const wrapper = document.createElement('div');
        wrapper.id = id;
        wrapper.className = 'chat-message assistant';
        wrapper.innerHTML = '<div class="typing-indicator"><span></span><span></span><span></span></div>';
        messagesArea.appendChild(wrapper);
        messagesArea.scrollTop = messagesArea.scrollHeight;
        return id;
    }

    function removeTyping(id) {
        const elem = document.getElementById(id);
        if (elem) elem.remove();
    }
});
