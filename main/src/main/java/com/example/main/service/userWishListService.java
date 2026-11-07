package com.example.main.service;

import com.example.main.dto.WishListRequest;
import com.example.main.dto.WishListResponse;
import com.example.main.entity.movie;
import com.example.main.entity.user;
import com.example.main.entity.userWishList;
import com.example.main.enums.role;
import com.example.main.repository.movieRepository;
import com.example.main.repository.userWishListRepository;
import com.example.main.security.securityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class userWishListService {

    private final userWishListRepository userWishListRepository;
    private final movieRepository movieRepository;
    private final securityUtils securityUtils;

    @Transactional
    public WishListResponse addToWishList(WishListRequest request) {
        user currentUser = securityUtils.getCurrentUser();

        movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        if (userWishListRepository.existsByUserIdAndMovieId(currentUser.getId(), movie.getId())) {
            throw new IllegalArgumentException("Phim này đã có trong danh sách yêu thích của bạn");
        }

        userWishList wishList = new userWishList(currentUser, movie);
        userWishList saved = userWishListRepository.save(wishList);

        return WishListResponse.fromEntity(saved);
    }

    @Transactional
    public void removeFromWishList(Long wishListId) {
        user currentUser = securityUtils.getCurrentUser();

        userWishList item = userWishListRepository.findById(wishListId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục trong danh sách yêu thích với ID: " + wishListId));

        // Kiểm tra quyền: chỉ chủ sở hữu hoặc ADMIN mới được xóa
        if (!item.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xóa mục yêu thích này");
        }

        userWishListRepository.delete(item);
    }

    @Transactional
    public void removeMovieFromWishList(Long movieId) {
        user currentUser = securityUtils.getCurrentUser();

        if (!userWishListRepository.existsByUserIdAndMovieId(currentUser.getId(), movieId)) {
            throw new IllegalArgumentException("Phim này không có trong danh sách yêu thích của bạn");
        }

        userWishListRepository.deleteByUserIdAndMovieId(currentUser.getId(), movieId);
    }

    @Transactional(readOnly = true)
    public List<WishListResponse> getMyWishList() {
        user currentUser = securityUtils.getCurrentUser();
        return userWishListRepository.findByUserId(currentUser.getId()).stream()
                .map(WishListResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isInWishList(Long movieId) {
        user currentUser = securityUtils.getCurrentUser();
        return userWishListRepository.existsByUserIdAndMovieId(currentUser.getId(), movieId);
    }

    @Transactional(readOnly = true)
    public WishListResponse getWishListById(Long id) {
        user currentUser = securityUtils.getCurrentUser();

        userWishList item = userWishListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục yêu thích với ID: " + id));

        if (!item.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xem mục yêu thích này");
        }

        return WishListResponse.fromEntity(item);
    }
}

