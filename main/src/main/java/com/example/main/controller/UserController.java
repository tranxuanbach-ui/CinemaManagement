package com.example.main.controller;

import com.example.main.dto.user.UserDto;
import com.example.main.entity.user.User;
import com.example.main.service.user.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping
    User createUser(@RequestBody UserDto request){

        return userService.createUser(request);
    }
    @GetMapping
    List<User> getUser(){
        return userService.getUser();
    }
    @GetMapping("/{userid}")
    User getUser(@PathVariable("userid") Long userid){
        return userService.getUser(userid);
    }

}
