package com.example.photomanager.controller;

import com.example.photomanager.auth.AuthContext;
import com.example.photomanager.model.ChatMessageRequest;
import com.example.photomanager.model.ChatMessageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @PostMapping("/message")
    public ChatMessageResponse receiveMessage(@RequestBody @Valid ChatMessageRequest request) {
        // AuthFilter guarantees the user has been authenticated.
        // For now we only accept and do not process.
        String userId = AuthContext.getUserId();
        if (userId == null || userId.isBlank()) {
            // Should never happen, but keep response predictable.
            return new ChatMessageResponse(false);
        }
        return new ChatMessageResponse(true);
    }
}

