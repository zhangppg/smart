package com.example.photomanager.auth;

import com.example.photomanager.auth.entity.AppUserEntity;
import com.example.photomanager.auth.entity.UserRuleEntity;
import com.example.photomanager.auth.repository.AppUserRepository;
import com.example.photomanager.auth.repository.UserRuleRepository;
import com.example.photomanager.model.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class UserService {
    // user_rule.rule_code stores numeric role codes in this project:
    // 0 = super (all photos + manage), 1 = read-only (all photos), 3 = normal (own photos + manage own).
    private static final String DEFAULT_RULE = "3";

    private final AppUserRepository appUserRepository;
    private final UserRuleRepository userRuleRepository;
    private final RoleCodeService roleCodeService;

    public UserService(AppUserRepository appUserRepository,
                       UserRuleRepository userRuleRepository,
                       RoleCodeService roleCodeService) {
        this.appUserRepository = appUserRepository;
        this.userRuleRepository = userRuleRepository;
        this.roleCodeService = roleCodeService;
    }

    @Transactional
    public User register(String username, String rawPassword, Integer roleCode) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(rawPassword);

        if (appUserRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        int normalizedRole = normalizeRoleCode(roleCode);

        AppUserEntity entity = new AppUserEntity();
        entity.setUsername(normalizedUsername);
        entity.setPasswordHash(hashPassword(rawPassword));
        entity.setRoleCode(normalizedRole);
        AppUserEntity saved = appUserRepository.save(entity);

        UserRuleEntity userRule = new UserRuleEntity();
        userRule.setUserId(saved.getId());
        userRule.setRuleCode(String.valueOf(normalizedRole));
        userRuleRepository.save(userRule);

        return toModel(saved);
    }

    @Transactional(readOnly = true)
    public User authenticate(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(rawPassword);

        AppUserEntity entity = appUserRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!entity.getPasswordHash().equals(hashPassword(rawPassword))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return toModel(entity);
    }

    private User toModel(AppUserEntity entity) {
        User user = new User();
        user.setId(String.valueOf(entity.getId()));
        user.setUsername(entity.getUsername());
        user.setPasswordHash(entity.getPasswordHash());
        // Source of truth: user_role.role_code (if present); fallback to user.role_code.
        user.setRoleCode(roleCodeService.getRoleCode(String.valueOf(entity.getId())));
        user.setCreatedAt(entity.getCreatedAt());
        return user;
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        return username.trim().toLowerCase();
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }
    }

    private int normalizeRoleCode(Integer roleCode) {
        if (roleCode == null) {
            return Integer.parseInt(DEFAULT_RULE);
        }
        if (roleCode == 0 || roleCode == 1 || roleCode == 3) {
            return roleCode;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid roleCode (allowed: 0, 1, 3)");
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
