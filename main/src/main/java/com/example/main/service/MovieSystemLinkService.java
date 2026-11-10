package com.example.main.service;

import com.example.main.dto.MovieSystemLinkRequest;
import com.example.main.dto.MovieSystemLinkResponse;
import com.example.main.entity.CinemaSystem;
import com.example.main.entity.Movie;
import com.example.main.entity.MovieSystemLink;
import com.example.main.repository.CinemaSystemRepository;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.MovieSystemLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieSystemLinkService {

    private final MovieSystemLinkRepository linkRepository;
    private final MovieRepository movieRepository;
    private final CinemaSystemRepository cinemaSystemRepository;

    @Transactional(readOnly = true)
    public List<MovieSystemLinkResponse> getAllLinks(Long movieId, Long systemId) {
        List<MovieSystemLink> links;
        if (movieId != null && systemId != null) {
            links = linkRepository.findByMovieIdAndSystemId(movieId, systemId)
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (movieId != null) {
            links = linkRepository.findByMovieId(movieId);
        } else if (systemId != null) {
            links = linkRepository.findBySystemId(systemId);
        } else {
            links = linkRepository.findAll();
        }

        return links.stream()
                .map(MovieSystemLinkResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovieSystemLinkResponse getLinkById(Long id) {
        MovieSystemLink link = linkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy liên kết với ID: " + id));
        return MovieSystemLinkResponse.fromEntity(link);
    }

    @Transactional
    public MovieSystemLinkResponse createLink(MovieSystemLinkRequest request) {
        if (linkRepository.existsByMovieIdAndSystemId(request.getMovieId(), request.getSystemId())) {
            throw new IllegalArgumentException("Liên kết cho phim này tại hệ thống rạp đã tồn tại");
        }

        Movie m = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        CinemaSystem sys = cinemaSystemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + request.getSystemId()));

        MovieSystemLink newLink = new MovieSystemLink(m, sys, request.getDetailLink().trim());
        MovieSystemLink saved = linkRepository.save(newLink);
        return MovieSystemLinkResponse.fromEntity(saved);
    }

    @Transactional
    public MovieSystemLinkResponse updateLink(Long id, MovieSystemLinkRequest request) {
        MovieSystemLink existing = linkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy liên kết với ID: " + id));

        boolean isSameMovie = existing.getMovie().getId().equals(request.getMovieId());
        boolean isSameSystem = existing.getSystem().getId().equals(request.getSystemId());

        if ((!isSameMovie || !isSameSystem) &&
                linkRepository.existsByMovieIdAndSystemId(request.getMovieId(), request.getSystemId())) {
            throw new IllegalArgumentException("Liên kết cho phim này tại hệ thống rạp đã tồn tại");
        }

        Movie m = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        CinemaSystem sys = cinemaSystemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + request.getSystemId()));

        existing.setMovie(m);
        existing.setSystem(sys);
        existing.setDetailLink(request.getDetailLink().trim());

        MovieSystemLink updated = linkRepository.save(existing);
        return MovieSystemLinkResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteLink(Long id) {
        if (!linkRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy liên kết với ID: " + id);
        }
        linkRepository.deleteById(id);
    }
}