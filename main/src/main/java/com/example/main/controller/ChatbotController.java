package com.example.main.controller;

import com.example.main.dto.ChatbotRequest;
import com.example.main.dto.ChatbotResponse;
import com.example.main.dto.ChatbotStateDto;
import com.example.main.service.ChatbotService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private static final String SESSION_STATE_ATTR = "CHATBOT_CONVERSATION_STATE";

    private final ChatbotService chatbotService;

    /**
     * Endpoint hội thoại AI tư vấn phim theo địa điểm, thể loại và khung giờ chiếu.
     * Lưu ý: WebMvcConfig tự động thêm tiền tố /api -> /api/chatbot và /api/chatbot/
     */
    @PostMapping({"", "/"})
    public ResponseEntity<ChatbotResponse> chat(
            @RequestBody(required = false) ChatbotRequest request,
            HttpSession session
    ) {
        ChatbotRequest safeRequest = request != null ? request : new ChatbotRequest();
        ChatbotStateDto sessionState = (ChatbotStateDto) session.getAttribute(SESSION_STATE_ATTR);

        ChatbotResponse response = chatbotService.chat(safeRequest, sessionState);
        session.setAttribute(SESSION_STATE_ATTR, response.getState());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset(HttpSession session) {
        session.removeAttribute(SESSION_STATE_ATTR);
        return ResponseEntity.ok(Map.of("message", "Đã làm mới phiên tư vấn AI."));
    }
}

