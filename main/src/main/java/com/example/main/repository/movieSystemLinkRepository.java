package com.example.main.repository;

import com.example.main.entity.movieSystemLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface movieSystemLinkRepository extends JpaRepository<movieSystemLink, Long> {
    List<movieSystemLink> findByMovieId(Long movieId);
    List<movieSystemLink> findBySystemId(Long systemId);
    Optional<movieSystemLink> findByMovieIdAndSystemId(Long movieId, Long systemId);
    boolean existsByMovieIdAndSystemId(Long movieId, Long systemId);
}