package com.example.main.controller;

import com.example.main.config.SecurityConfig;
import com.example.main.config.WebMvcConfig;
import com.example.main.dto.ChatbotRequest;
import com.example.main.dto.ChatbotResponse;
import com.example.main.dto.ChatbotStateDto;
import com.example.main.enums.ChatbotStep;
import com.example.main.security.CustomUserDetailsService;
import com.example.main.service.ChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatbotController.class)
@Import({WebMvcConfig.class, SecurityConfig.class})
class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatbotService chatbotService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldAllowAnonymousPostToApiChatbotAndPersistSessionState() throws Exception {
        ChatbotStateDto returnedState = ChatbotStateDto.builder()
                .location("Cầu Giấy (Hà Nội)")
                .matchedCinemaIds(List.of(10L, 11L))
                .pendingStep(ChatbotStep.GENRE)
                .build();

        ChatbotResponse serviceResponse = ChatbotResponse.builder()
                .conversationId("conv-1")
                .pendingStep(ChatbotStep.GENRE)
                .state(returnedState)
                .matchedCinemas(List.of())
                .recommendations(List.of())
                .quickReplies(List.of("Không cần", "Hành Động"))
                .aiProvider("local-nlp")
                .reply("Mình đã tìm thấy 2 rạp gần khu vực Cầu Giấy!")
                .build();

        when(chatbotService.chat(any(ChatbotRequest.class), isNull())).thenReturn(serviceResponse);

        MockHttpSession session = new MockHttpSession();

        // Lượt 1: Truy cập /api/chatbot/ không cần đăng nhập
        mockMvc.perform(post("/api/chatbot/")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Mình ở Cầu Giấy"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending_step").value("GENRE"))
                .andExpect(jsonPath("$.reply").value("Mình đã tìm thấy 2 rạp gần khu vực Cầu Giấy!"))
                .andExpect(jsonPath("$.quick_replies[0]").value("Không cần"));

        // Lượt 2: Truy cập /api/chatbot dùng lại HttpSession đã lưu state
        ChatbotStateDto step2State = returnedState.copy();
        step2State.setGenreSkipped(true);
        step2State.setPendingStep(ChatbotStep.TIME);

        when(chatbotService.chat(any(ChatbotRequest.class), any(ChatbotStateDto.class)))
                .thenReturn(ChatbotResponse.builder()
                        .conversationId("conv-1")
                        .pendingStep(ChatbotStep.TIME)
                        .state(step2State)
                        .matchedCinemas(List.of())
                        .recommendations(List.of())
                        .quickReplies(List.of("Không cần"))
                        .usedHighRatingFallback(true)
                        .aiProvider("local-nlp")
                        .reply("Đây là top phim rating cao nhất!")
                        .build());

        mockMvc.perform(post("/api/chatbot")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"không cần"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending_step").value("TIME"))
                .andExpect(jsonPath("$.used_high_rating_fallback").value(true));

        verify(chatbotService).chat(any(ChatbotRequest.class), any(ChatbotStateDto.class));
    }

    @Test
    void shouldAllowAnonymousResetEndpoint() throws Exception {
        mockMvc.perform(post("/api/chatbot/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}

