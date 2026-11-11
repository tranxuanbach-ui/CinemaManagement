package com.example.main.service;

import com.example.main.dto.*;
import com.example.main.dto.ChatbotMovieRecommendationDto.CinemaShowtimeGroupDto;
import com.example.main.dto.ChatbotMovieRecommendationDto.ShowtimeSlotDto;
import com.example.main.entity.Cinema;
import com.example.main.entity.Genre;
import com.example.main.entity.Movie;
import com.example.main.entity.Showtime;
import com.example.main.enums.ChatbotStep;
import com.example.main.repository.CinemaRepository;
import com.example.main.repository.GenreRepository;
import com.example.main.repository.ShowtimeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatbotService {

    private static final int MAX_CINEMAS_DISPLAY = 6;
    private static final int MAX_RECOMMENDED_MOVIES = 5;
    private static final int MAX_CINEMAS_PER_MOVIE = 3;
    private static final int MAX_DATES_PER_CINEMA = 3;
    private static final DateTimeFormatter VN_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final CinemaRepository cinemaRepository;
    private final GenreRepository genreRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ChatbotLocationMatcher locationMatcher;
    private final ChatbotQueryExtractor queryExtractor;
    private final Clock clock;

    @Autowired
    public ChatbotService(
            CinemaRepository cinemaRepository,
            GenreRepository genreRepository,
            ShowtimeRepository showtimeRepository,
            ChatbotLocationMatcher locationMatcher,
            ChatbotQueryExtractor queryExtractor
    ) {
        this(
                cinemaRepository,
                genreRepository,
                showtimeRepository,
                locationMatcher,
                queryExtractor,
                Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"))
        );
    }

    public ChatbotService(
            CinemaRepository cinemaRepository,
            GenreRepository genreRepository,
            ShowtimeRepository showtimeRepository,
            ChatbotLocationMatcher locationMatcher,
            ChatbotQueryExtractor queryExtractor,
            Clock clock
    ) {
        this.cinemaRepository = cinemaRepository;
        this.genreRepository = genreRepository;
        this.showtimeRepository = showtimeRepository;
        this.locationMatcher = locationMatcher;
        this.queryExtractor = queryExtractor;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ChatbotResponse chat(ChatbotRequest request, ChatbotStateDto sessionState) {
        String conversationId = resolveConversationId(request);
        ChatbotStateDto state = resolveInitialState(request, sessionState);
        String userMessage = request != null && request.getMessage() != null ? request.getMessage().trim() : "";

        LocalDate today = LocalDate.now(clock);
        List<Cinema> allCinemas = cinemaRepository.findAllWithSystem();
        List<Genre> allGenres = genreRepository.findAll();

        if (userMessage.isBlank()) {
            return buildAskLocationResponse(
                    conversationId,
                    new ChatbotStateDto(),
                    "Xin chào! Mình là **Trợ lý AI Tư Vấn Phim**.\nBạn đang ở **khu vực / quận / thành phố** nào để mình tìm rạp gần nhất cho bạn ạ?",
                    "local-nlp"
            );
        }

        ChatbotQueryExtractor.ExtractedSlots extracted =
                queryExtractor.extract(userMessage, state, allCinemas, allGenres, today);
        String aiProvider = extracted.usedGemini() ? "gemini+local-nlp" : "local-nlp";

        if (extracted.resetRequested()) {
            return buildAskLocationResponse(
                    conversationId,
                    new ChatbotStateDto(),
                    "Mình đã làm mới cuộc trò chuyện! Bạn muốn tìm rạp chiếu phim ở **khu vực / quận / thành phố** nào ạ?",
                    aiProvider
            );
        }

        // Lưu tạm các tiêu chí thể loại / thời gian nếu user đã nói ngay từ câu đầu trước khi có địa điểm
        applyGenreAndTimeUpdates(state, extracted);

        // Nếu user vừa yêu cầu "đổi địa điểm" nhưng chưa nói tên địa điểm mới
        if (extracted.locationExplicitChange() && extracted.location() == null) {
            state.setLocation(null);
            state.setMatchedCinemaIds(new ArrayList<>());
            state.setMatchedCinemaNames(new ArrayList<>());
            state.setPendingStep(ChatbotStep.LOCATION);
            return buildAskLocationResponse(
                    conversationId,
                    state,
                    "Bạn muốn đổi sang **khu vực / quận / thành phố** nào ạ? (Ví dụ: Cầu Giấy, Đống Đa, Quận 1, Quận 7, Đà Nẵng...)",
                    aiProvider
            );
        }

        // BƯỚC 1: KIỂM TRA & TRUY VẤN ĐỊA ĐIỂM -> RẠP GẦN ĐÓ
        List<Cinema> matchedCinemas = resolveMatchedCinemas(state, extracted, allCinemas);
        if (extracted.location() != null && matchedCinemas.isEmpty()) {
            // Người dùng có nhập tên khu vực nhưng DB chưa có rạp khớp
            ChatbotStateDto askState = state.copy();
            askState.setLocation(null);
            askState.setMatchedCinemaIds(new ArrayList<>());
            askState.setMatchedCinemaNames(new ArrayList<>());
            askState.setPendingStep(ChatbotStep.LOCATION);
            String notFoundReply = "Hiện tại hệ thống chưa tìm thấy cụm rạp nào thuộc khu vực **" + extracted.location() + "**.\n"
                    + "Bạn thử nhập tên **quận/huyện hoặc thành phố lớn** gần đó giúp mình nhé (VD: Hà Nội, Cầu Giấy, Đống Đa, TP.HCM, Quận 1, Quận 7, Đà Nẵng, Hải Phòng)!";
            return buildAskLocationResponse(conversationId, askState, notFoundReply, aiProvider);
        }

        if (state.getLocation() == null || matchedCinemas.isEmpty()) {
            state.setPendingStep(ChatbotStep.LOCATION);
            StringBuilder msg = new StringBuilder();
            if (state.getGenre() != null || hasTimeConstraint(state)) {
                msg.append("Mình đã ghi nhận tiêu chí của bạn");
                if (state.getGenre() != null) {
                    msg.append(" (thể loại **").append(state.getGenre()).append("**");
                    if (hasTimeConstraint(state)) {
                        msg.append(", ").append(state.getTimeLabel());
                    }
                    msg.append(")");
                } else if (hasTimeConstraint(state)) {
                    msg.append(" (").append(state.getTimeLabel()).append(")");
                }
                msg.append(".\n");
            }
            msg.append("Để tìm đúng rạp thuận tiện nhất, bạn đang ở **khu vực / quận / thành phố** nào ạ?");
            return buildAskLocationResponse(conversationId, state, msg.toString(), aiProvider);
        }

        // Truy vấn suất chiếu thuộc các rạp gần đó trong DB
        List<Showtime> cinemaShowtimes = fetchActiveShowtimes(state.getMatchedCinemaIds(), today);
        if (cinemaShowtimes.isEmpty()) {
            state.setPendingStep(ChatbotStep.LOCATION);
            return ChatbotResponse.builder()
                    .conversationId(conversationId)
                    .pendingStep(ChatbotStep.LOCATION)
                    .state(state)
                    .matchedCinemas(toCinemaDtos(matchedCinemas))
                    .recommendations(List.of())
                    .quickReplies(List.of("Hà Nội", "Cầu Giấy", "TP.HCM", "Quận 1", "Đà Nẵng", "Làm mới"))
                    .aiProvider(aiProvider)
                    .reply("Mình tìm thấy " + matchedCinemas.size() + " rạp gần **" + state.getLocation() + "** ("
                            + formatCinemaListInline(matchedCinemas)
                            + "), nhưng hiện các rạp này chưa có lịch chiếu khả dụng trong dữ liệu.\nBạn muốn thử khu vực khác không?")
                    .build();
        }

        // BƯỚC 2: KIỂM TRA THỂ LOẠI PHIM TẠI CÁC RẠP ĐÃ TRUY VẤN
        if (state.getGenre() == null && !state.isGenreSkipped()) {
            // Nếu ở bước trước user đòi "rating cao" ngay câu có địa điểm -> coi như bỏ qua lọc thể loại và lấy top rating luôn
            if (extracted.explicitHighRating() && extracted.genre() == null) {
                state.setGenreSkipped(true);
            } else {
                return buildAskGenreResponse(conversationId, state, matchedCinemas, cinemaShowtimes, aiProvider);
            }
        }

        // Lọc phim tại các rạp đã truy vấn theo thể loại (hoặc lấy toàn bộ nếu "không cần" để xếp hạng rating cao)
        List<Showtime> genreFilteredShowtimes = filterByGenreIfNeeded(cinemaShowtimes, state);
        if (genreFilteredShowtimes.isEmpty() && state.getGenre() != null) {
            String failedGenre = state.getGenre();
            state.setGenre(null);
            state.setGenreSkipped(false);
            state.setPendingStep(ChatbotStep.GENRE);
            List<String> availableGenres = extractAvailableGenres(cinemaShowtimes);
            List<String> quickReplies = new ArrayList<>();
            quickReplies.add("Không cần");
            availableGenres.stream().limit(5).forEach(quickReplies::add);
            quickReplies.add("Đổi địa điểm");

            return ChatbotResponse.builder()
                    .conversationId(conversationId)
                    .pendingStep(ChatbotStep.GENRE)
                    .state(state)
                    .matchedCinemas(toCinemaDtos(matchedCinemas))
                    .recommendations(List.of())
                    .quickReplies(quickReplies)
                    .aiProvider(aiProvider)
                    .reply("Tại các rạp gần **" + state.getLocation() + "** hiện chưa có suất chiếu nào thuộc thể loại **" + failedGenre + "**.\n"
                            + "Các thể loại đang chiếu tại đây gồm: " + String.join(", ", availableGenres) + ".\n"
                            + "Bạn muốn chọn thể loại khác hay nhắn **không cần** để mình gợi ý những phim **rating cao nhất**?")
                    .build();
        }

        List<Movie> rankedMovies = rankMoviesByRating(genreFilteredShowtimes);

        // BƯỚC 3: KIỂM TRA KHUNG THỜI GIAN CHIẾU
        if (!state.isTimeSkipped() && !hasTimeConstraint(state)) {
            return buildAskTimeResponse(
                    conversationId, state, matchedCinemas, rankedMovies, genreFilteredShowtimes, aiProvider
            );
        }

        // Hoàn thành: lọc hoặc đề xuất khung giờ lý tưởng cho người bận rộn
        return buildFinalRecommendationsResponse(
                conversationId, state, matchedCinemas, rankedMovies, genreFilteredShowtimes, today, aiProvider
        );
    }

    private ChatbotStateDto resolveInitialState(ChatbotRequest request, ChatbotStateDto sessionState) {
        if (request != null && request.getState() != null) {
            return request.getState().copy();
        }
        if (sessionState != null) {
            return sessionState.copy();
        }
        return new ChatbotStateDto();
    }

    private String resolveConversationId(ChatbotRequest request) {
        if (request != null && request.getConversationId() != null && !request.getConversationId().isBlank()) {
            return request.getConversationId();
        }
        return UUID.randomUUID().toString();
    }

    private void applyGenreAndTimeUpdates(ChatbotStateDto state, ChatbotQueryExtractor.ExtractedSlots extracted) {
        ChatbotStep currentStep = state.getPendingStep() != null ? state.getPendingStep() : ChatbotStep.LOCATION;

        // Cập nhật thể loại nếu user chỉ định thể loại mới hoặc yêu cầu đổi thể loại
        if (extracted.genre() != null) {
            state.setGenre(extracted.genre());
            state.setGenreSkipped(false);
        } else if (extracted.genreChangeRequested()) {
            state.setGenre(null);
            state.setGenreSkipped(false);
        } else if (extracted.explicitHighRating() && extracted.genre() == null && currentStep != ChatbotStep.TIME) {
            state.setGenre(null);
            state.setGenreSkipped(true);
        } else if (extracted.skipPreference() && currentStep == ChatbotStep.GENRE) {
            state.setGenre(null);
            state.setGenreSkipped(true);
        }

        // Cập nhật thời gian nếu user chỉ định khung giờ mới hoặc yêu cầu đổi khung giờ
        ChatbotQueryExtractor.TimeExtraction time = extracted.time();
        if (time != null && time.hasAnyTimeConstraint()) {
            state.setTimeSlot(time.timeSlot());
            state.setTimeLabel(time.timeLabel());
            state.setStartTime(time.startTime());
            state.setEndTime(time.endTime());
            state.setTargetDate(time.targetDate());
            state.setWeekendOnly(time.weekendOnly());
            state.setTimeSkipped(false);
        } else if (extracted.timeChangeRequested()) {
            state.setTimeSlot(null);
            state.setTimeLabel(null);
            state.setStartTime(null);
            state.setEndTime(null);
            state.setTargetDate(null);
            state.setWeekendOnly(false);
            state.setTimeSkipped(false);
        } else if ((extracted.skipPreference() && currentStep == ChatbotStep.TIME)
                || extracted.explicitBusyIdealTime()) {
            state.setTimeSlot("IDEAL_BUSY");
            state.setTimeLabel("Khung giờ lý tưởng nhất cho người bận rộn");
            state.setStartTime(null);
            state.setEndTime(null);
            state.setTargetDate(null);
            state.setWeekendOnly(false);
            state.setTimeSkipped(true);
        }
    }

    private List<Cinema> resolveMatchedCinemas(
            ChatbotStateDto state,
            ChatbotQueryExtractor.ExtractedSlots extracted,
            List<Cinema> allCinemas
    ) {
        if (extracted.location() != null) {
            List<Cinema> matched = locationMatcher.findMatchingCinemas(extracted.location(), allCinemas);
            if (!matched.isEmpty()) {
                state.setLocation(extracted.location());
                state.setMatchedCinemaIds(matched.stream().map(Cinema::getId).toList());
                state.setMatchedCinemaNames(matched.stream().map(Cinema::getName).toList());
            }
            return matched;
        }

        if (state.getMatchedCinemaIds() != null && !state.getMatchedCinemaIds().isEmpty()) {
            Set<Long> ids = new HashSet<>(state.getMatchedCinemaIds());
            return allCinemas.stream().filter(c -> ids.contains(c.getId())).toList();
        }

        if (state.getLocation() != null && !state.getLocation().isBlank()) {
            List<Cinema> matched = locationMatcher.findMatchingCinemas(state.getLocation(), allCinemas);
            state.setMatchedCinemaIds(matched.stream().map(Cinema::getId).toList());
            state.setMatchedCinemaNames(matched.stream().map(Cinema::getName).toList());
            return matched;
        }

        return List.of();
    }

    private List<Showtime> fetchActiveShowtimes(List<Long> cinemaIds, LocalDate today) {
        if (cinemaIds == null || cinemaIds.isEmpty()) {
            return List.of();
        }
        List<Showtime> all = showtimeRepository.findWithDetailsByCinemaIds(cinemaIds);
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        List<Showtime> upcoming = all.stream()
                .filter(s -> s.getShowDate() != null && !s.getShowDate().isBefore(today))
                .toList();
        return upcoming.isEmpty() ? all : upcoming;
    }

    private ChatbotResponse buildAskLocationResponse(
            String conversationId,
            ChatbotStateDto state,
            String message,
            String aiProvider
    ) {
        state.setPendingStep(ChatbotStep.LOCATION);
        return ChatbotResponse.builder()
                .conversationId(conversationId)
                .pendingStep(ChatbotStep.LOCATION)
                .state(state)
                .matchedCinemas(List.of())
                .recommendations(List.of())
                .quickReplies(List.of("Cầu Giấy", "Đống Đa", "Hà Nội", "Quận 1", "Quận 7", "TP.HCM", "Đà Nẵng", "Hải Phòng"))
                .aiProvider(aiProvider)
                .reply(message)
                .build();
    }

    private ChatbotResponse buildAskGenreResponse(
            String conversationId,
            ChatbotStateDto state,
            List<Cinema> matchedCinemas,
            List<Showtime> cinemaShowtimes,
            String aiProvider
    ) {
        state.setPendingStep(ChatbotStep.GENRE);
        List<String> availableGenres = extractAvailableGenres(cinemaShowtimes);

        List<String> quickReplies = new ArrayList<>();
        quickReplies.add("Không cần");
        availableGenres.stream().limit(6).forEach(quickReplies::add);

        StringBuilder reply = new StringBuilder();
        reply.append("Mình đã tìm thấy **").append(matchedCinemas.size()).append(" rạp gần khu vực ")
                .append(state.getLocation()).append("**:\n")
                .append(formatCinemaBullets(matchedCinemas)).append("\n");

        if (hasTimeConstraint(state)) {
            reply.append("_(Khung giờ đã ghi nhận: ").append(state.getTimeLabel()).append(")_\n\n");
        } else {
            reply.append("\n");
        }

        reply.append("Bạn muốn xem **thể loại phim** nào ạ?");
        if (!availableGenres.isEmpty()) {
            reply.append(" (Đang có: ").append(String.join(", ", availableGenres.stream().limit(6).toList())).append(")");
        }
        reply.append("\nNếu **không cần** chọn thể loại, hãy nhắn `không cần` — mình sẽ lọc những phim có **rating cao nhất** tại các rạp trên cho bạn!");

        return ChatbotResponse.builder()
                .conversationId(conversationId)
                .pendingStep(ChatbotStep.GENRE)
                .state(state)
                .matchedCinemas(toCinemaDtos(matchedCinemas))
                .recommendations(List.of())
                .quickReplies(quickReplies)
                .aiProvider(aiProvider)
                .reply(reply.toString())
                .build();
    }

    private ChatbotResponse buildAskTimeResponse(
            String conversationId,
            ChatbotStateDto state,
            List<Cinema> matchedCinemas,
            List<Movie> rankedMovies,
            List<Showtime> genreFilteredShowtimes,
            String aiProvider
    ) {
        state.setPendingStep(ChatbotStep.TIME);

        StringBuilder reply = new StringBuilder();
        if (state.isGenreSkipped()) {
            reply.append("Bạn chọn không giới hạn thể loại — mình đã lọc **top phim có rating cao nhất** tại các rạp gần **")
                    .append(state.getLocation()).append("**:\n");
        } else {
            reply.append("Mình tìm thấy **").append(rankedMovies.size()).append(" phim thể loại ")
                    .append(state.getGenre()).append("** đang chiếu gần **").append(state.getLocation()).append("**:\n");
        }

        int previewCount = Math.min(4, rankedMovies.size());
        for (int i = 0; i < previewCount; i++) {
            Movie m = rankedMovies.get(i);
            reply.append("• **").append(m.getTitle()).append("** (⭐ ")
                    .append(formatRating(effectiveRating(m))).append("/10)");
            if (m.getGenres() != null && !m.getGenres().isEmpty()) {
                String genreNames = m.getGenres().stream().map(Genre::getName).sorted().collect(Collectors.joining(", "));
                reply.append(" — ").append(genreNames);
            }
            reply.append("\n");
        }
        if (rankedMovies.size() > previewCount) {
            reply.append("• _...và ").append(rankedMovies.size() - previewCount).append(" phim khác._\n");
        }

        reply.append("\nBạn muốn đi xem vào **khung giờ chiếu nào** (VD: buổi tối, sau 19h, trưa, sáng, cuối tuần)?\n");
        reply.append("Nếu **không cần** chọn giờ cố định, hãy nhắn `không cần` — mình sẽ đề xuất **các khung giờ lý tưởng nhất cho người bận rộn**!");

        // Kiểm tra các buổi nào thực sự có suất chiếu để gợi ý chip sát thực tế
        List<String> quickReplies = new ArrayList<>();
        quickReplies.add("Không cần");
        quickReplies.add("Buổi tối (18h-22h)");
        quickReplies.add("Sau 19h");
        quickReplies.add("Buổi chiều (14h-18h)");
        quickReplies.add("Buổi trưa (11h30-14h)");
        quickReplies.add("Cuối tuần");

        return ChatbotResponse.builder()
                .conversationId(conversationId)
                .pendingStep(ChatbotStep.TIME)
                .state(state)
                .matchedCinemas(toCinemaDtos(matchedCinemas))
                .recommendations(List.of())
                .quickReplies(quickReplies)
                .usedHighRatingFallback(state.isGenreSkipped())
                .aiProvider(aiProvider)
                .reply(reply.toString())
                .build();
    }

    private ChatbotResponse buildFinalRecommendationsResponse(
            String conversationId,
            ChatbotStateDto state,
            List<Cinema> matchedCinemas,
            List<Movie> rankedMovies,
            List<Showtime> genreFilteredShowtimes,
            LocalDate today,
            String aiProvider
    ) {
        if (state.isTimeSkipped()) {
            return buildIdealBusyScheduleResponse(
                    conversationId, state, matchedCinemas, rankedMovies, genreFilteredShowtimes, today, aiProvider
            );
        }

        List<Showtime> timeFiltered = filterShowtimesByTime(genreFilteredShowtimes, state);

        // Nếu không khớp đúng giờ/ngày đã yêu cầu -> thông báo minh bạch & gợi ý các lựa chọn thay thế gần nhất
        if (timeFiltered.isEmpty()) {
            String requestedTimeDesc = state.getTimeLabel() != null ? state.getTimeLabel() : "khung giờ bạn yêu cầu";
            String availableSummary = summarizeAvailableTimePeriods(genreFilteredShowtimes);
            state.setTimeSlot(null);
            state.setTimeLabel(null);
            state.setStartTime(null);
            state.setEndTime(null);
            state.setTargetDate(null);
            state.setWeekendOnly(false);
            state.setTimeSkipped(false);
            state.setPendingStep(ChatbotStep.TIME);

            return ChatbotResponse.builder()
                    .conversationId(conversationId)
                    .pendingStep(ChatbotStep.TIME)
                    .state(state)
                    .matchedCinemas(toCinemaDtos(matchedCinemas))
                    .recommendations(List.of())
                    .usedHighRatingFallback(state.isGenreSkipped())
                    .quickReplies(List.of("Không cần", "Buổi tối (18h-22h)", "Buổi chiều (14h-18h)", "Buổi sáng (8h-12h)", "Đổi thể loại", "Làm mới"))
                    .aiProvider(aiProvider)
                    .reply("Hiện tại chưa có suất chiếu phù hợp với **" + requestedTimeDesc + "** cho các phim đã lọc tại khu vực **"
                            + state.getLocation() + "**.\n"
                            + "Các suất hiện có rơi vào: " + availableSummary + ".\n"
                            + "Bạn hãy chọn khung giờ khác hoặc nhắn `không cần` để mình gợi ý **khung giờ lý tưởng cho người bận rộn** nhé!")
                    .build();
        }

        List<Movie> finalMovies = rankMoviesByRating(timeFiltered);
        List<ChatbotMovieRecommendationDto> recs = buildRecommendationDtos(
                finalMovies, timeFiltered, false, today
        );

        state.setPendingStep(ChatbotStep.COMPLETED);
        StringBuilder reply = new StringBuilder();
        reply.append("Dưới đây là gợi ý phù hợp nhất dành cho bạn tại khu vực **").append(state.getLocation()).append("**");
        if (state.isGenreSkipped()) {
            reply.append(" | **Top phim rating cao**");
        } else {
            reply.append(" | Thể loại **").append(state.getGenre()).append("**");
        }
        reply.append(" | **").append(state.getTimeLabel()).append("**:\n\n");

        appendRecommendationSummaryText(reply, recs, false);
        reply.append("\nBạn có thể bấm trực tiếp vào thẻ phim bên dưới để xem chi tiết, hoặc nhắn để đổi **thể loại / khung giờ / địa điểm** nhé!");

        return ChatbotResponse.builder()
                .conversationId(conversationId)
                .pendingStep(ChatbotStep.COMPLETED)
                .state(state)
                .matchedCinemas(toCinemaDtos(matchedCinemas))
                .recommendations(recs)
                .quickReplies(List.of("Đổi khung giờ", "Đổi thể loại", "Đổi địa điểm", "Làm mới"))
                .usedHighRatingFallback(state.isGenreSkipped())
                .usedIdealBusySchedule(false)
                .aiProvider(aiProvider)
                .reply(reply.toString())
                .build();
    }

    private ChatbotResponse buildIdealBusyScheduleResponse(
            String conversationId,
            ChatbotStateDto state,
            List<Cinema> matchedCinemas,
            List<Movie> rankedMovies,
            List<Showtime> genreFilteredShowtimes,
            LocalDate today,
            String aiProvider
    ) {
        // Sắp xếp suất chiếu ưu tiên khung giờ vàng cho người bận rộn, giữ tối đa đa dạng suất đẹp
        List<Showtime> prioritized = genreFilteredShowtimes.stream()
                .sorted(Comparator
                        .comparingInt(this::busyPersonPriorityTier)
                        .thenComparing(Showtime::getShowDate)
                        .thenComparing(Showtime::getShowTime))
                .toList();

        List<ChatbotMovieRecommendationDto> recs = buildRecommendationDtos(
                rankedMovies, prioritized, true, today
        );

        state.setPendingStep(ChatbotStep.COMPLETED);
        StringBuilder reply = new StringBuilder();
        reply.append("Với lịch trình bận rộn tại khu vực **").append(state.getLocation()).append("**");
        if (state.isGenreSkipped()) {
            reply.append(" (ưu tiên **phim rating cao nhất**)");
        } else {
            reply.append(" (thể loại **").append(state.getGenre()).append("**)");
        }
        reply.append(", mình đề xuất **những khung giờ lý tưởng nhất**:\n");
        reply.append("1. **Sau giờ làm (18:30 – 21:30):** Tan làm kịp di chuyển, ăn nhẹ và về nghỉ ngơi hợp lý.\n");
        reply.append("2. **Tối muộn (21:31 – 22:45):** Phù hợp khi tăng ca hoặc muốn tránh giờ tan tầm.\n");
        reply.append("3. **Nghỉ trưa nhanh (11:45 – 13:30) / Cuối tuần (09:30 – 17:30):** Linh hoạt không vướng giờ hành chính.\n\n");

        appendRecommendationSummaryText(reply, recs, true);
        reply.append("\nBạn muốn đặt vé khung giờ nào hoặc đổi tiêu chí khác cứ nhắn cho mình nhé!");

        return ChatbotResponse.builder()
                .conversationId(conversationId)
                .pendingStep(ChatbotStep.COMPLETED)
                .state(state)
                .matchedCinemas(toCinemaDtos(matchedCinemas))
                .recommendations(recs)
                .quickReplies(List.of("Buổi tối (18h-22h)", "Cuối tuần", "Đổi thể loại", "Đổi địa điểm", "Làm mới"))
                .usedHighRatingFallback(state.isGenreSkipped())
                .usedIdealBusySchedule(true)
                .aiProvider(aiProvider)
                .reply(reply.toString())
                .build();
    }

    /**
     * Xếp hạng ưu tiên suất chiếu cho người bận rộn:
     * 1: Tối sau giờ làm (18:30 - 21:30)
     * 2: Tối muộn / tăng ca (21:31 - 22:45)
     * 3: Giờ nghỉ trưa (11:45 - 13:30)
     * 4: Ban ngày cuối tuần (T7/CN 09:30 - 17:30)
     * 5: Các suất còn lại
     */
    int busyPersonPriorityTier(Showtime s) {
        if (s == null || s.getShowTime() == null || s.getShowDate() == null) {
            return 5;
        }
        LocalTime t = s.getShowTime();
        DayOfWeek dow = s.getShowDate().getDayOfWeek();
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

        if (!t.isBefore(LocalTime.of(18, 30)) && !t.isAfter(LocalTime.of(21, 30))) {
            return 1;
        }
        if (t.isAfter(LocalTime.of(21, 30)) && !t.isAfter(LocalTime.of(22, 45))) {
            return 2;
        }
        if (!t.isBefore(LocalTime.of(11, 45)) && !t.isAfter(LocalTime.of(13, 30))) {
            return 3;
        }
        if (isWeekend && !t.isBefore(LocalTime.of(9, 30)) && !t.isAfter(LocalTime.of(17, 30))) {
            return 4;
        }
        return 5;
    }

    private String busyBadgeForShowtime(Showtime s) {
        return switch (busyPersonPriorityTier(s)) {
            case 1 -> "Khung Vàng Sau Giờ Làm (18:30–21:30)";
            case 2 -> "Tối Muộn / Tăng Ca (21:31–22:45)";
            case 3 -> "Nghỉ Trưa Nhanh (11:45–13:30)";
            case 4 -> "Cuối Tuần Thảnh Thơi";
            default -> "Suất Khả Dụng";
        };
    }

    private List<Showtime> filterByGenreIfNeeded(List<Showtime> showtimes, ChatbotStateDto state) {
        if (state.isGenreSkipped() || state.getGenre() == null || state.getGenre().isBlank()) {
            return showtimes;
        }
        String normTargetGenre = ChatbotTextNormalizer.normalize(state.getGenre());
        return showtimes.stream()
                .filter(s -> s.getMovie() != null && s.getMovie().getGenres() != null)
                .filter(s -> s.getMovie().getGenres().stream()
                        .anyMatch(g -> g.getName() != null
                                && ChatbotTextNormalizer.normalize(g.getName()).equals(normTargetGenre)))
                .toList();
    }

    private List<Showtime> filterShowtimesByTime(List<Showtime> showtimes, ChatbotStateDto state) {
        return showtimes.stream()
                .filter(s -> {
                    if (state.getTargetDate() != null && !state.getTargetDate().equals(s.getShowDate())) {
                        return false;
                    }
                    if (state.isWeekendOnly() && s.getShowDate() != null) {
                        DayOfWeek dow = s.getShowDate().getDayOfWeek();
                        if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                            return false;
                        }
                    }
                    if (state.getStartTime() != null && s.getShowTime() != null
                            && s.getShowTime().isBefore(state.getStartTime())) {
                        return false;
                    }
                    if (state.getEndTime() != null && s.getShowTime() != null
                            && s.getShowTime().isAfter(state.getEndTime())) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private List<Movie> rankMoviesByRating(List<Showtime> showtimes) {
        Map<Long, Movie> uniqueMovies = new LinkedHashMap<>();
        Map<Long, Integer> showtimeCounts = new HashMap<>();

        for (Showtime s : showtimes) {
            if (s.getMovie() == null || s.getMovie().getId() == null) {
                continue;
            }
            Long id = s.getMovie().getId();
            uniqueMovies.putIfAbsent(id, s.getMovie());
            showtimeCounts.merge(id, 1, Integer::sum);
        }

        return uniqueMovies.values().stream()
                .sorted(Comparator
                        .comparingDouble(this::effectiveRating).reversed()
                        .thenComparing(Comparator.comparingInt((Movie m) -> showtimeCounts.getOrDefault(m.getId(), 0)).reversed())
                        .thenComparing(Movie::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private List<ChatbotMovieRecommendationDto> buildRecommendationDtos(
            List<Movie> orderedMovies,
            List<Showtime> candidateShowtimes,
            boolean idealBusyMode,
            LocalDate today
    ) {
        Map<Long, List<Showtime>> byMovie = candidateShowtimes.stream()
                .filter(s -> s.getMovie() != null && s.getMovie().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getMovie().getId(), LinkedHashMap::new, Collectors.toList()));

        List<ChatbotMovieRecommendationDto> result = new ArrayList<>();
        for (Movie movie : orderedMovies) {
            if (result.size() >= MAX_RECOMMENDED_MOVIES) {
                break;
            }
            List<Showtime> movieShowtimes = byMovie.getOrDefault(movie.getId(), List.of());
            if (movieShowtimes.isEmpty()) {
                continue;
            }

            // Nếu là chế độ người bận rộn và có ít nhất một suất thuộc tier 1..4 -> lọc giữ lại tối đa suất tier 1..4
            List<Showtime> selectedShowtimes = movieShowtimes;
            if (idealBusyMode) {
                List<Showtime> busyFriendly = movieShowtimes.stream()
                        .filter(s -> busyPersonPriorityTier(s) <= 4)
                        .toList();
                if (!busyFriendly.isEmpty()) {
                    selectedShowtimes = busyFriendly;
                }
            }

            List<CinemaShowtimeGroupDto> cinemaGroups = groupShowtimesByCinema(selectedShowtimes, idealBusyMode, today);
            if (cinemaGroups.isEmpty()) {
                continue;
            }

            List<String> genres = movie.getGenres() != null
                    ? movie.getGenres().stream().map(Genre::getName).sorted().toList()
                    : List.of();

            result.add(ChatbotMovieRecommendationDto.builder()
                    .movieId(movie.getId())
                    .title(movie.getTitle())
                    .rating(effectiveRating(movie))
                    .posterUrl(movie.getPosterUrl())
                    .runtimeMinutes(movie.getRuntimeMinutes())
                    .genres(genres)
                    .cinemaShowtimes(cinemaGroups)
                    .build());
        }
        return result;
    }

    private List<CinemaShowtimeGroupDto> groupShowtimesByCinema(
            List<Showtime> movieShowtimes,
            boolean idealBusyMode,
            LocalDate today
    ) {
        Map<Long, List<Showtime>> byCinema = new LinkedHashMap<>();
        for (Showtime s : movieShowtimes) {
            if (s.getCinema() != null && s.getCinema().getId() != null) {
                byCinema.computeIfAbsent(s.getCinema().getId(), k -> new ArrayList<>()).add(s);
            }
        }

        // Sắp xếp cụm rạp ưu tiên rạp có suất tier bận rộn tốt hơn hoặc có nhiều suất gần nhất
        List<Map.Entry<Long, List<Showtime>>> sortedCinemas = byCinema.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<Long, List<Showtime>> e) -> idealBusyMode
                                ? e.getValue().stream().mapToInt(this::busyPersonPriorityTier).min().orElse(5)
                                : 1)
                        .thenComparing(Comparator.comparingInt((Map.Entry<Long, List<Showtime>> e) -> e.getValue().size()).reversed()))
                .limit(MAX_CINEMAS_PER_MOVIE)
                .toList();

        List<CinemaShowtimeGroupDto> groups = new ArrayList<>();
        for (Map.Entry<Long, List<Showtime>> entry : sortedCinemas) {
            List<Showtime> cShowtimes = entry.getValue();
            Cinema cinema = cShowtimes.get(0).getCinema();

            Map<LocalDate, List<Showtime>> byDate = cShowtimes.stream()
                    .filter(s -> s.getShowDate() != null)
                    .collect(Collectors.groupingBy(Showtime::getShowDate, TreeMap::new, Collectors.toList()));

            List<ShowtimeSlotDto> schedules = new ArrayList<>();
            for (Map.Entry<LocalDate, List<Showtime>> dateEntry : byDate.entrySet()) {
                if (schedules.size() >= MAX_DATES_PER_CINEMA) {
                    break;
                }
                LocalDate date = dateEntry.getKey();
                List<Showtime> dayList = dateEntry.getValue().stream()
                        .sorted(Comparator.comparing(Showtime::getShowTime))
                        .toList();

                List<String> formattedTimes = dayList.stream()
                        .map(s -> s.getShowTime().format(TIME_FMT))
                        .distinct()
                        .limit(8)
                        .toList();

                String badge = idealBusyMode
                        ? dayList.stream()
                        .min(Comparator.comparingInt(this::busyPersonPriorityTier))
                        .map(this::busyBadgeForShowtime)
                        .orElse(null)
                        : null;

                schedules.add(ShowtimeSlotDto.builder()
                        .date(date)
                        .dateLabel(formatDateLabel(date, today))
                        .busyBadge(badge)
                        .times(formattedTimes)
                        .build());
            }

            groups.add(CinemaShowtimeGroupDto.builder()
                    .cinemaId(cinema.getId())
                    .cinemaName(cinema.getName())
                    .systemName(cinema.getSystem() != null ? cinema.getSystem().getName() : null)
                    .schedules(schedules)
                    .build());
        }
        return groups;
    }

    private void appendRecommendationSummaryText(
            StringBuilder reply,
            List<ChatbotMovieRecommendationDto> recs,
            boolean idealBusyMode
    ) {
        int index = 1;
        for (ChatbotMovieRecommendationDto movie : recs) {
            reply.append(index++).append(". **").append(movie.getTitle()).append("**")
                    .append(" (⭐ ").append(formatRating(movie.getRating())).append("/10");
            if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
                reply.append(" • ").append(String.join(", ", movie.getGenres()));
            }
            reply.append(")\n");

            if (movie.getCinemaShowtimes() != null) {
                for (CinemaShowtimeGroupDto cg : movie.getCinemaShowtimes()) {
                    reply.append("   • _").append(cg.getCinemaName()).append("_: ");
                    List<String> dateSummaries = new ArrayList<>();
                    for (ShowtimeSlotDto slot : cg.getSchedules()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(slot.getDateLabel()).append(" [").append(String.join(", ", slot.getTimes())).append("]");
                        if (idealBusyMode && slot.getBusyBadge() != null) {
                            sb.append(" (").append(slot.getBusyBadge()).append(")");
                        }
                        dateSummaries.add(sb.toString());
                    }
                    reply.append(String.join(" | ", dateSummaries)).append("\n");
                }
            }
        }
    }

    private boolean hasTimeConstraint(ChatbotStateDto state) {
        return state.getTimeSlot() != null
                || state.getStartTime() != null
                || state.getEndTime() != null
                || state.getTargetDate() != null
                || state.isWeekendOnly();
    }

    private double effectiveRating(Movie m) {
        if (m == null) {
            return 0.0;
        }
        if (m.getRating() != null) {
            return m.getRating();
        }
        return m.getDefaultRating() != null ? m.getDefaultRating() : 0.0;
    }

    private String formatRating(double rating) {
        return String.format(Locale.US, "%.1f", rating);
    }

    private String formatDateLabel(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return "Hôm nay (" + date.format(VN_DATE_FMT) + ")";
        }
        if (date.equals(today.plusDays(1))) {
            return "Ngày mai (" + date.format(VN_DATE_FMT) + ")";
        }
        String dayName = switch (date.getDayOfWeek()) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ nhật";
        };
        return dayName + " (" + date.format(VN_DATE_FMT) + ")";
    }

    private List<String> extractAvailableGenres(List<Showtime> showtimes) {
        Map<String, Long> freq = new LinkedHashMap<>();
        Set<Long> seenMovieIds = new HashSet<>();
        for (Showtime s : showtimes) {
            Movie m = s.getMovie();
            if (m == null || m.getId() == null || !seenMovieIds.add(m.getId()) || m.getGenres() == null) {
                continue;
            }
            for (Genre g : m.getGenres()) {
                if (g.getName() != null) {
                    freq.merge(g.getName(), 1L, Long::sum);
                }
            }
        }
        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();
    }

    private String summarizeAvailableTimePeriods(List<Showtime> showtimes) {
        Set<String> periods = new LinkedHashSet<>();
        for (Showtime s : showtimes) {
            LocalTime t = s.getShowTime();
            if (t == null) {
                continue;
            }
            if (t.isBefore(LocalTime.of(12, 0))) {
                periods.add("Buổi sáng (trước 12h)");
            } else if (t.isBefore(LocalTime.of(14, 0))) {
                periods.add("Buổi trưa (12h-14h)");
            } else if (t.isBefore(LocalTime.of(18, 0))) {
                periods.add("Buổi chiều (14h-18h)");
            } else if (t.isBefore(LocalTime.of(22, 0))) {
                periods.add("Buổi tối (18h-22h)");
            } else {
                periods.add("Tối muộn (sau 22h)");
            }
        }
        return periods.isEmpty() ? "chưa có dữ liệu giờ" : String.join(", ", periods);
    }

    private List<ChatbotCinemaDto> toCinemaDtos(List<Cinema> cinemas) {
        return cinemas.stream()
                .map(c -> ChatbotCinemaDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .systemName(c.getSystem() != null ? c.getSystem().getName() : null)
                        .build())
                .toList();
    }

    private String formatCinemaBullets(List<Cinema> cinemas) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(MAX_CINEMAS_DISPLAY, cinemas.size());
        for (int i = 0; i < limit; i++) {
            sb.append("• ").append(cinemas.get(i).getName()).append("\n");
        }
        if (cinemas.size() > limit) {
            sb.append("• _...và ").append(cinemas.size() - limit).append(" rạp khác gần đó._\n");
        }
        return sb.toString();
    }

    private String formatCinemaListInline(List<Cinema> cinemas) {
        int limit = Math.min(4, cinemas.size());
        String joined = cinemas.stream().limit(limit).map(Cinema::getName).collect(Collectors.joining(", "));
        if (cinemas.size() > limit) {
            return joined + ", ...";
        }
        return joined;
    }
}
