(function () {
    "use strict";

    const CHAT_API_URL = "/api/chatbot";
    const RESET_API_URL = "/api/chatbot/reset";
    const STORAGE_KEY = "cinema_ai_chatbot_session_v1";

    const DEFAULT_GREETING =
        "Xin chào! Mình là **Trợ lý AI Tư Vấn Phim**.\n" +
        "Mình sẽ giúp bạn chọn phim theo **địa điểm rạp**, **thể loại** và **khung giờ chiếu** phù hợp nhất.\n\n" +
        "Bạn đang ở **khu vực / quận / thành phố** nào ạ?";

    const DEFAULT_QUICK_REPLIES = [
        "Cầu Giấy",
        "Đống Đa",
        "Hà Nội",
        "Quận 1",
        "Quận 7",
        "TP.HCM",
        "Đà Nẵng"
    ];

    document.addEventListener("DOMContentLoaded", function () {
        const refs = ensureChatWidgetDom();
        if (!refs) {
            return;
        }

        let sessionData = loadSession();
        let isSending = false;

        if (!sessionData.messages || sessionData.messages.length === 0) {
            sessionData = createFreshSession();
            saveSession(sessionData);
        }

        renderAllMessages(refs.messagesEl, sessionData.messages);
        if (sessionData.isOpen) {
            openChatWindow(refs, sessionData, false);
        } else {
            closeChatWindow(refs, sessionData, false);
        }

        refs.toggleBtn.addEventListener("click", function () {
            if (sessionData.isOpen) {
                closeChatWindow(refs, sessionData, true);
            } else {
                openChatWindow(refs, sessionData, true);
            }
        });

        refs.closeBtn.addEventListener("click", function () {
            closeChatWindow(refs, sessionData, true);
            refs.toggleBtn.focus();
        });

        refs.resetBtn.addEventListener("click", async function () {
            if (isSending) {
                return;
            }
            try {
                await fetch(RESET_API_URL, { method: "POST" });
            } catch (_) {
                // Vẫn reset cục bộ ngay cả khi mất kết nối tạm thời
            }
            sessionData = createFreshSession(true);
            saveSession(sessionData);
            renderAllMessages(refs.messagesEl, sessionData.messages);
            refs.inputEl.focus();
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && sessionData.isOpen) {
                closeChatWindow(refs, sessionData, true);
                refs.toggleBtn.focus();
            }
        });

        refs.formEl.addEventListener("submit", function (event) {
            event.preventDefault();
            sendUserMessage(refs.inputEl.value);
        });

        async function sendUserMessage(rawText) {
            const text = (rawText || "").trim();
            if (!text || isSending) {
                return;
            }

            isSending = true;
            refs.inputEl.value = "";
            refs.sendBtn.disabled = true;

            // Xóa quick-replies ở các tin nhắn cũ để tránh bấm nhầm lượt cũ
            clearQuickRepliesOnPreviousMessages(sessionData.messages);

            sessionData.messages.push({
                role: "user",
                text: text
            });
            saveSession(sessionData);
            renderAllMessages(refs.messagesEl, sessionData.messages);

            const typingNode = appendTypingIndicator(refs.messagesEl);

            try {
                const payload = {
                    message: text,
                    conversation_id: sessionData.conversationId,
                    state: sessionData.state,
                    history: sessionData.messages.slice(-10).map(function (m) {
                        return { role: m.role, content: m.text };
                    })
                };

                const response = await fetch(CHAT_API_URL, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }

                const data = await response.json();
                sessionData.conversationId = data.conversation_id || sessionData.conversationId;
                sessionData.state = data.state || null;

                sessionData.messages.push({
                    role: "bot",
                    text: data.reply || "Mình đã xử lý xong yêu cầu của bạn.",
                    recommendations: Array.isArray(data.recommendations) ? data.recommendations : [],
                    quickReplies: Array.isArray(data.quick_replies) ? data.quick_replies : [],
                    usedIdealBusySchedule: Boolean(data.used_ideal_busy_schedule),
                    usedHighRatingFallback: Boolean(data.used_high_rating_fallback)
                });
            } catch (err) {
                console.error("Chatbot API error:", err);
                sessionData.messages.push({
                    role: "bot",
                    text: "Xin lỗi, hiện tại máy chủ AI đang bận hoặc mất kết nối. Bạn vui lòng thử lại sau giây lát nhé!",
                    recommendations: [],
                    quickReplies: ["Thử lại", "Làm mới"]
                });
            } finally {
                if (typingNode && typingNode.parentNode) {
                    typingNode.parentNode.removeChild(typingNode);
                }
                isSending = false;
                refs.sendBtn.disabled = false;
                saveSession(sessionData);
                renderAllMessages(refs.messagesEl, sessionData.messages);
                if (sessionData.isOpen) {
                    refs.inputEl.focus();
                }
            }
        }

        function renderAllMessages(container, messages) {
            container.innerHTML = "";
            messages.forEach(function (msg, index) {
                const isLatest = index === messages.length - 1;
                const el = createMessageElement(msg, isLatest);
                container.appendChild(el);
            });
            container.scrollTop = container.scrollHeight;
        }

        function createMessageElement(msg, isLatest) {
            const wrapper = document.createElement("div");
            wrapper.className = "msg " + (msg.role === "user" ? "user-msg" : "bot-msg ai-format");

            if (msg.role === "user") {
                wrapper.textContent = msg.text;
                return wrapper;
            }

            const textBlock = document.createElement("div");
            textBlock.className = "chat-bot-text";
            textBlock.innerHTML = formatBotMarkdown(msg.text);
            wrapper.appendChild(textBlock);

            if (Array.isArray(msg.recommendations) && msg.recommendations.length > 0) {
                const cardsWrap = document.createElement("div");
                cardsWrap.className = "chat-movie-cards";

                msg.recommendations.forEach(function (movie) {
                    cardsWrap.appendChild(createMovieRecommendationCard(movie));
                });

                wrapper.appendChild(cardsWrap);
            }

            if (isLatest && Array.isArray(msg.quickReplies) && msg.quickReplies.length > 0) {
                const chipsWrap = document.createElement("div");
                chipsWrap.className = "chat-quick-replies";

                msg.quickReplies.forEach(function (label) {
                    const btn = document.createElement("button");
                    btn.type = "button";
                    btn.className = "chat-quick-chip";
                    btn.textContent = label;
                    btn.addEventListener("click", function () {
                        sendUserMessage(label);
                    });
                    chipsWrap.appendChild(btn);
                });

                wrapper.appendChild(chipsWrap);
            }

            return wrapper;
        }
    });

    function ensureChatWidgetDom() {
        let container = document.getElementById("ai-chat-container");
        if (!container) {
            container = document.createElement("div");
            container.id = "ai-chat-container";
            container.className = "ai-chat-container";
            container.innerHTML =
                '<div id="ai-chat-window" class="ai-chat-window chat-hidden" role="dialog" aria-label="Trợ lý AI Tư Vấn Phim" hidden>' +
                    '<div class="chat-header">' +
                        '<div class="chat-header-info">' +
                            '<span class="chat-header-avatar" aria-hidden="true">AI</span>' +
                            '<div>' +
                                '<div class="chat-header-title">Trợ lý AI Tư Vấn Phim</div>' +
                                '<div class="chat-header-subtitle">Địa điểm • Thể loại • Giờ chiếu</div>' +
                            '</div>' +
                        '</div>' +
                        '<div class="chat-header-actions">' +
                            '<button type="button" id="reset-chat-btn" class="chat-reset-btn" title="Làm mới hội thoại" aria-label="Làm mới hội thoại">↺</button>' +
                            '<button type="button" id="close-chat-btn" class="close-chat-btn" title="Đóng khung chat" aria-label="Đóng khung chat">&times;</button>' +
                        '</div>' +
                    '</div>' +
                    '<div id="chat-messages" class="chat-messages" role="log" aria-live="polite"></div>' +
                    '<form id="chat-form" class="chat-input-area">' +
                        '<input type="text" id="chat-input" placeholder="Nhập khu vực, thể loại hoặc giờ chiếu..." autocomplete="off" aria-label="Tin nhắn tư vấn phim">' +
                        '<button type="submit" id="send-chat-btn" class="send-chat-btn" aria-label="Gửi tin nhắn">Gửi</button>' +
                    '</form>' +
                '</div>' +
                '<button type="button" id="ai-chat-toggle" class="ai-chat-toggle" aria-label="Mở Trợ lý AI tư vấn phim" aria-expanded="false" aria-controls="ai-chat-window">' +
                    '<span class="ai-toggle-icon" aria-hidden="true">' +
                        '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
                            '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>' +
                            '<path d="M9.5 9.5h.01"></path>' +
                            '<path d="M14.5 9.5h.01"></path>' +
                        '</svg>' +
                    '</span>' +
                    '<span class="ai-toggle-badge">AI</span>' +
                '</button>';
            document.body.appendChild(container);
        }

        const toggleBtn = document.getElementById("ai-chat-toggle");
        const windowEl = document.getElementById("ai-chat-window");
        const closeBtn = document.getElementById("close-chat-btn");
        const resetBtn = document.getElementById("reset-chat-btn");
        const messagesEl = document.getElementById("chat-messages");
        const inputEl = document.getElementById("chat-input");
        const sendBtn = document.getElementById("send-chat-btn");
        const formEl = document.getElementById("chat-form");

        if (!toggleBtn || !windowEl || !closeBtn || !resetBtn || !messagesEl || !inputEl || !sendBtn || !formEl) {
            return null;
        }

        return { toggleBtn, windowEl, closeBtn, resetBtn, messagesEl, inputEl, sendBtn, formEl };
    }

    function openChatWindow(refs, sessionData, focusInput) {
        refs.windowEl.hidden = false;
        refs.windowEl.classList.remove("chat-hidden");
        refs.toggleBtn.setAttribute("aria-expanded", "true");
        refs.toggleBtn.classList.add("chat-open");
        sessionData.isOpen = true;
        saveSession(sessionData);
        refs.messagesEl.scrollTop = refs.messagesEl.scrollHeight;
        if (focusInput) {
            refs.inputEl.focus();
        }
    }

    function closeChatWindow(refs, sessionData, persist) {
        refs.windowEl.hidden = true;
        refs.windowEl.classList.add("chat-hidden");
        refs.toggleBtn.setAttribute("aria-expanded", "false");
        refs.toggleBtn.classList.remove("chat-open");
        sessionData.isOpen = false;
        if (persist) {
            saveSession(sessionData);
        }
    }

    function createMovieRecommendationCard(movie) {
        const card = document.createElement("div");
        card.className = "chat-movie-card";

        const header = document.createElement("a");
        header.className = "chat-movie-link";
        const movieId = movie.movie_id;
        header.href = movieId ? "/series_detail.html?id=" + encodeURIComponent(String(movieId)) : "#";

        const safePoster = sanitizeUrl(movie.poster_url);
        if (safePoster) {
            const img = document.createElement("img");
            img.className = "chat-movie-poster";
            img.src = safePoster;
            img.alt = movie.title || "Poster phim";
            img.loading = "lazy";
            header.appendChild(img);
        }

        const meta = document.createElement("div");
        meta.className = "chat-movie-meta";

        const title = document.createElement("div");
        title.className = "chat-movie-title";
        title.textContent = movie.title || "Phim đề xuất";
        meta.appendChild(title);

        const sub = document.createElement("div");
        sub.className = "chat-movie-sub";
        const ratingText = typeof movie.rating === "number" ? "⭐ " + movie.rating.toFixed(1) + "/10" : "";
        const genresText = Array.isArray(movie.genres) && movie.genres.length > 0 ? movie.genres.join(", ") : "";
        sub.textContent = [ratingText, genresText].filter(Boolean).join(" • ");
        meta.appendChild(sub);

        header.appendChild(meta);
        card.appendChild(header);

        if (Array.isArray(movie.cinema_showtimes) && movie.cinema_showtimes.length > 0) {
            const showtimeList = document.createElement("div");
            showtimeList.className = "chat-movie-showtimes";

            movie.cinema_showtimes.forEach(function (cinemaGroup) {
                const row = document.createElement("div");
                row.className = "chat-cinema-row";

                const cinemaName = document.createElement("div");
                cinemaName.className = "chat-cinema-name";
                cinemaName.textContent = cinemaGroup.cinema_name || "Rạp chiếu";
                row.appendChild(cinemaName);

                if (Array.isArray(cinemaGroup.schedules)) {
                    cinemaGroup.schedules.forEach(function (sch) {
                        const schLine = document.createElement("div");
                        schLine.className = "chat-schedule-line";

                        const dateSpan = document.createElement("span");
                        dateSpan.className = "chat-schedule-date";
                        dateSpan.textContent = sch.date_label || sch.date || "";
                        schLine.appendChild(dateSpan);

                        if (sch.busy_badge) {
                            const badgeSpan = document.createElement("span");
                            badgeSpan.className = "chat-busy-badge";
                            badgeSpan.textContent = sch.busy_badge;
                            schLine.appendChild(badgeSpan);
                        }

                        if (Array.isArray(sch.times) && sch.times.length > 0) {
                            const timesWrap = document.createElement("div");
                            timesWrap.className = "chat-time-pills";
                            sch.times.forEach(function (t) {
                                const timePill = document.createElement("span");
                                timePill.className = "chat-time-pill";
                                timePill.textContent = t;
                                timesWrap.appendChild(timePill);
                            });
                            schLine.appendChild(timesWrap);
                        }

                        row.appendChild(schLine);
                    });
                }

                showtimeList.appendChild(row);
            });

            card.appendChild(showtimeList);
        }

        return card;
    }

    function appendTypingIndicator(container) {
        const el = document.createElement("div");
        el.className = "msg bot-msg chat-typing";
        el.innerHTML = '<span></span><span></span><span></span>';
        container.appendChild(el);
        container.scrollTop = container.scrollHeight;
        return el;
    }

    function formatBotMarkdown(raw) {
        const escaped = escapeHtml(raw || "");
        return escaped
            .replace(/\*\*([^*\n]+)\*\*/g, '<strong class="movie-highlight">$1</strong>')
            .replace(/_([^_\n]+)_/g, "<em>$1</em>")
            .replace(/`([^`\n]+)`/g, '<code class="chat-inline-code">$1</code>')
            .replace(/\n/g, "<br>");
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function sanitizeUrl(url) {
        if (!url || typeof url !== "string") {
            return null;
        }
        const trimmed = url.trim();
        if (trimmed.startsWith("https://") || trimmed.startsWith("http://") || trimmed.startsWith("/")) {
            return trimmed;
        }
        return null;
    }

    function clearQuickRepliesOnPreviousMessages(messages) {
        if (!Array.isArray(messages)) {
            return;
        }
        messages.forEach(function (m) {
            if (m && m.role === "bot") {
                m.quickReplies = [];
            }
        });
    }

    function createFreshSession(keepOpen) {
        return {
            conversationId: null,
            isOpen: Boolean(keepOpen),
            state: null,
            messages: [
                {
                    role: "bot",
                    text: DEFAULT_GREETING,
                    recommendations: [],
                    quickReplies: DEFAULT_QUICK_REPLIES.slice()
                }
            ]
        };
    }

    function loadSession() {
        try {
            const raw = sessionStorage.getItem(STORAGE_KEY);
            if (!raw) {
                return createFreshSession(false);
            }
            const parsed = JSON.parse(raw);
            if (!parsed || !Array.isArray(parsed.messages)) {
                return createFreshSession(false);
            }
            return parsed;
        } catch (_) {
            return createFreshSession(false);
        }
    }

    function saveSession(sessionData) {
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(sessionData));
        } catch (_) {
            // Bỏ qua nếu trình duyệt chặn sessionStorage
        }
    }
})();
