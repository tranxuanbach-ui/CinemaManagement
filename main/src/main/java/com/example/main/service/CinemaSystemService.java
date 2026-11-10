package com.example.main.service;

import com.example.main.dto.CinemaSystemRequest;
import com.example.main.dto.CinemaSystemResponse;
import com.example.main.entity.CinemaSystem;
import com.example.main.repository.CinemaSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CinemaSystemService {

    private final CinemaSystemRepository cinemaSystemRepository;

    @Transactional(readOnly = true)
    public List<CinemaSystemResponse> getAllSystems() {
        return cinemaSystemRepository.findAll().stream()
                .map(CinemaSystemResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CinemaSystemResponse getSystemById(Long id) {
        CinemaSystem system = cinemaSystemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + id));
        return CinemaSystemResponse.fromEntity(system);
    }

    @Transactional
    public CinemaSystemResponse createSystem(CinemaSystemRequest request) {
        String trimmedName = request.getName().trim();
        if (cinemaSystemRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Hệ thống rạp '" + trimmedName + "' đã tồn tại");
        }

        CinemaSystem newSystem = new CinemaSystem(trimmedName);
        CinemaSystem saved = cinemaSystemRepository.save(newSystem);
        return CinemaSystemResponse.fromEntity(saved);
    }

    @Transactional
    public CinemaSystemResponse updateSystem(Long id, CinemaSystemRequest request) {
        CinemaSystem system = cinemaSystemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + id));

        String trimmedName = request.getName().trim();
        cinemaSystemRepository.findByNameIgnoreCase(trimmedName)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("Tên hệ thống rạp '" + trimmedName + "' đã được sử dụng");
                    }
                });

        system.setName(trimmedName);
        CinemaSystem updated = cinemaSystemRepository.save(system);
        return CinemaSystemResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteSystem(Long id) {
        if (!cinemaSystemRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + id);
        }
        cinemaSystemRepository.deleteById(id);
    }
}