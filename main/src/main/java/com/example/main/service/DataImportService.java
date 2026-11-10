package com.example.main.service;

import com.example.main.dto.DataImportResponse;
import com.example.main.entity.Cinema;
import com.example.main.entity.CinemaSystem;
import com.example.main.entity.Genre;
import com.example.main.entity.Movie;
import com.example.main.entity.MovieSystemLink;
import com.example.main.entity.Showtime;
import com.example.main.repository.CinemaRepository;
import com.example.main.repository.CinemaSystemRepository;
import com.example.main.repository.GenreRepository;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.MovieSystemLinkRepository;
import com.example.main.repository.ShowtimeRepository;
import com.example.main.repository.UserRatingRepository;
import com.example.main.repository.UserWishListRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final CinemaSystemRepository cinemaSystemRepository;
    private final CinemaRepository cinemaRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieSystemLinkRepository movieSystemLinkRepository;
    private final UserWishListRepository userWishListRepository;
    private final UserRatingRepository userRatingRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    /**
     * Đồng bộ dữ liệu phim và lịch chiếu từ file data_phim_java.json.
     * Quy tắc:
     * 1. Phim nào KHÔNG có trong JSON -> Xóa khỏi database, tự động xóa khỏi danh sách theo dõi (wishlist) của người dùng.
     * 2. Phim nào CÓ SẴN trong database -> Cập nhật thông tin và cập nhật toàn bộ lịch chiếu mới.
     * 3. Phim nào MỚI trong JSON -> Thêm mới phim, thể loại, link rạp và lịch chiếu.
     */
    @Transactional
    public DataImportResponse importDataFromJson() {
        log.info("Bắt đầu tiến trình đồng bộ dữ liệu phim từ file JSON...");

        JsonNode rootNode = loadJsonNode();
        if (rootNode == null || !rootNode.isArray()) {
            throw new IllegalStateException("Không thể đọc được mảng phim từ file JSON!");
        }

        int totalJsonMovies = rootNode.size();
        log.info("Đã nạp thành công {} phim từ JSON", totalJsonMovies);

        // 1. Tải cache bộ nhớ để giảm tải query DB
        Map<String, Genre> genreCache = new HashMap<>();
        genreRepository.findAll().forEach(g -> genreCache.put(g.getName().trim().toLowerCase(), g));

        Map<String, CinemaSystem> systemCache = new HashMap<>();
        cinemaSystemRepository.findAll().forEach(s -> systemCache.put(s.getName().trim().toLowerCase(), s));

        Map<String, Cinema> cinemaCache = new HashMap<>();
        cinemaRepository.findAll().forEach(c -> {
            Long sysId = c.getSystem() != null ? c.getSystem().getId() : 0L;
            cinemaCache.put(sysId + "_" + c.getName().trim().toLowerCase(), c);
        });

        // 2. Thu thập danh sách tiêu đề chuẩn hóa từ JSON
        Set<String> jsonNormalizedTitles = new HashSet<>();
        for (JsonNode mNode : rootNode) {
            String title = mNode.path("title").asText("").trim();
            if (!title.isEmpty()) {
                jsonNormalizedTitles.add(normalize(title));
            }
        }

        // 3. XỬ LÝ XÓA: Phim nào không có trong JSON thì xóa khỏi DB
        // Khi xóa: Tự động xóa phim khỏi phần theo dõi (wishlist) của tất cả user
        List<Movie> allDbMovies = movieRepository.findAll();
        List<String> deletedMovieTitles = new ArrayList<>();

        for (Movie m : allDbMovies) {
            if (!jsonNormalizedTitles.contains(normalize(m.getTitle()))) {
                Long movieId = m.getId();
                deletedMovieTitles.add(m.getTitle() + " (#" + movieId + ")");

                // Bước 3.1: Tự động xóa phim khỏi danh sách theo dõi của người dùng
                userWishListRepository.deleteByMovieId(movieId);

                // Bước 3.2: Xóa các đánh giá của người dùng
                userRatingRepository.deleteByMovieId(movieId);

                // Bước 3.3: Xóa lịch suất chiếu
                showtimeRepository.deleteByMovieId(movieId);

                // Bước 3.4: Xóa liên kết hệ thống rạp
                movieSystemLinkRepository.deleteByMovieId(movieId);

                // Bước 3.5: Xóa quan hệ thể loại
                if (m.getGenres() != null) {
                    m.getGenres().clear();
                }

                // Bước 3.6: Xóa phim khỏi database
                movieRepository.delete(m);
                log.info("-> Đã xóa phim '{}' (#{}) và tự động xóa khỏi danh sách theo dõi của user", m.getTitle(), movieId);
            }
        }

        // 4. XỬ LÝ CẬP NHẬT HOẶC THÊM MỚI PHIM VÀ LỊCH CHIẾU
        List<String> updatedMovieTitles = new ArrayList<>();
        List<String> createdMovieTitles = new ArrayList<>();
        int totalShowtimesImported = 0;

        for (JsonNode mNode : rootNode) {
            String title = mNode.path("title").asText("").trim();
            if (title.isEmpty()) continue;

            String releaseDateStr = mNode.path("release_date").asText("").trim();
            LocalDate releaseDate = null;
            if (!releaseDateStr.isEmpty()) {
                try {
                    releaseDate = LocalDate.parse(releaseDateStr, DATE_FORMATTER);
                } catch (Exception ex) {
                    log.warn("Không thể parse release_date '{}' cho phim '{}'", releaseDateStr, title);
                }
            }

            String description = mNode.path("description").asText("");
            String posterUrl = mNode.path("poster_url").asText("");
            String trailer = mNode.path("trailer").asText("");
            Double rating = mNode.path("rating").isNumber() ? mNode.path("rating").asDouble() : 0.0;
            Integer runtimeMinutes = mNode.path("runtime_minutes").isInt() ? mNode.path("runtime_minutes").asInt() : 0;

            // Thể loại (genres)
            Set<Genre> movieGenres = new HashSet<>();
            JsonNode genresArray = mNode.path("genres");
            if (genresArray.isArray()) {
                for (JsonNode gNode : genresArray) {
                    String gName = gNode.asText("").trim();
                    if (!gName.isEmpty()) {
                        movieGenres.add(getOrCreateGenre(gName, genreCache));
                    }
                }
            }

            // Kiểm tra xem phim đã có sẵn trong database hay chưa
            Optional<Movie> existingOpt = movieRepository.findByTitleIgnoreCase(title);
            Movie currentMovie;
            boolean isExisting = existingOpt.isPresent();

            if (isExisting) {
                currentMovie = existingOpt.get();
                updatedMovieTitles.add(title);
            } else {
                currentMovie = new Movie();
                currentMovie.setTitle(title);
                createdMovieTitles.add(title);
            }

            currentMovie.setDefaultRating(rating);
            currentMovie.setReleaseDate(releaseDate);
            currentMovie.setDescription(description);
            currentMovie.setPosterUrl(posterUrl);
            currentMovie.setTrailer(trailer);
            currentMovie.setRuntimeMinutes(runtimeMinutes);
            currentMovie.setGenres(movieGenres);

            // Tính điểm rating trung bình cộng (gốc + user ratings)
            double sumRating = rating > 0.0 ? rating : 0.0;
            int countRating = rating > 0.0 ? 1 : 0;
            if (currentMovie.getId() != null) {
                var userRatings = userRatingRepository.findByMovieId(currentMovie.getId());
                if (userRatings != null) {
                    for (var ur : userRatings) {
                        if (ur.getRating() != null && ur.getRating() > 0.0) {
                            sumRating += ur.getRating();
                            countRating++;
                        }
                    }
                }
            }
            if (countRating > 0) {
                currentMovie.setRating(Math.round((sumRating / countRating) * 10.0) / 10.0);
            } else {
                currentMovie.setRating(0.0);
            }

            // Lưu phim
            currentMovie = movieRepository.save(currentMovie);

            // Cập nhật detail_link (movie_system_link)
            movieSystemLinkRepository.deleteByMovieId(currentMovie.getId());
            JsonNode detailLinkNode = mNode.path("detail_link");
            if (detailLinkNode.isObject()) {
                for (Map.Entry<String, JsonNode> entry : detailLinkNode.properties()) {
                    String systemName = entry.getKey().trim();
                    String linkUrl = entry.getValue().asText("").trim();
                    if (!systemName.isEmpty() && !linkUrl.isEmpty()) {
                        CinemaSystem sys = getOrCreateSystem(systemName, systemCache);
                        MovieSystemLink link = new MovieSystemLink(currentMovie, sys, linkUrl);
                        movieSystemLinkRepository.save(link);
                    }
                }
            }

            // Cập nhật lịch chiếu: Xóa lịch chiếu cũ của phim và chèn toàn bộ lịch chiếu mới từ JSON
            showtimeRepository.deleteByMovieId(currentMovie.getId());

            List<Showtime> showtimesToSave = new ArrayList<>();
            JsonNode showtimesArray = mNode.path("showtimes");
            if (showtimesArray.isArray()) {
                for (JsonNode stNode : showtimesArray) {
                    String sysName = stNode.path("system").asText("").trim();
                    if (sysName.isEmpty()) continue;
                    CinemaSystem sys = getOrCreateSystem(sysName, systemCache);

                    JsonNode cinemaShowtimesArray = stNode.path("system_showtimes");
                    if (cinemaShowtimesArray.isArray()) {
                        for (JsonNode cNode : cinemaShowtimesArray) {
                            String cinemaName = cNode.path("cinema_name").asText("").trim();
                            if (cinemaName.isEmpty()) continue;
                            Cinema cin = getOrCreateCinema(cinemaName, sys, cinemaCache);

                            JsonNode scheduleArray = cNode.path("schedule");
                            if (scheduleArray.isArray()) {
                                for (JsonNode schNode : scheduleArray) {
                                    String dateStr = schNode.path("date").asText("").trim();
                                    LocalDate showDate;
                                    try {
                                        showDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                                    } catch (Exception e) {
                                        continue;
                                    }

                                    JsonNode versionsArray = schNode.path("versions");
                                    if (versionsArray.isArray()) {
                                        for (JsonNode vNode : versionsArray) {
                                            String version = vNode.path("version").asText("2D").trim();
                                            JsonNode timesArray = vNode.path("times");
                                            if (timesArray.isArray()) {
                                                for (JsonNode tNode : timesArray) {
                                                    String timeStr = tNode.asText("").trim();
                                                    LocalTime showTime;
                                                    try {
                                                        showTime = LocalTime.parse(timeStr, TIME_FORMATTER);
                                                    } catch (Exception e) {
                                                        continue;
                                                    }

                                                    Showtime st = new Showtime();
                                                    st.setMovie(currentMovie);
                                                    st.setCinema(cin);
                                                    st.setShowDate(showDate);
                                                    st.setShowTime(showTime);
                                                    st.setMovieVersion(version);
                                                    showtimesToSave.add(st);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!showtimesToSave.isEmpty()) {
                showtimeRepository.saveAll(showtimesToSave);
                totalShowtimesImported += showtimesToSave.size();
            }
        }

        String summary = String.format(
                "Đồng bộ hoàn tất: Tổng %d phim từ JSON, %d phim cập nhật lịch chiếu, %d phim mới, %d phim đã xóa (tự động xóa theo dõi của user), %d suất chiếu đã lưu.",
                totalJsonMovies, updatedMovieTitles.size(), createdMovieTitles.size(), deletedMovieTitles.size(), totalShowtimesImported
        );
        log.info(summary);

        return DataImportResponse.builder()
                .message(summary)
                .totalJsonMovies(totalJsonMovies)
                .createdMovies(createdMovieTitles.size())
                .updatedMovies(updatedMovieTitles.size())
                .deletedMovies(deletedMovieTitles.size())
                .userWishlistsRemoved(deletedMovieTitles.size())
                .totalShowtimesImported(totalShowtimesImported)
                .deletedMovieTitles(deletedMovieTitles)
                .updatedMovieTitles(updatedMovieTitles)
                .createdMovieTitles(createdMovieTitles)
                .build();
    }

    private JsonNode loadJsonNode() {
        // Kiểm tra các đường dẫn file khả dĩ (trên ổ đĩa hoặc classpath)
        String[] candidateFilePaths = {
                "main/src/main/resources/data/dataPhimJava.json", "main/src/main/resources/data/data_phim_java.json",
                "src/main/resources/data/dataPhimJava.json", "src/main/resources/data/data_phim_java.json",
                "main/src/main/resources/data/data_phim_json.java",
                "src/main/resources/data/data_phim_json.java",
                "main/target/classes/data/data_phim_java.json",
                "target/classes/data/data_phim_java.json"
        };

        ObjectMapper mapper = new ObjectMapper();

        for (String path : candidateFilePaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try (InputStream is = new FileInputStream(f)) {
                    log.info("Tìm thấy file JSON tại đường dẫn: {}", f.getAbsolutePath());
                    return mapper.readTree(is);
                } catch (Exception e) {
                    log.warn("Lỗi khi đọc file từ đường dẫn {}: {}", path, e.getMessage());
                }
            }
        }

        // Thử tải từ Classpath resource
        String[] candidateResources = {
                "data/dataPhimJava.json", "data/data_phim_java.json",
                "data/data_phim_json.java"
        };
        for (String res : candidateResources) {
            try {
                ClassPathResource resource = new ClassPathResource(res);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        log.info("Tìm thấy file JSON từ classpath resource: {}", res);
                        return mapper.readTree(is);
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi khi đọc resource {}: {}", res, e.getMessage());
            }
        }

        throw new IllegalStateException("Không tìm thấy file data_phim_java.json trong dự án!");
    }

    private Genre getOrCreateGenre(String name, Map<String, Genre> cache) {
        String key = name.trim().toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Genre g = genreRepository.findByName(name.trim())
                .orElseGet(() -> genreRepository.save(new Genre(name.trim())));
        cache.put(key, g);
        return g;
    }

    private CinemaSystem getOrCreateSystem(String name, Map<String, CinemaSystem> cache) {
        String key = name.trim().toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        CinemaSystem sys = cinemaSystemRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> cinemaSystemRepository.save(new CinemaSystem(name.trim())));
        cache.put(key, sys);
        return sys;
    }

    private Cinema getOrCreateCinema(String name, CinemaSystem system, Map<String, Cinema> cache) {
        String key = system.getId() + "_" + name.trim().toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Cinema cin = cinemaRepository.findByNameIgnoreCaseAndSystemId(name.trim(), system.getId())
                .orElseGet(() -> {
                    Cinema c = new Cinema();
                    c.setName(name.trim());
                    c.setSystem(system);
                    return cinemaRepository.save(c);
                });
        cache.put(key, cin);
        return cin;
    }

    @PostConstruct
    public void initDefaultRatingsIfMissing() {
        try {
            List<Movie> allMovies = movieRepository.findAll();
            boolean needUpdate = allMovies.stream().anyMatch(m -> m.getDefaultRating() == null);
            if (!needUpdate) return;

            JsonNode rootNode = loadJsonNode();
            if (rootNode != null && rootNode.isArray()) {
                Map<String, Double> jsonRatings = new HashMap<>();
                for (JsonNode mNode : rootNode) {
                    String title = mNode.path("title").asText("").trim();
                    Double r = mNode.path("rating").isNumber() ? mNode.path("rating").asDouble() : 0.0;
                    if (!title.isEmpty()) {
                        jsonRatings.put(normalize(title), r);
                    }
                }

                for (Movie m : allMovies) {
                    if (m.getDefaultRating() == null) {
                        Double defRating = jsonRatings.getOrDefault(normalize(m.getTitle()), 0.0);
                        m.setDefaultRating(defRating);

                        double sum = defRating > 0.0 ? defRating : 0.0;
                        int count = defRating > 0.0 ? 1 : 0;
                        var userRatings = userRatingRepository.findByMovieId(m.getId());
                        if (userRatings != null) {
                            for (var ur : userRatings) {
                                if (ur.getRating() != null && ur.getRating() > 0.0) {
                                    sum += ur.getRating();
                                    count++;
                                }
                            }
                        }
                        if (count > 0) {
                            m.setRating(Math.round((sum / count) * 10.0) / 10.0);
                        } else {
                            m.setRating(0.0);
                        }
                        movieRepository.save(m);
                    }
                }
                log.info("Đã đồng bộ lại điểm đánh giá gốc (default_rating) cho các phim trong DB");
            }
        } catch (Exception e) {
            log.warn("Không thể tự động khởi tạo default_rating: {}", e.getMessage());
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
