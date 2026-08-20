document.addEventListener('DOMContentLoaded', function () {
    const toggleBtn = document.getElementById('chatbotToggle');
    const closeBtn = document.getElementById('chatbotClose');
    const chatWindow = document.getElementById('chatbotWindow');
    const messagesArea = document.getElementById('chatbotMessages');
    const input = document.getElementById('chatbotInput');
    const sendBtn = document.getElementById('chatbotSend');
    const quickReplies = document.querySelectorAll('.quick-reply-btn');

    if (!toggleBtn) return;

    const contextPath = document.querySelector('meta[name="context-path"]')
        ?.getAttribute('content') || '';

    let sending = false;

    function openChat() {
        chatWindow.classList.remove('hidden');
        toggleBtn.classList.add('hidden');
        input.focus();
    }

    function closeChat() {
        chatWindow.classList.add('hidden');
        toggleBtn.classList.remove('hidden');
    }

    toggleBtn.addEventListener('click', openChat);
    closeBtn.addEventListener('click', closeChat);

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !chatWindow.classList.contains('hidden')) {
            closeChat();
        }
    });

    function sendMessage() {
        const message = input.value.trim();
        if (!message || sending) return;

        sending = true;
        appendMessage('user', message);
        input.value = '';
        sendBtn.disabled = true;

        const typingId = showTyping();

        fetch(contextPath + '/api/chatbot', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: 'message=' + encodeURIComponent(message)
        })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                removeTyping(typingId);
                if (data.success) {
                    appendMessage('assistant', data.reply, data.timestamp);
                } else {
                    appendMessage('assistant', data.message || 'Xin lỗi, có lỗi xảy ra.');
                }
            })
            .catch(function () {
                removeTyping(typingId);
                appendMessage('assistant', 'Xin lỗi, không thể kết nối tới máy chủ.');
            })
            .finally(function () {
                sending = false;
                sendBtn.disabled = false;
                input.focus();
            });
    }

    input.addEventListener('keypress', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    sendBtn.addEventListener('click', sendMessage);

    quickReplies.forEach(function (btn) {
        btn.addEventListener('click', function () {
            input.value = btn.textContent;
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
            bubble.innerHTML = formatChatHtml(content);
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

    function inlineFormat(text) {
        return text
            .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
            .replace(/`([^`]+)`/g, '<code>$1</code>');
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
        return line.trim().indexOf('|') !== -1;
    }

    function isBullet(line) {
        return /^\s*[-*•]\s+/.test(line);
    }

    function isOrdered(line) {
        return /^\s*\d+[.)]\s+/.test(line);
    }

    function normalizeChatText(raw) {
        return String(raw)
            .replace(/\r\n/g, '\n')
            .replace(/\*\*Bước\s*(\d+)\s*:\*\*\s*/gi, '\n\n$1. ')
            .replace(/Bước\s*(\d+)\s*:\s*/gi, '\n$1. ')
            .replace(/\*\*([^*]+)\*\*(?=\S)/g, '**$1** ')
            .replace(/\s+\*\s+\*\*/g, '\n**')
            .replace(/\n{3,}/g, '\n\n')
            .trim();
    }

    function formatChatHtml(raw) {
        const normalized = normalizeChatText(String(raw || ''));
        const escaped = escapeHtml(normalized);
        const lines = escaped.split('\n');
        const out = [];
        let i = 0;

        while (i < lines.length) {
            const line = lines[i];

            if (line.trim() === '') {
                i += 1;
                continue;
            }

            if (isTableRow(line) && i + 1 < lines.length && isTableSeparator(lines[i + 1])) {
                const headers = splitTableRow(line);
                i += 2;
                const rows = [];
                while (i < lines.length && isTableRow(lines[i]) && !isTableSeparator(lines[i])) {
                    rows.push(splitTableRow(lines[i]));
                    i += 1;
                }
                out.push(renderTable(headers, rows));
                continue;
            }

            if (isBullet(line)) {
                const items = [];
                while (i < lines.length && isBullet(lines[i])) {
                    items.push(lines[i].replace(/^\s*[-*•]\s+/, ''));
                    i += 1;
                }
                out.push('<ul>' + items.map(function (item) {
                    return '<li>' + inlineFormat(item) + '</li>';
                }).join('') + '</ul>');
                continue;
            }

            if (isOrdered(line)) {
                const items = [];
                while (i < lines.length && isOrdered(lines[i])) {
                    items.push(lines[i].replace(/^\s*\d+[.)]\s+/, ''));
                    i += 1;
                }
                out.push('<ol>' + items.map(function (item) {
                    return '<li>' + inlineFormat(item) + '</li>';
                }).join('') + '</ol>');
                continue;
            }

            const para = [];
            while (i < lines.length
                    && lines[i].trim() !== ''
                    && !isTableRow(lines[i])
                    && !isBullet(lines[i])
                    && !isOrdered(lines[i])) {
                para.push(inlineFormat(lines[i].trim()));
                i += 1;
            }
            out.push('<p>' + para.join('<br>') + '</p>');
        }

        return out.join('') || '<p></p>';
    }

    function renderTable(headers, rows) {
        let html = '<div class="chat-md-table-wrap"><table class="chat-md-table"><thead><tr>';
        headers.forEach(function (h) {
            html += '<th>' + inlineFormat(h) + '</th>';
        });
        html += '</tr></thead><tbody>';
        rows.forEach(function (row) {
            html += '<tr>';
            headers.forEach(function (_, idx) {
                html += '<td>' + inlineFormat(row[idx] || '') + '</td>';
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
