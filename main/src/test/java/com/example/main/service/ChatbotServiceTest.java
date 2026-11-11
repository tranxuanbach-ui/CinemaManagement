package com.example.main.service;

import com.example.main.dto.ChatbotMovieRecommendationDto;
import com.example.main.dto.ChatbotRequest;
import com.example.main.dto.ChatbotResponse;
import com.example.main.entity.Cinema;
import com.example.main.entity.CinemaSystem;
import com.example.main.entity.Genre;
import com.example.main.entity.Movie;
import com.example.main.entity.Showtime;
import com.example.main.enums.ChatbotStep;
import com.example.main.repository.CinemaRepository;
import com.example.main.repository.GenreRepository;
import com.example.main.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;

    private ChatbotService chatbotService;

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11); // Thứ Tư
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TODAY.atTime(10, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
            ZoneId.of("Asia/Ho_Chi_Minh")
    );

    private Cinema cgvCauGiay;
    private Cinema bhdCauGiay;
    private Cinema cgvHaiBaTrung;
    private Cinema cgvQuan7;
    private Cinema cgvHoangMai;

    private Genre actionGenre;
    private Genre horrorGenre;
    private Genre comedyGenre;

    private Movie topRatedActionMovie;
    private Movie mediumHorrorMovie;
    private Movie topRatedComedyMovie;

    @BeforeEach
    void setUp() {
        ChatbotLocationMatcher locationMatcher = new ChatbotLocationMatcher();
        ChatbotGeminiClient geminiClient = new ChatbotGeminiClient("", "gemini-flash-latest", new ObjectMapper());
        ChatbotQueryExtractor extractor = new ChatbotQueryExtractor(locationMatcher, geminiClient);
        chatbotService = new ChatbotService(
                cinemaRepository, genreRepository, showtimeRepository, locationMatcher, extractor, FIXED_CLOCK
        );

        CinemaSystem cgv = makeSystem(1L, "CGV");
        CinemaSystem bhd = makeSystem(2L, "BHD");

        cgvCauGiay = makeCinema(10L, cgv, "CGV Indochina Plaza Hà Nội");
        bhdCauGiay = makeCinema(11L, bhd, "BHD Star Discovery");
        cgvHaiBaTrung = makeCinema(12L, cgv, "CGV Vincom Center Bà Triệu");
        cgvQuan7 = makeCinema(13L, cgv, "CGV Crescent Mall");
        cgvHoangMai = makeCinema(14L, cgv, "CGV Rice City");

        actionGenre = makeGenre(1L, "Hành Động");
        horrorGenre = makeGenre(2L, "Kinh Dị");
        comedyGenre = makeGenre(3L, "Hài Hước");

        topRatedActionMovie = makeMovie(100L, "Chiến Binh Bóng Đêm", 9.1, 112, "https://example.com/action.jpg", Set.of(actionGenre));
        mediumHorrorMovie = makeMovie(101L, "Lời Nguyền Rạp Cũ", 7.8, 98, "https://example.com/horror.jpg", Set.of(horrorGenre));
        topRatedComedyMovie = makeMovie(102L, "Bò Sữa Siêu Quậy", 9.4, 105, "https://example.com/comedy.jpg", Set.of(comedyGenre));

        when(cinemaRepository.findAllWithSystem()).thenReturn(List.of(
                cgvCauGiay, bhdCauGiay, cgvHaiBaTrung, cgvQuan7, cgvHoangMai
        ));
        when(genreRepository.findAll()).thenReturn(List.of(actionGenre, horrorGenre, comedyGenre));
    }

    @Test
    void shouldAskLocationFirstWhenUserProvidesNoLocation() {
        ChatbotResponse res = chatbotService.chat(
                ChatbotRequest.builder().message("Tư vấn giúp mình một bộ phim hay").build(),
                null
        );

        assertEquals(ChatbotStep.LOCATION, res.getPendingStep());
        assertTrue(res.getMatchedCinemas().isEmpty());
        assertTrue(res.getRecommendations().isEmpty());
        assertTrue(res.getReply().toLowerCase().contains("khu vực") || res.getReply().toLowerCase().contains("địa điểm"));
    }

    @Test
    void shouldFollowFull3StepFlowAndUseHighRatingPlusIdealBusyShowtimesWhenSkipped() {
        when(showtimeRepository.findWithDetailsByCinemaIds(anyList())).thenReturn(List.of(
                makeShowtime(1L, topRatedComedyMovie, cgvCauGiay, TODAY, LocalTime.of(14, 30), "2D PHỤ ĐỀ"),
                makeShowtime(2L, topRatedComedyMovie, cgvCauGiay, TODAY, LocalTime.of(19, 30), "2D PHỤ ĐỀ"),
                makeShowtime(3L, topRatedComedyMovie, bhdCauGiay, TODAY, LocalTime.of(21, 45), "2D PHỤ ĐỀ"),
                makeShowtime(4L, topRatedActionMovie, cgvCauGiay, TODAY, LocalTime.of(12, 15), "2D PHỤ ĐỀ"),
                makeShowtime(5L, topRatedActionMovie, bhdCauGiay, TODAY, LocalTime.of(20, 0), "2D PHỤ ĐỀ")
        ));

        // Bước 1: Cung cấp địa điểm Cầu Giấy -> AI tìm đúng rạp Cầu Giấy và hỏi thể loại
        ChatbotResponse step1 = chatbotService.chat(
                ChatbotRequest.builder().message("Mình đang ở Cầu Giấy").build(),
                null
        );
        assertEquals(ChatbotStep.GENRE, step1.getPendingStep());
        assertEquals(2, step1.getMatchedCinemas().size());
        assertTrue(step1.getMatchedCinemas().stream().anyMatch(c -> c.getName().contains("Indochina")));
        assertTrue(step1.getMatchedCinemas().stream().anyMatch(c -> c.getName().contains("Discovery")));

        // Bước 2: User trả lời "không cần" thể loại -> AI lấy top rating và hỏi khung giờ chiếu
        ChatbotResponse step2 = chatbotService.chat(
                ChatbotRequest.builder().message("không cần").state(step1.getState()).build(),
                null
        );
        assertEquals(ChatbotStep.TIME, step2.getPendingStep());
        assertTrue(step2.isUsedHighRatingFallback());
        assertTrue(step2.getReply().contains("Bò Sữa Siêu Quậy"));

        // Bước 3: User trả lời "không cần" khung giờ -> AI đề xuất khung giờ lý tưởng cho người bận rộn
        ChatbotResponse step3 = chatbotService.chat(
                ChatbotRequest.builder().message("không cần").state(step2.getState()).build(),
                null
        );
        assertEquals(ChatbotStep.COMPLETED, step3.getPendingStep());
        assertTrue(step3.isUsedHighRatingFallback());
        assertTrue(step3.isUsedIdealBusySchedule());
        assertFalse(step3.getRecommendations().isEmpty());

        ChatbotMovieRecommendationDto firstMovie = step3.getRecommendations().get(0);
        assertEquals("Bò Sữa Siêu Quậy", firstMovie.getTitle());

        List<String> allRecommendedTimes = firstMovie.getCinemaShowtimes().stream()
                .flatMap(g -> g.getSchedules().stream())
                .flatMap(s -> s.getTimes().stream())
                .toList();
        assertTrue(allRecommendedTimes.contains("19:30"));
        assertFalse(allRecommendedTimes.contains("14:30"));
    }

    @Test
    void shouldFilterBySpecificGenreAndSpecificTimeWindowAcrossTurns() {
        when(showtimeRepository.findWithDetailsByCinemaIds(anyList())).thenReturn(List.of(
                makeShowtime(10L, topRatedActionMovie, cgvCauGiay, TODAY, LocalTime.of(10, 0), "2D PHỤ ĐỀ"),
                makeShowtime(11L, topRatedActionMovie, cgvCauGiay, TODAY, LocalTime.of(19, 15), "2D PHỤ ĐỀ"),
                makeShowtime(12L, mediumHorrorMovie, cgvCauGiay, TODAY, LocalTime.of(20, 0), "2D PHỤ ĐỀ")
        ));

        // Turn 1: có cả thể loại & giờ nhưng thiếu địa điểm -> AI ghi nhớ và hỏi địa điểm
        ChatbotResponse turn1 = chatbotService.chat(
                ChatbotRequest.builder().message("Tôi muốn xem phim hành động vào buổi tối").build(),
                null
        );
        assertEquals(ChatbotStep.LOCATION, turn1.getPendingStep());
        assertEquals("Hành Động", turn1.getState().getGenre());
        assertEquals("EVENING", turn1.getState().getTimeSlot());

        // Turn 2: cung cấp địa điểm Cầu Giấy -> trả thẳng kết quả đã lọc
        ChatbotResponse turn2 = chatbotService.chat(
                ChatbotRequest.builder().message("Ở Cầu Giấy").state(turn1.getState()).build(),
                null
        );
        assertEquals(ChatbotStep.COMPLETED, turn2.getPendingStep());
        assertFalse(turn2.isUsedHighRatingFallback());
        assertFalse(turn2.isUsedIdealBusySchedule());
        assertEquals(1, turn2.getRecommendations().size());
        assertEquals("Chiến Binh Bóng Đêm", turn2.getRecommendations().get(0).getTitle());

        List<String> times = turn2.getRecommendations().get(0).getCinemaShowtimes().get(0).getSchedules().get(0).getTimes();
        assertEquals(List.of("19:15"), times);
    }

    @Test
    void shouldHandleAllThreeSlotsInSingleMessageAtDistrict7() {
        when(showtimeRepository.findWithDetailsByCinemaIds(anyList())).thenReturn(List.of(
                makeShowtime(20L, mediumHorrorMovie, cgvQuan7, TODAY, LocalTime.of(20, 30), "2D PHỤ ĐỀ"),
                makeShowtime(21L, topRatedActionMovie, cgvQuan7, TODAY, LocalTime.of(20, 45), "2D PHỤ ĐỀ")
        ));

        ChatbotResponse res = chatbotService.chat(
                ChatbotRequest.builder().message("Tối nay ở Quận 7 có phim kinh dị nào chiếu sau 19h không?").build(),
                null
        );

        assertEquals(ChatbotStep.COMPLETED, res.getPendingStep());
        assertEquals(1, res.getMatchedCinemas().size());
        assertEquals("CGV Crescent Mall", res.getMatchedCinemas().get(0).getName());
        assertEquals(1, res.getRecommendations().size());
        assertEquals("Lời Nguyền Rạp Cũ", res.getRecommendations().get(0).getTitle());
    }

    @Test
    void shouldAvoidFalsePositivesOnVietnamesePronounsAndDistrictNames() {
        when(showtimeRepository.findWithDetailsByCinemaIds(anyList())).thenReturn(List.of(
                makeShowtime(30L, topRatedActionMovie, cgvHaiBaTrung, TODAY, LocalTime.of(19, 0), "2D PHỤ ĐỀ")
        ));

        ChatbotResponse res1 = chatbotService.chat(
                ChatbotRequest.builder().message("Tôi muốn xem phim chiếu rạp gần Hai Bà Trưng").build(),
                null
        );
        assertEquals(ChatbotStep.GENRE, res1.getPendingStep());
        assertEquals("Hai Bà Trưng (Hà Nội)", res1.getState().getLocation());
        assertNull(res1.getState().getGenre());
        assertNull(res1.getState().getTimeSlot());

        when(showtimeRepository.findWithDetailsByCinemaIds(anyList())).thenReturn(List.of(
                makeShowtime(31L, topRatedActionMovie, cgvHoangMai, TODAY, LocalTime.of(19, 0), "2D PHỤ ĐỀ")
        ));
        ChatbotResponse res2 = chatbotService.chat(
                ChatbotRequest.builder().message("Mình ở Hoàng Mai").build(),
                null
        );
        assertEquals(ChatbotStep.GENRE, res2.getPendingStep());
        assertNull(res2.getState().getTargetDate());
    }

    @Test
    void shouldSupportChangingGenreChangingTimeAndResettingConversation() {
        when(showtimeRepository.findWithDetailsByCinemaIds(anyList())).thenReturn(List.of(
                makeShowtime(40L, topRatedActionMovie, cgvCauGiay, TODAY, LocalTime.of(19, 0), "2D PHỤ ĐỀ"),
                makeShowtime(41L, mediumHorrorMovie, cgvCauGiay, TODAY, LocalTime.of(20, 0), "2D PHỤ ĐỀ")
        ));

        ChatbotResponse done = chatbotService.chat(
                ChatbotRequest.builder().message("Tối nay ở Cầu Giấy có phim hành động nào không?").build(),
                null
        );
        assertEquals(ChatbotStep.COMPLETED, done.getPendingStep());

        ChatbotResponse changeGenre = chatbotService.chat(
                ChatbotRequest.builder().message("Đổi thể loại").state(done.getState()).build(),
                null
        );
        assertEquals(ChatbotStep.GENRE, changeGenre.getPendingStep());

        ChatbotResponse pickedHorror = chatbotService.chat(
                ChatbotRequest.builder().message("Kinh dị").state(changeGenre.getState()).build(),
                null
        );
        assertEquals(ChatbotStep.COMPLETED, pickedHorror.getPendingStep());
        assertEquals("Lời Nguyền Rạp Cũ", pickedHorror.getRecommendations().get(0).getTitle());

        ChatbotResponse reset = chatbotService.chat(
                ChatbotRequest.builder().message("Làm mới").state(pickedHorror.getState()).build(),
                null
        );
        assertEquals(ChatbotStep.LOCATION, reset.getPendingStep());
        assertNull(reset.getState().getLocation());
        assertNull(reset.getState().getGenre());
        assertNull(reset.getState().getTimeSlot());
    }

    private CinemaSystem makeSystem(Long id, String name) {
        CinemaSystem s = new CinemaSystem(name);
        s.setId(id);
        return s;
    }

    private Cinema makeCinema(Long id, CinemaSystem system, String name) {
        Cinema c = new Cinema();
        c.setId(id);
        c.setSystem(system);
        c.setName(name);
        return c;
    }

    private Genre makeGenre(Long id, String name) {
        Genre g = new Genre(name);
        g.setId(id);
        return g;
    }

    private Movie makeMovie(Long id, String title, Double rating, Integer runtimeMinutes, String posterUrl, Set<Genre> genres) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle(title);
        m.setRating(rating);
        m.setRuntimeMinutes(runtimeMinutes);
        m.setPosterUrl(posterUrl);
        m.setGenres(genres);
        return m;
    }

    private Showtime makeShowtime(Long id, Movie movie, Cinema cinema, LocalDate showDate, LocalTime showTime, String version) {
        Showtime st = new Showtime();
        st.setId(id);
        st.setMovie(movie);
        st.setCinema(cinema);
        st.setShowDate(showDate);
        st.setShowTime(showTime);
        st.setMovieVersion(version);
        return st;
    }
}
