package com.example.main.service;

import com.example.main.dto.cinemaRequest;
import com.example.main.dto.cinemaResponse;
import com.example.main.entity.cinema;
import com.example.main.entity.cinemaSystem;
import com.example.main.repository.cinemaRepository;
import com.example.main.repository.cinemaSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class cinemaService {

    private final cinemaRepository cinemaRepository;
    private final cinemaSystemRepository cinemaSystemRepository;

    @Transactional(readOnly = true)
    public List<cinemaResponse> getAllCinemas(Long systemId) {
        List<cinema> cinemas = (systemId != null)
                ? cinemaRepository.findBySystemId(systemId)
                : cinemaRepository.findAll();

        return cinemas.stream()
                .map(cinemaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public cinemaResponse getCinemaById(Long id) {
        cinema c = cinemaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rạp chiếu với ID: " + id));
        return cinemaResponse.fromEntity(c);
    }

    @Transactional
    public cinemaResponse createCinema(cinemaRequest request) {
        cinemaSystem system = cinemaSystemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hệ thống rạp với ID: " + request.getSystemId()));

        String trimmedName = request.getName().trim();
        if (cinemaRepository.existsByNameIgnoreCaseAndSystemId(trimmedName, system.getId())) {
            throw new IllegalArgumentException("Rạp '" + trimmedName + "' đã tồn tại trong hệ thống " + system.getName());
        }

        cinema newCinema = new cinema();
        newCinema.setName(trimmedName);
        newCinema.setSystem(system);

        cinema saved = cinemaRepository.save(newCinema);
        return cinemaResponse.fromEntity(saved);
    }

    @Transactional
    public cinemaResponse updateCinema(Long id, cinemaRequest request) {
        cinema existingCinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rạp chiếu với ID: " + id));

        cinemaSystem system = cinemaSystemRepository.findById(request.getSystemId())
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

        cinema updated = cinemaRepository.save(existingCinema);
        return cinemaResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteCinema(Long id) {
        if (!cinemaRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy rạp chiếu với ID: " + id);
        }
        cinemaRepository.deleteById(id);
    }
}