package com.example.main.service.user;

import com.example.main.dto.user.UserDto;
import com.example.main.entity.user.User;
import com.example.main.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(UserDto request){
        User user = new User();
        if (userRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("User existed (ᵕ—ᴗ—)");

        user.setId((request.getId()));
        user.setUsername(request.getUsername());
        return userRepository.save(user);
    }

    public List<User> getUser(){
        return userRepository.findAll();
    }
    public User getUser(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found ¯\\_(ツ)_/¯"));
    }

    public void deleteUser(Long id){
        userRepository.deleteById(id);
    }
}
