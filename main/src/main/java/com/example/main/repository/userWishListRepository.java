package com.example.main.repository;

import com.example.main.entity.userWishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface userWishListRepository extends JpaRepository<userWishList, Long> {
    Optional<userWishList> findByUserIdAndMovieId(Long userId, Long movieId);
    List<userWishList> findByUserId(Long userId);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    void deleteByUserIdAndMovieId(Long userId, Long movieId);

    // Tự động xóa phim khỏi toàn bộ danh sách theo dõi của người dùng khi phim bị xóa
    @Modifying
    @Query("DELETE FROM userWishList w WHERE w.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
}
