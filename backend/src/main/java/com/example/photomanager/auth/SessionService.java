package com.example.photomanager.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    private static final long TOKEN_EXPIRE_SECONDS = 7L * 24 * 60 * 60;

    private static class Session {
        private final String userId;
        private final Instant createdAt;

        private Session(String userId, Instant createdAt) {
            this.userId = userId;
            this.createdAt = createdAt;
        }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String createToken(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(userId, Instant.now()));
        return token;
    }

    public String validateAndGetUserId(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.createdAt.plusSeconds(TOKEN_EXPIRE_SECONDS).isBefore(Instant.now())) {
            sessions.remove(token);
            return null;
        }
        return session.userId;
    }
}
