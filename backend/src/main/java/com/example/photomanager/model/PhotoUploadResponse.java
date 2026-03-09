package com.example.photomanager.model;

public class PhotoUploadResponse {
    private String message;
    private PhotoItem photo;

    public PhotoUploadResponse() {
    }

    public PhotoUploadResponse(String message, PhotoItem photo) {
        this.message = message;
        this.photo = photo;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PhotoItem getPhoto() {
        return photo;
    }

    public void setPhoto(PhotoItem photo) {
        this.photo = photo;
    }
}
