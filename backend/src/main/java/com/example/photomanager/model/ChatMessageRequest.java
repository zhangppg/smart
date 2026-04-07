package com.example.photomanager.model;

import jakarta.validation.constraints.NotBlank;

public class ChatMessageRequest {
    @NotBlank
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

