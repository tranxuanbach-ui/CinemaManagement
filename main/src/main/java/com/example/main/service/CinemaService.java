package com.example.main.service;

import com.example.main.dto.CinemaRequest;
import com.example.main.dto.CinemaResponse;
import com.example.main.entity.Cinema;
import com.example.main.entity.CinemaSystem;
import com.example.main.repository.CinemaRepository;
import com.example.main.repository.CinemaSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaSystemRepository cinemaSystemRepository;

    @Transactional(readOnly = true)
    public List<CinemaResponse> getAllCinemas(Long systemId) {
        List<Cinema> cinemas = (systemId != null)
                ? cinemaRepository.findBySystemId(systemId)
                : cinemaRepository.findAll();

        return cinemas.stream()
                .map(CinemaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(Long id) {
        Cinema c = cinemaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rạp chiếu với ID: " + id));
        return CinemaResponse.fromEntity(c);
    }

    @Transactional
    public CinemaResponse createCinema(CinemaRequest request) {
        CinemaSystem system = cinemaSystemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + request.getSystemId()));

        String trimmedName = request.getName().trim();
        if (cinemaRepository.existsByNameIgnoreCaseAndSystemId(trimmedName, system.getId())) {
            throw new IllegalArgumentException("Rạp '" + trimmedName + "' đã tồn tại trong hệ thống " + system.getName());
        }

        Cinema newCinema = new Cinema();
        newCinema.setName(trimmedName);
        newCinema.setSystem(system);

        Cinema saved = cinemaRepository.save(newCinema);
        return CinemaResponse.fromEntity(saved);
    }

    @Transactional
    public CinemaResponse updateCinema(Long id, CinemaRequest request) {
        Cinema existingCinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rạp chiếu với ID: " + id));

        CinemaSystem system = cinemaSystemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + request.getSystemId()));

        String trimmedName = request.getName().trim();
        // Nếu đổi tên hoặc đổi hệ thống rạp thì kiểm tra trùng lặp
        boolean isSameName = existingCinema.getName().equalsIgnoreCase(trimmedName);
        boolean isSameSystem = existingCinema.getSystem().getId().equals(system.getId());

        if ((!isSameName || !isSameSystem) && cinemaRepository.existsByNameIgnoreCaseAndSystemId(trimmedName, system.getId())) {
            throw new IllegalArgumentException("Rạp '" + trimmedName + "' đã tồn tại trong hệ thống " + system.getName());
        }

        existingCinema.setName(trimmedName);
        existingCinema.setSystem(system);

        Cinema updated = cinemaRepository.save(existingCinema);
        return CinemaResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteCinema(Long id) {
        if (!cinemaRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy rạp chiếu với ID: " + id);
        }
        cinemaRepository.deleteById(id);
    }
}