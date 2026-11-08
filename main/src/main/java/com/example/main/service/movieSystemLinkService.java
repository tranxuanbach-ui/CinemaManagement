package com.example.main.service;

import com.example.main.dto.movieSystemLinkRequest;
import com.example.main.dto.movieSystemLinkResponse;
import com.example.main.entity.cinemaSystem;
import com.example.main.entity.movie;
import com.example.main.entity.movieSystemLink;
import com.example.main.repository.cinemaSystemRepository;
import com.example.main.repository.movieRepository;
import com.example.main.repository.movieSystemLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class movieSystemLinkService {

    private final movieSystemLinkRepository linkRepository;
    private final movieRepository movieRepository;
    private final cinemaSystemRepository cinemaSystemRepository;

    @Transactional(readOnly = true)
    public List<movieSystemLinkResponse> getAllLinks(Long movieId, Long systemId) {
        List<movieSystemLink> links;
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
                .map(movieSystemLinkResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public movieSystemLinkResponse getLinkById(Long id) {
        movieSystemLink link = linkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy liên kết với ID: " + id));
        return movieSystemLinkResponse.fromEntity(link);
    }

    @Transactional
    public movieSystemLinkResponse createLink(movieSystemLinkRequest request) {
        if (linkRepository.existsByMovieIdAndSystemId(request.getMovieId(), request.getSystemId())) {
            throw new IllegalArgumentException("Liên kết cho phim này tại hệ thống rạp đã tồn tại");
        }

        movie m = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        cinemaSystem sys = cinemaSystemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + request.getSystemId()));

        movieSystemLink newLink = new movieSystemLink(m, sys, request.getDetailLink().trim());
        movieSystemLink saved = linkRepository.save(newLink);
        return movieSystemLinkResponse.fromEntity(saved);
    }

    @Transactional
    public movieSystemLinkResponse updateLink(Long id, movieSystemLinkRequest request) {
        movieSystemLink existing = linkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy liên kết với ID: " + id));

        boolean isSameMovie = existing.getMovie().getId().equals(request.getMovieId());
        boolean isSameSystem = existing.getSystem().getId().equals(request.getSystemId());

        if ((!isSameMovie || !isSameSystem) &&
                linkRepository.existsByMovieIdAndSystemId(request.getMovieId(), request.getSystemId())) {
            throw new IllegalArgumentException("Liên kết cho phim này tại hệ thống rạp đã tồn tại");
        }

        movie m = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        cinemaSystem sys = cinemaSystemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + request.getSystemId()));

        existing.setMovie(m);
        existing.setSystem(sys);
        existing.setDetailLink(request.getDetailLink().trim());

        movieSystemLink updated = linkRepository.save(existing);
        return movieSystemLinkResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteLink(Long id) {
        if (!linkRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy liên kết với ID: " + id);
        }
        linkRepository.deleteById(id);
    }
}