package com.example.main.service;

import com.example.main.dto.ShowtimeRequest;
import com.example.main.dto.ShowtimeResponse;
import com.example.main.entity.Cinema;
import com.example.main.entity.Movie;
import com.example.main.entity.Showtime;
import com.example.main.repository.CinemaRepository;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final CinemaRepository cinemaRepository;

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getAllShowtimes(Long movieId, Long cinemaId, LocalDate showDate) {
        List<Showtime> list;
        if (movieId != null && cinemaId != null && showDate != null) {
            list = showtimeRepository.findByMovieIdAndCinemaIdAndShowDate(movieId, cinemaId, showDate);
        } else if (movieId != null && showDate != null) {
            list = showtimeRepository.findByMovieIdAndShowDate(movieId, showDate);
        } else if (cinemaId != null && showDate != null) {
            list = showtimeRepository.findByCinemaIdAndShowDate(cinemaId, showDate);
        } else if (movieId != null) {
            list = showtimeRepository.findByMovieId(movieId);
        } else if (cinemaId != null) {
            list = showtimeRepository.findByCinemaId(cinemaId);
        } else if (showDate != null) {
            list = showtimeRepository.findByShowDate(showDate);
        } else {
            list = showtimeRepository.findAll();
        }

        return list.stream()
                .map(ShowtimeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(Long id) {
        Showtime st = showtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu với ID: " + id));
        return ShowtimeResponse.fromEntity(st);
    }

    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Movie m = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        Cinema c = cinemaRepository.findById(request.getCinemaId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rạp chiếu với ID: " + request.getCinemaId()));

        Showtime st = new Showtime();
        st.setMovie(m);
        st.setCinema(c);
        st.setShowDate(request.getShowDate());
        st.setShowTime(request.getShowTime());
        st.setMovieVersion(request.getMovieVersion().trim());

        Showtime saved = showtimeRepository.save(st);
        return ShowtimeResponse.fromEntity(saved);
    }

    @Transactional
    public ShowtimeResponse updateShowtime(Long id, ShowtimeRequest request) {
        Showtime existing = showtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu với ID: " + id));

        Movie m = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        Cinema c = cinemaRepository.findById(request.getCinemaId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rạp chiếu với ID: " + request.getCinemaId()));

        existing.setMovie(m);
        existing.setCinema(c);
        existing.setShowDate(request.getShowDate());
        existing.setShowTime(request.getShowTime());
        existing.setMovieVersion(request.getMovieVersion().trim());

        Showtime updated = showtimeRepository.save(existing);
        return ShowtimeResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteShowtime(Long id) {
        if (!showtimeRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy suất chiếu với ID: " + id);
        }
        showtimeRepository.deleteById(id);
    }
}