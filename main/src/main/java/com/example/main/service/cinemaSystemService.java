package com.example.main.service;

import com.example.main.dto.cinemaSystemRequest;
import com.example.main.dto.cinemaSystemResponse;
import com.example.main.entity.cinemaSystem;
import com.example.main.repository.cinemaSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class cinemaSystemService {

    private final cinemaSystemRepository cinemaSystemRepository;

    @Transactional(readOnly = true)
    public List<cinemaSystemResponse> getAllSystems() {
        return cinemaSystemRepository.findAll().stream()
                .map(cinemaSystemResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public cinemaSystemResponse getSystemById(Long id) {
        cinemaSystem system = cinemaSystemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + id));
        return cinemaSystemResponse.fromEntity(system);
    }

    @Transactional
    public cinemaSystemResponse createSystem(cinemaSystemRequest request) {
        String trimmedName = request.getName().trim();
        if (cinemaSystemRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Hệ thống rạp '" + trimmedName + "' đã tồn tại");
        }

        cinemaSystem newSystem = new cinemaSystem(trimmedName);
        cinemaSystem saved = cinemaSystemRepository.save(newSystem);
        return cinemaSystemResponse.fromEntity(saved);
    }

    @Transactional
    public cinemaSystemResponse updateSystem(Long id, cinemaSystemRequest request) {
        cinemaSystem system = cinemaSystemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + id));

        String trimmedName = request.getName().trim();
        cinemaSystemRepository.findByNameIgnoreCase(trimmedName)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("Tên hệ thống rạp '" + trimmedName + "' đã được sử dụng");
                    }
                });

        system.setName(trimmedName);
        cinemaSystem updated = cinemaSystemRepository.save(system);
        return cinemaSystemResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteSystem(Long id) {
        if (!cinemaSystemRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + id);
        }
        cinemaSystemRepository.deleteById(id);
    }
}