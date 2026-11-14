package com.example.main.service;

import com.example.main.dto.*;
import com.example.main.dto.ChatbotMovieRecommendationDto.CinemaShowtimeGroupDto;
import com.example.main.dto.ChatbotMovieRecommendationDto.ShowtimeSlotDto;
import com.example.main.entity.Cinema;
import com.example.main.entity.Genre;
import com.example.main.entity.Movie;
import com.example.main.entity.Showtime;
import com.example.main.repository.CinemaRepository;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.ShowtimeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý hội thoại tư vấn phim thông minh thông qua mô hình ngôn ngữ lớn (Gemini Flash Lite).
 * Trò chuyện tự nhiên, tư vấn phim theo sở thích và tâm trạng của khán giả,
 * tự động liên kết các thẻ phim tương tác và chỉ hỏi địa điểm khi khách hàng đã chọn được phim ưng ý.
 */
@Service
@Slf4j
public class ChatbotService {

    private static final int MAX_RECOMMENDED_MOVIES = 5;
    private static final int MAX_CINEMAS_PER_MOVIE = 3;
    private static final int MAX_DATES_PER_CINEMA = 3;
    private static final DateTimeFormatter VN_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static final String DEFAULT_GREETING =
            "Xin chào! Mình là **Trợ lý AI Tư Vấn Phim** 🎬\n" +
            "Hôm nay bạn đang muốn tìm một bộ phim như thế nào? Cứ thoải mái chia sẻ gu phim, thể loại yêu thích hoặc tâm trạng hôm nay, mình sẽ gợi ý những bộ phim đang chiếu rạp đỉnh nhất nhé! 🍿";

    public static final String BUSY_FALLBACK_MESSAGE =
            "Hệ thống AI hiện đang bận hoặc tạm nghỉ do lượng truy cập cao. Bạn vui lòng thử lại sau giây lát nhé! 🎬🍿";

    private final CinemaRepository cinemaRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ChatbotGeminiClient geminiClient;
    private final Clock clock;

    @Autowired
    public ChatbotService(
            CinemaRepository cinemaRepository,
            MovieRepository movieRepository,
            ShowtimeRepository showtimeRepository,
            ChatbotGeminiClient geminiClient
    ) {
        this(
                cinemaRepository,
                movieRepository,
                showtimeRepository,
                geminiClient,
                Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"))
        );
    }

