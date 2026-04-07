package com.example.photomanager.model;

public class ChatMessageResponse {
    private boolean ok;

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(boolean ok) {
        this.ok = ok;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }
}

