package com.example.photomanager.controller;

import com.example.photomanager.auth.AuthContext;
import com.example.photomanager.model.ChatMessageRequest;
import com.example.photomanager.model.ChatMessageResponse;
import com.example.photomanager.service.N8nWebhookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final N8nWebhookService n8nWebhookService;

    public ChatController(N8nWebhookService n8nWebhookService) {
        this.n8nWebhookService = n8nWebhookService;
    }

    @PostMapping("/message")
    public ChatMessageResponse receiveMessage(@RequestBody @Valid ChatMessageRequest request) {
        // AuthFilter guarantees the user has been authenticated.
        // For now we only accept and do not process.
        String userId = AuthContext.getUserId();
        if (userId == null || userId.isBlank()) {
            // Should never happen, but keep response predictable.
            return new ChatMessageResponse(false);
        }

        n8nWebhookService.sendToN8n(userId, request.getText());
        return new ChatMessageResponse(true);
    }
}
