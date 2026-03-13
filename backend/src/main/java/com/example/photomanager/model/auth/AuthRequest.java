package com.example.photomanager.model.auth;

import jakarta.validation.constraints.NotBlank;

public class AuthRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    // Optional for login; for register we default to 3 (normal user) if omitted.
    private Integer roleCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(Integer roleCode) {
        this.roleCode = roleCode;
    }
}
