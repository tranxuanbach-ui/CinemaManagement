package com.example.main.service;

import com.example.main.dto.ChatbotMovieRecommendationDto;
import com.example.main.dto.ChatbotRequest;
import com.example.main.dto.ChatbotResponse;
import com.example.main.entity.Cinema;
import com.example.main.entity.CinemaSystem;
import com.example.main.entity.Genre;
import com.example.main.entity.Movie;
import com.example.main.entity.Showtime;
import com.example.main.repository.CinemaRepository;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private ChatbotGeminiClient geminiClient;

    private ChatbotService chatbotService;

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TODAY.atTime(10, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
            ZoneId.of("Asia/Ho_Chi_Minh")
    );

    private Movie topComedyMovie;
    private Movie actionMovie;
    private Cinema cgvCinema;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                cinemaRepository,
                movieRepository,
                showtimeRepository,
                geminiClient,
                FIXED_CLOCK
        );

        CinemaSystem cgv = new CinemaSystem("CGV");
        cgv.setId(1L);

        cgvCinema = new Cinema();
        cgvCinema.setId(10L);
        cgvCinema.setName("CGV Indochina Plaza Hà Nội");
        cgvCinema.setSystem(cgv);

        Genre comedy = new Genre("Hài Hước");
        comedy.setId(1L);

        Genre action = new Genre("Hành Động");
        action.setId(2L);

        topComedyMovie = new Movie();
        topComedyMovie.setId(101L);
        topComedyMovie.setTitle("Bò Sữa Siêu Quậy");
        topComedyMovie.setRating(9.2);
        topComedyMovie.setDefaultRating(9.0);
        topComedyMovie.setRuntimeMinutes(98);
        topComedyMovie.setPosterUrl("https://example.com/poster.jpg");
        topComedyMovie.setGenres(Set.of(comedy));

        actionMovie = new Movie();
        actionMovie.setId(102L);
        actionMovie.setTitle("Tử Chiến Trên Không");
        actionMovie.setRating(8.8);
        actionMovie.setDefaultRating(8.5);
        actionMovie.setRuntimeMinutes(120);
        actionMovie.setPosterUrl("https://example.com/action.jpg");
        actionMovie.setGenres(Set.of(action));
    }

    @Test
    void shouldReturnDefaultGreetingWhenUserMessageIsEmpty() {
        ChatbotResponse res = chatbotService.chat(
                ChatbotRequest.builder().message("").build(),
                null
        );

        assertNotNull(res);
        assertEquals(ChatbotService.DEFAULT_GREETING, res.getReply());
        assertTrue(res.getRecommendations().isEmpty());
        assertTrue(res.getQuickReplies().isEmpty());
        verifyNoInteractions(geminiClient);
    }

    @Test
    void shouldHandleResetCommandGracefully() {
        ChatbotResponse res = chatbotService.chat(
                ChatbotRequest.builder().message("làm mới").build(),
                null
        );

        assertNotNull(res);
        assertTrue(res.getReply().contains("Đã làm mới"));
        assertTrue(res.getRecommendations().isEmpty());
        assertTrue(res.getQuickReplies().isEmpty());
        verifyNoInteractions(geminiClient);
    }

    @Test
    void shouldReturnBusyMessageWhenGeminiFailsOrKeyMissing() {
        when(movieRepository.findAllWithGenres()).thenReturn(List.of(topComedyMovie, actionMovie));
        when(cinemaRepository.findAllWithSystem()).thenReturn(List.of(cgvCinema));
        when(geminiClient.generateChatResponse(anyString(), anyList(), anyList(), anyList(), any()))
                .thenReturn(Optional.empty());

        ChatbotResponse res = chatbotService.chat(
                ChatbotRequest.builder().message("Tư vấn cho mình phim hài hay nhất với").build(),
                null
        );

        assertNotNull(res);
        assertEquals(ChatbotService.BUSY_FALLBACK_MESSAGE, res.getReply());
        assertEquals("ai-busy", res.getAiProvider());
        assertTrue(res.getRecommendations().isEmpty());
        assertTrue(res.getQuickReplies().isEmpty());
    }

    @Test
    void shouldReturnGeminiResponseAndAttachMentionedMovieCards() {
        when(movieRepository.findAllWithGenres()).thenReturn(List.of(topComedyMovie, actionMovie));
        when(cinemaRepository.findAllWithSystem()).thenReturn(List.of(cgvCinema));
        when(geminiClient.generateChatResponse(anyString(), anyList(), anyList(), anyList(), any()))
                .thenReturn(Optional.of("Hôm nay bạn hãy thử xem phim **Bò Sữa Siêu Quậy** nhé! Phim cực kỳ hài hước và thư giãn!"));

        ChatbotResponse res = chatbotService.chat(
                ChatbotRequest.builder().message("Mình muốn xem phim gì cười nhiều").build(),
                null
        );

        assertNotNull(res);
        assertEquals("gemini-flash-lite", res.getAiProvider());
        assertTrue(res.getReply().contains("Bò Sữa Siêu Quậy"));
        assertEquals(1, res.getRecommendations().size());
        assertTrue(res.getQuickReplies().isEmpty());

        ChatbotMovieRecommendationDto rec = res.getRecommendations().get(0);
        assertEquals(101L, rec.getMovieId());
        assertEquals("Bò Sữa Siêu Quậy", rec.getTitle());
        assertEquals(9.2, rec.getRating());
        assertNotNull(rec.getCinemaShowtimes());
        assertTrue(rec.getCinemaShowtimes().isEmpty());
    }

    @Test
    void shouldPassUserLocationFromWebToGeminiClient() {
        when(movieRepository.findAllWithGenres()).thenReturn(List.of(topComedyMovie));
        when(cinemaRepository.findAllWithSystem()).thenReturn(List.of(cgvCinema));
        when(geminiClient.generateChatResponse(anyString(), anyList(), anyList(), anyList(), eq("Cầu Giấy, Hà Nội")))
                .thenReturn(Optional.of("Rạp gần Cầu Giấy nhất là Indochina Plaza!"));

        ChatbotResponse res = chatbotService.chat(
                ChatbotRequest.builder()
                        .message("Phim này chiếu ở đâu gần mình?")
                        .userLocation("Cầu Giấy, Hà Nội")
                        .build(),
                null
        );

        assertNotNull(res);
        assertEquals("Rạp gần Cầu Giấy nhất là Indochina Plaza!", res.getReply());
        verify(geminiClient).generateChatResponse(
                eq("Phim này chiếu ở đâu gần mình?"),
                anyList(),
                anyList(),
                anyList(),
                eq("Cầu Giấy, Hà Nội")
        );
    }
}
