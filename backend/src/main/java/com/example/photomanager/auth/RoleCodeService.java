package com.example.photomanager.auth;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoleCodeService {
    private final JdbcTemplate jdbcTemplate;

    public RoleCodeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int getRoleCode(String userId) {
        Long uid = parseUserId(userId);
        if (uid == null) {
            return 0;
        }

        // Source of truth: user_rule.rule_code (often numeric "0"/"1").
        String ruleCode = queryRuleCode(uid);
        Integer mapped = mapRuleCode(ruleCode);
        if (mapped != null) {
            return mapped;
        }

        Integer fromUser = queryRoleCode("SELECT role_code FROM user WHERE id=? LIMIT 1", uid);
        return fromUser == null ? 0 : fromUser;
    }

    public void ensureDefaultRoleRow(String userId, int roleCode) {
        Long uid = parseUserId(userId);
        if (uid == null) {
            return;
        }
        // Ensure user_rule has a default numeric role code if missing.
        try {
            String existing = jdbcTemplate.queryForObject(
                    "SELECT rule_code FROM user_rule WHERE user_id=? LIMIT 1",
                    String.class,
                    uid
            );
            if (existing != null && !existing.isBlank()) {
                return;
            }
        } catch (DataAccessException ignored) {
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO user_rule(user_id, rule_code) VALUES(?, ?)",
                    uid,
                    String.valueOf(roleCode)
            );
        } catch (DataAccessException ignored) {
        }
    }

    private Integer queryRoleCode(String sql, Long userId) {
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (DataAccessException ex) {
            return null;
        }
    }

    private String queryRuleCode(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT rule_code FROM user_rule WHERE user_id=? ORDER BY id DESC LIMIT 1",
                    String.class,
                    userId
            );
        } catch (DataAccessException ex) {
            return null;
        }
    }

    private Integer mapRuleCode(String ruleCode) {
        if (ruleCode == null) return null;
        String v = ruleCode.trim();
        if (v.isEmpty()) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ignored) {
        }
        if ("ROLE_ADMIN".equalsIgnoreCase(v) || "ADMIN".equalsIgnoreCase(v)) {
            return 1;
        }
        if ("ROLE_USER".equalsIgnoreCase(v) || "USER".equalsIgnoreCase(v)) {
            return 0;
        }
        return null;
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