    public ChatbotService(
            CinemaRepository cinemaRepository,
            MovieRepository movieRepository,
            ShowtimeRepository showtimeRepository,
            ChatbotGeminiClient geminiClient,
            Clock clock
    ) {
        this.cinemaRepository = cinemaRepository;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.geminiClient = geminiClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ChatbotResponse chat(ChatbotRequest request, ChatbotStateDto sessionState) {
        String conversationId = resolveConversationId(request);
        String userMessage = request != null && request.getMessage() != null ? request.getMessage().trim() : "";

        // Khi người dùng mới mở chat hoặc gửi tin nhắn rỗng
        if (userMessage.isBlank()) {
            return ChatbotResponse.builder()
                    .conversationId(conversationId)
                    .reply(DEFAULT_GREETING)
                    .state(new ChatbotStateDto())
                    .matchedCinemas(List.of())
                    .recommendations(List.of())
                    .quickReplies(List.of())
                    .aiProvider("gemini-flash-lite")
                    .build();
        }

        // Người dùng yêu cầu reset hoặc làm mới hội thoại
        if (isResetCommand(userMessage)) {
            return ChatbotResponse.builder()
                    .conversationId(UUID.randomUUID().toString())
                    .reply("Đã làm mới phiên trò chuyện!\n" + DEFAULT_GREETING)
                    .state(new ChatbotStateDto())
                    .matchedCinemas(List.of())
                    .recommendations(List.of())
                    .quickReplies(List.of())
                    .aiProvider("gemini-flash-lite")
                    .build();
        }

        List<Movie> allMovies = movieRepository.findAllWithGenres();
        List<Cinema> allCinemas = cinemaRepository.findAllWithSystem();

        List<ChatbotRequest.ChatbotHistoryItemDto> history = (request != null && request.getHistory() != null)
                ? request.getHistory()
                : List.of();
        String userLocation = (request != null && request.getUserLocation() != null && !request.getUserLocation().isBlank())
                ? request.getUserLocation().trim()
                : null;
        Optional<String> geminiReplyOpt = geminiClient.generateChatResponse(userMessage, history, allMovies, allCinemas, userLocation);

        // Trường hợp API bận, lỗi mạng hoặc lượng truy cập cao: thông báo AI đang bận mà không fallback NLP
        if (geminiReplyOpt.isEmpty()) {
            return ChatbotResponse.builder()
                    .conversationId(conversationId)
                    .reply(BUSY_FALLBACK_MESSAGE)
                    .state(sessionState != null ? sessionState : new ChatbotStateDto())
                    .matchedCinemas(List.of())
                    .recommendations(List.of())
                    .quickReplies(List.of())
                    .aiProvider("ai-busy")
                    .build();
        }

        String replyText = geminiReplyOpt.get();
        LocalDate today = LocalDate.now(clock);

        // Trích xuất các phim được nhắc đến trong câu trả lời của AI để đính kèm thẻ xem chi tiết/đặt vé
        List<ChatbotMovieRecommendationDto> recommendations = matchRecommendedMovies(replyText, allMovies, today);

        return ChatbotResponse.builder()
                .conversationId(conversationId)
                .reply(replyText)
                .state(sessionState != null ? sessionState : new ChatbotStateDto())
                .matchedCinemas(List.of())
                .recommendations(recommendations)
                .quickReplies(List.of())
                .aiProvider("gemini-flash-lite")
                .build();
    }

    private boolean isResetCommand(String msg) {
        if (msg == null) {
            return false;
        }
        String clean = msg.trim().toLowerCase(Locale.ROOT);
        return clean.equals("reset")
                || clean.equals("làm mới")
                || clean.equals("lam moi")
                || clean.equals("bắt đầu lại")
                || clean.equals("bat dau lai")
                || clean.equals("bắt đầu");
    }

    private List<ChatbotMovieRecommendationDto> matchRecommendedMovies(String replyText, List<Movie> allMovies, LocalDate today) {
        if (replyText == null || replyText.isBlank() || allMovies == null || allMovies.isEmpty()) {
            return List.of();
        }

        List<ChatbotMovieRecommendationDto> list = new ArrayList<>();
        Set<Long> addedMovieIds = new HashSet<>();

        for (Movie movie : allMovies) {
            if (movie == null || movie.getId() == null || movie.getTitle() == null) {
                continue;
            }
            if (addedMovieIds.contains(movie.getId())) {
                continue;
            }

            if (isMovieMentioned(replyText, movie)) {
                addedMovieIds.add(movie.getId());
                list.add(buildMovieRecommendation(movie, today));
                if (list.size() >= MAX_RECOMMENDED_MOVIES) {
                    break;
                }
            }
        }
        return list;
    }

    private boolean isMovieMentioned(String text, Movie movie) {
        String fullTitle = movie.getTitle().trim();
        if (containsIgnoreCase(text, fullTitle)) {
            return true;
        }
        // Với phim có tiêu đề kèm phụ đề (VD "A: B" hoặc "A - B")
        if (fullTitle.contains(":") || fullTitle.contains("-")) {
            String[] segments = fullTitle.split("[:\\-]");
            for (String seg : segments) {
                String clean = seg.trim();
                if (clean.length() >= 5 && containsIgnoreCase(text, clean)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null || target.isEmpty()) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(target.toLowerCase(Locale.ROOT));
    }

    private ChatbotMovieRecommendationDto buildMovieRecommendation(Movie movie, LocalDate today) {
        List<String> genres = (movie.getGenres() != null)
                ? movie.getGenres().stream().map(Genre::getName).toList()
                : List.of();

        Double rating = movie.getRating() != null ? movie.getRating() : movie.getDefaultRating();

        return ChatbotMovieRecommendationDto.builder()
                .movieId(movie.getId())
                .title(movie.getTitle())
                .rating(rating)
                .posterUrl(movie.getPosterUrl())
                .runtimeMinutes(movie.getRuntimeMinutes())
                .genres(genres)
                .cinemaShowtimes(List.of())
                .build();
    }

    private List<CinemaShowtimeGroupDto> groupShowtimesByCinema(List<Showtime> showtimes, LocalDate today) {
        if (showtimes == null || showtimes.isEmpty()) {
            return List.of();
        }

        Map<Cinema, List<Showtime>> byCinema = showtimes.stream()
                .filter(s -> s.getCinema() != null && s.getShowDate() != null && !s.getShowDate().isBefore(today))
                .collect(Collectors.groupingBy(Showtime::getCinema, LinkedHashMap::new, Collectors.toList()));

        List<CinemaShowtimeGroupDto> result = new ArrayList<>();
        for (Map.Entry<Cinema, List<Showtime>> entry : byCinema.entrySet()) {
            if (result.size() >= MAX_CINEMAS_PER_MOVIE) {
                break;
            }
            Cinema cinema = entry.getKey();
            List<Showtime> cinemaShowtimes = entry.getValue();

            Map<LocalDate, List<String>> byDate = cinemaShowtimes.stream()
                    .collect(Collectors.groupingBy(
                            Showtime::getShowDate,
                            TreeMap::new,
                            Collectors.mapping(s -> s.getShowTime() != null ? s.getShowTime().format(TIME_FMT) : "", Collectors.toList())
                    ));

            List<ShowtimeSlotDto> slots = new ArrayList<>();
            for (Map.Entry<LocalDate, List<String>> dateEntry : byDate.entrySet()) {
                if (slots.size() >= MAX_DATES_PER_CINEMA) {
                    break;
                }
                LocalDate date = dateEntry.getKey();
                List<String> times = dateEntry.getValue().stream().filter(t -> !t.isBlank()).sorted().distinct().toList();
                String dateLabel = date.equals(today) ? "Hôm nay (" + date.format(VN_DATE_FMT) + ")"
                        : (date.equals(today.plusDays(1)) ? "Ngày mai (" + date.format(VN_DATE_FMT) + ")" : date.format(VN_DATE_FMT));

                slots.add(ShowtimeSlotDto.builder()
                        .date(date)
                        .dateLabel(dateLabel)
                        .times(times)
                        .build());
            }

            String sysName = (cinema.getSystem() != null) ? cinema.getSystem().getName() : "Rạp";
            result.add(CinemaShowtimeGroupDto.builder()
                    .cinemaId(cinema.getId())
                    .cinemaName(cinema.getName())
                    .systemName(sysName)
                    .schedules(slots)
                    .build());
        }
        return result;
    }

    private String resolveConversationId(ChatbotRequest request) {
        if (request != null && request.getConversationId() != null && !request.getConversationId().isBlank()) {
            return request.getConversationId().trim();
        }
        return UUID.randomUUID().toString();
    }
}
