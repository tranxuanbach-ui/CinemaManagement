package com.example.main.service;

import com.example.main.dto.GenreRequest;
import com.example.main.dto.GenreResponse;
import com.example.main.entity.genre;
import com.example.main.repository.genreRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class genreService {

    private final genreRepository genreRepository;

    // 1. Lấy danh sách thể loại
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll()
                .stream()
                .map(g -> new GenreResponse(g.getId(), g.getName()))
                .toList();
    }

    // 2. Lấy thể loại theo ID
    public GenreResponse getGenreById(Long id) {
        genre g = genreRepository.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy thể loại"));

        return new GenreResponse(g.getId(), g.getName());
    }

    // 3. Thêm thể loại
    public GenreResponse createGenre(GenreRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException(
                "Tên thể loại không được để trống");
        }

        if (genreRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException(
                "Thể loại đã tồn tại");
        }

        genre g = new genre();
        g.setName(request.getName());

        genre savedGenre = genreRepository.save(g);

        return new GenreResponse(
            savedGenre.getId(), savedGenre.getName());
    }

    // 4. Cập nhật thể loại
    public GenreResponse updateGenre(Long id, GenreRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException(
                "Tên thể loại không được để trống");
        }

        genre g = genreRepository.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy thể loại"));

        boolean nameExists = genreRepository.findByName(request.getName())
                .filter(existing -> !existing.getId().equals(id))
                .isPresent();

        if (nameExists) {
            throw new IllegalArgumentException(
                "Tên thể loại đã được sử dụng");
        }

        g.setName(request.getName());

        genre updatedGenre = genreRepository.save(g);

        return new GenreResponse(
            updatedGenre.getId(), updatedGenre.getName());
    }

    // 5. Xóa thể loại
    @Transactional
    public void deleteGenre(Long id) {
        genre g = genreRepository.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy thể loại"));

        // Xóa liên kết với các phim, không xóa phim
        g.getMovies().forEach(movie ->
                movie.getGenres().remove(g));

        genreRepository.delete(g);
    }
}
