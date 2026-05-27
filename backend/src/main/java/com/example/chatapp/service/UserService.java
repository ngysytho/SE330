package com.example.chatapp.service;

import com.example.chatapp.dto.UserDtos.UpdateUserRequest;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_FCM_TOKENS_PER_USER = 20;

    private final FirebaseService firebase;

    public UserService(FirebaseService firebase) {
        this.firebase = firebase;
    }

    public User get(String id) {
        User user = firebase.get(FirebaseService.USERS, id, User.class);
        if (user.getDeletedAt() != null) {
            throw new com.example.chatapp.exception.ResourceNotFoundException("User not found");
        }
        return user;
    }

    public User update(String id, UpdateUserRequest request) {
        Map<String, Object> updates = new HashMap<>();
        put(updates, "name", request.name());
        put(updates, "phoneNumber", request.phoneNumber());
        put(updates, "address", request.address());
        put(updates, "avatarImage", request.avatarImage());
        put(updates, "birthday", request.birthday());
        put(updates, "gender", request.gender());
        put(updates, "note", request.note());
        updates.put("updatedAt", firebase.now());
        firebase.update(FirebaseService.USERS, id, updates);
        return get(id);
    }

    public User registerFcmToken(String id, String token) {
        String sanitized = sanitizeToken(token);
        User user = get(id);
        List<String> tokens = new ArrayList<>(safeTokens(user));
        tokens.remove(sanitized);
        tokens.add(0, sanitized);
        if (tokens.size() > MAX_FCM_TOKENS_PER_USER) {
            tokens = new ArrayList<>(tokens.subList(0, MAX_FCM_TOKENS_PER_USER));
        }
        long now = firebase.now();
        firebase.update(FirebaseService.USERS, id, Map.of("fcmTokens", tokens, "updatedAt", now));
        user.setFcmTokens(tokens);
        user.setUpdatedAt(now);
        log.info("[PUSH] Registered FCM token={} for userId={} totalTokens={}", maskToken(sanitized), id, tokens.size());
        return user;
    }

    public void removeFcmToken(String id, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        User user = get(id);
        List<String> tokens = new ArrayList<>(safeTokens(user));
        if (!tokens.remove(token.trim())) {
            log.info("[PUSH] FCM token={} not found for userId={} during removal", maskToken(token), id);
            return;
        }
        firebase.update(FirebaseService.USERS, id, Map.of("fcmTokens", tokens, "updatedAt", firebase.now()));
        log.info("[PUSH] Removed FCM token={} for userId={} remainingTokens={}", maskToken(token), id, tokens.size());
    }

    public List<String> fcmTokens(String id) {
        return safeTokens(get(id));
    }

    public List<User> search(String keyword) {
        String lower = keyword == null ? "" : keyword.toLowerCase();
        return firebase.all(FirebaseService.USERS, User.class).stream()
                .filter(user -> user.getDeletedAt() == null)
                .filter(user -> lower.isBlank()
                        || contains(user.getName(), lower)
                        || contains(user.getGmail(), lower)
                        || contains(user.getPhoneNumber(), lower))
                .limit(30)
                .toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void put(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private List<String> safeTokens(User user) {
        if (user.getFcmTokens() == null) {
            return Collections.emptyList();
        }
        return user.getFcmTokens().stream()
                .filter(token -> token != null && !token.isBlank())
                .distinct()
                .toList();
    }

    private String sanitizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("FCM token is required");
        }
        return token.trim();
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "<blank>";
        }
        String trimmed = token.trim();
        if (trimmed.length() <= 16) {
            return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
        }
        return trimmed.substring(0, 8) + "..." + trimmed.substring(trimmed.length() - 6);
    }
}
