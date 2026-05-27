package com.example.chatapp.controller;

import com.example.chatapp.dto.AuthDtos.UserResponse;
import com.example.chatapp.dto.UserDtos.FcmTokenRequest;
import com.example.chatapp.dto.UserDtos.UpdateUserRequest;
import com.example.chatapp.security.SecurityUtils;
import com.example.chatapp.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(userService.get(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.update(SecurityUtils.currentUserId(), request));
    }

    @PostMapping("/me/fcm-token")
    public UserResponse registerFcmToken(@RequestBody FcmTokenRequest request) {
        return UserResponse.from(userService.registerFcmToken(SecurityUtils.currentUserId(), request.token()));
    }

    @DeleteMapping("/me/fcm-token")
    public void removeFcmToken(@RequestBody FcmTokenRequest request) {
        userService.removeFcmToken(SecurityUtils.currentUserId(), request.token());
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String id) {
        return UserResponse.from(userService.get(id));
    }

    @GetMapping("/search")
    public List<UserResponse> search(@RequestParam(defaultValue = "") String keyword) {
        return userService.search(keyword).stream().map(UserResponse::from).toList();
    }
}
