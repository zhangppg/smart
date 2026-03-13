package com.example.photomanager.model.auth;

public class AuthResponse {
    private String token;
    private String username;
    private int roleCode;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, int roleCode) {
        this.token = token;
        this.username = username;
        this.roleCode = roleCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(int roleCode) {
        this.roleCode = roleCode;
    }
}
