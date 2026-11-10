package com.example.main.service;

import com.example.main.dto.GenreResponse;
import com.example.main.dto.MovieResponse;
import com.example.main.entity.Genre;
import com.example.main.entity.Movie;
import com.example.main.entity.User;
import com.example.main.entity.UserRating;
import com.example.main.entity.UserWishList;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.UserRatingRepository;
import com.example.main.repository.UserWishListRepository;
import com.example.main.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MovieRepository movieRepository;
    private final UserRatingRepository userRatingRepository;
    private final UserWishListRepository userWishListRepository;
    private final SecurityUtils securityUtils;

    private static final int DEFAULT_TOP_LIMIT = 10;

    /**
     * Lấy danh sách phim gợi ý cho người dùng hiện tại theo phần trăm ưu tiên.
     * Tự động áp dụng Cold Start nếu chưa đăng nhập hoặc là người dùng mới.
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> getRecommendations(Integer limit) {
        int finalLimit = (limit != null && limit > 0) ? limit : DEFAULT_TOP_LIMIT;

        Optional<User> currentUserOpt = securityUtils.getCurrentUserSafely();
        if (currentUserOpt.isEmpty()) {
            // Khách chưa đăng nhập -> Áp dụng Cold Start
            return getColdStartRecommendations(finalLimit);
        }

        return getRecommendationsForUser(currentUserOpt.get().getId(), finalLimit);
    }

    /**
     * Xử lý Cold Start: Trả về danh sách top phim có điểm đánh giá cao nhất trong database.
     * Phần trăm ưu tiên của phim được chuẩn hóa theo thang điểm đánh giá (0% - 100%).
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> getColdStartRecommendations(int limit) {
        List<Movie> topMovies = movieRepository.findTopRatedMovies(PageRequest.of(0, limit));
        return topMovies.stream().map(m -> {
            double baseRating = m.getRating() != null ? m.getRating() :
                    (m.getDefaultRating() != null ? m.getDefaultRating() : 5.0);
            double ratingPercent = Math.min(100.0, Math.max(0.0, (baseRating / 10.0) * 100.0));
            return toResponse(m, ratingPercent);
        }).collect(Collectors.toList());
    }

    /**
     * Tính toán mức độ ưu tiên theo PHẦN TRĂM (0.0% - 100.0%) của người dùng dựa trên Thể loại (Genre)
     * từ dữ liệu UserWishList và UserRating.
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> getRecommendationsForUser(Long userId, int limit) {
        List<UserWishList> wishLists = userWishListRepository.findByUserId(userId);
        List<UserRating> ratings = userRatingRepository.findByUserId(userId);

        // Trường hợp người dùng mới chưa từng đánh giá hoặc thêm phim yêu thích nào -> Cold Start
        if (wishLists.isEmpty() && ratings.isEmpty()) {
            return getColdStartRecommendations(limit);
        }

        // 1. Thu thập điểm quan tâm thô (Raw Interest Points) cho từng thể loại
        Map<Long, Double> genreRawWeights = new HashMap<>();

        // A. Tín hiệu từ Danh sách yêu thích (Wishlist): +5.0 điểm cho mỗi thể loại
        for (UserWishList wl : wishLists) {
            if (wl.getMovie() != null && wl.getMovie().getGenres() != null) {
                for (Genre g : wl.getMovie().getGenres()) {
                    genreRawWeights.merge(g.getId(), 5.0, Double::sum);
                }
            }
        }

        // B. Tín hiệu từ Đánh giá (Rating): Phim rating >= 5 đóng góp tích cực
        // Điểm đánh giá (0 - 10 sao): 10 sao -> +6.0; 8 sao -> +4.0; 5 sao -> +1.0; < 5 sao -> 0 điểm
        for (UserRating r : ratings) {
            if (r.getMovie() != null && r.getMovie().getGenres() != null && r.getRating() != null) {
                double positiveWeight = Math.max(0.0, r.getRating() - 4.0);
                if (positiveWeight > 0) {
                    for (Genre g : r.getMovie().getGenres()) {
                        genreRawWeights.merge(g.getId(), positiveWeight, Double::sum);
                    }
                }
            }
        }

        double totalGenreWeight = genreRawWeights.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        // Nếu tổng điểm thể loại = 0 -> Fallback sang Cold Start
        if (totalGenreWeight <= 0) {
            return getColdStartRecommendations(limit);
        }

        // 2. Chuẩn hóa thành PHẦN TRĂM SỞ THÍCH CHO TỪNG THỂ LOẠI (Tổng tất cả thể loại = 100%)
        Map<Long, Double> genrePercentageMap = new HashMap<>();
        for (Map.Entry<Long, Double> entry : genreRawWeights.entrySet()) {
            double percent = (entry.getValue() / totalGenreWeight) * 100.0;
            genrePercentageMap.put(entry.getKey(), percent);
        }

        // 3. Danh sách các phim người dùng đã tương tác (cần loại trừ khỏi kết quả gợi ý)
        Set<Long> excludedMovieIds = new HashSet<>();
        for (UserWishList wl : wishLists) {
            if (wl.getMovie() != null) excludedMovieIds.add(wl.getMovie().getId());
        }
        for (UserRating r : ratings) {
            if (r.getMovie() != null) excludedMovieIds.add(r.getMovie().getId());
        }

        // 4. Quét toàn bộ phim ứng viên để tính phần trăm ưu tiên (0% - 100%)
        List<Movie> allMovies = movieRepository.findAllWithGenres();

        class ScoredMovie {
            final Movie movie;
            final double finalPercentage;

            ScoredMovie(Movie movie, double finalPercentage) {
                this.movie = movie;
                this.finalPercentage = finalPercentage;
            }
        }

        List<ScoredMovie> scoredMovies = new ArrayList<>();
        for (Movie m : allMovies) {
            if (excludedMovieIds.contains(m.getId())) {
                continue; // Loại bỏ phim người dùng đã xem/đánh giá/lưu
            }

            // a. Phần trăm độ phủ thể loại (Genre Match Percentage): tổng % các thể loại của phim mà user yêu thích
            double genreMatchPercent = 0.0;
            if (m.getGenres() != null) {
                for (Genre g : m.getGenres()) {
                    Double p = genrePercentageMap.get(g.getId());
                    if (p != null) {
                        genreMatchPercent += p;
                    }
                }
            }
            genreMatchPercent = Math.min(100.0, genreMatchPercent);

            // b. Phần trăm chất lượng phim (Rating Percentage): quy đổi thang 10 sang thang 100%
            double baseRating = m.getRating() != null ? m.getRating() :
                    (m.getDefaultRating() != null ? m.getDefaultRating() : 5.0);
            double ratingPercent = Math.min(100.0, Math.max(0.0, (baseRating / 10.0) * 100.0));

            // c. Phần trăm ưu tiên tổng thể (Bounded trong [0%, 100%]):
            // 70% từ độ phù hợp thể loại + 30% từ chất lượng tổng thể phim
            double finalPriorityPercent = (genreMatchPercent * 0.7) + (ratingPercent * 0.3);

            // Chỉ đưa vào danh sách nếu phim có ít nhất 1 thể loại thuộc sở thích người dùng
            if (genreMatchPercent > 0) {
                scoredMovies.add(new ScoredMovie(m, finalPriorityPercent));
            }
        }

        // Sắp xếp giảm dần theo phần trăm ưu tiên
        scoredMovies.sort((a, b) -> Double.compare(b.finalPercentage, a.finalPercentage));

        List<MovieResponse> recommendedResponses = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scoredMovies.size()); i++) {
            ScoredMovie sm = scoredMovies.get(i);
            recommendedResponses.add(toResponse(sm.movie, sm.finalPercentage));
        }

        // 5. Nếu chưa đủ số lượng phim (limit = 10) -> Bổ sung các phim điểm cao nhất (Cold Start fallback)
        if (recommendedResponses.size() < limit) {
            Set<Long> alreadyChosenIds = recommendedResponses.stream()
                    .map(MovieResponse::getId)
                    .collect(Collectors.toSet());

            List<Movie> topRatedMovies = movieRepository.findTopRatedMovies(PageRequest.of(0, limit * 2));
            for (Movie topMovie : topRatedMovies) {
                if (recommendedResponses.size() >= limit) break;
                if (!excludedMovieIds.contains(topMovie.getId()) && !alreadyChosenIds.contains(topMovie.getId())) {
                    double baseRating = topMovie.getRating() != null ? topMovie.getRating() :
                            (topMovie.getDefaultRating() != null ? topMovie.getDefaultRating() : 5.0);
                    double ratingPercent = Math.min(100.0, Math.max(0.0, (baseRating / 10.0) * 100.0));
                    recommendedResponses.add(toResponse(topMovie, ratingPercent));
                    alreadyChosenIds.add(topMovie.getId());
                }
            }
        }

        return recommendedResponses;
    }

    private MovieResponse toResponse(Movie m, Double matchPercentage) {
        List<GenreResponse> genres = (m.getGenres() == null) ? Collections.emptyList() :
                m.getGenres().stream()
                        .map(g -> new GenreResponse(g.getId(), g.getName()))
                        .toList();

        Double roundedPercentage = (matchPercentage != null) ?
                Math.round(matchPercentage * 10.0) / 10.0 : null;

        return new MovieResponse(
                m.getId(),
                m.getTitle(),
                m.getReleaseDate(),
                m.getDescription(),
                m.getPosterUrl(),
                m.getTrailer(),
                m.getRating(),
                m.getRuntimeMinutes(),
                genres,
                roundedPercentage
        );
    }
}
