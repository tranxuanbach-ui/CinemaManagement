package com.example.main.service;

import com.example.main.dto.WishListRequest;
import com.example.main.dto.WishListResponse;
import com.example.main.entity.Movie;
import com.example.main.entity.User;
import com.example.main.entity.UserWishList;
import com.example.main.enums.Role;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.UserWishListRepository;
import com.example.main.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserWishListService {

    private final UserWishListRepository userWishListRepository;
    private final MovieRepository movieRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public WishListResponse addToWishList(WishListRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        if (userWishListRepository.existsByUserIdAndMovieId(currentUser.getId(), movie.getId())) {
            throw new IllegalArgumentException("Phim này đã có trong danh sách yêu thích của bạn");
        }

        UserWishList wishList = new UserWishList(currentUser, movie);
        UserWishList saved = userWishListRepository.save(wishList);

        return WishListResponse.fromEntity(saved);
    }

    @Transactional
    public void removeFromWishList(Long wishListId) {
        User currentUser = securityUtils.getCurrentUser();

        UserWishList item = userWishListRepository.findById(wishListId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục trong danh sách yêu thích với ID: " + wishListId));

        // Kiểm tra quyền: chỉ chủ sở hữu hoặc ADMIN mới được xóa
        if (!item.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xóa mục yêu thích này");
        }

        userWishListRepository.delete(item);
    }

    @Transactional
    public void removeMovieFromWishList(Long movieId) {
        User currentUser = securityUtils.getCurrentUser();

        if (!userWishListRepository.existsByUserIdAndMovieId(currentUser.getId(), movieId)) {
            throw new IllegalArgumentException("Phim này không có trong danh sách yêu thích của bạn");
        }

        userWishListRepository.deleteByUserIdAndMovieId(currentUser.getId(), movieId);
    }

    @Transactional(readOnly = true)
    public List<WishListResponse> getMyWishList() {
        User currentUser = securityUtils.getCurrentUser();
        return userWishListRepository.findByUserId(currentUser.getId()).stream()
                .map(WishListResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isInWishList(Long movieId) {
        User currentUser = securityUtils.getCurrentUser();
        return userWishListRepository.existsByUserIdAndMovieId(currentUser.getId(), movieId);
    }

    @Transactional(readOnly = true)
    public WishListResponse getWishListById(Long id) {
        User currentUser = securityUtils.getCurrentUser();

        UserWishList item = userWishListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục yêu thích với ID: " + id));

        if (!item.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xem mục yêu thích này");
        }

        return WishListResponse.fromEntity(item);
    }
}

