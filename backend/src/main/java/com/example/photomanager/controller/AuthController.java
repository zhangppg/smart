package com.example.photomanager.controller;

import com.example.photomanager.auth.SessionService;
import com.example.photomanager.auth.UserService;
import com.example.photomanager.model.auth.AuthRequest;
import com.example.photomanager.model.auth.AuthResponse;
import com.example.photomanager.model.auth.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final SessionService sessionService;

    public AuthController(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody @Valid AuthRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        String token = sessionService.createToken(user.getId());
        return new AuthResponse(token, user.getUsername());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        User user = userService.authenticate(request.getUsername(), request.getPassword());
        String token = sessionService.createToken(user.getId());
        return new AuthResponse(token, user.getUsername());
    }
}
