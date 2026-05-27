package com.example.chatapp.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {
    private final FirebaseService firebase;
    private final Map<String, Set<String>> activeSessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, String> userBySession = new ConcurrentHashMap<>();

    public PresenceService(FirebaseService firebase) {
        this.firebase = firebase;
    }

    public boolean online(String userId, String sessionId) {
        String safeSessionId = safeSessionId(sessionId);
        Set<String> sessions = activeSessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = sessions.isEmpty();
        sessions.add(safeSessionId);
        userBySession.put(safeSessionId, userId);
        long now = firebase.now();
        firebase.save(FirebaseService.PRESENCE, userId, Map.of("userId", userId, "isOnline", true, "lastSeenAt", now));
        firebase.update(FirebaseService.USERS, userId, Map.of("isOnline", true, "lastSeenAt", now, "updatedAt", now));
        return wasOffline;
    }

    public boolean offline(String userId, String sessionId) {
        String safeSessionId = safeSessionId(sessionId);
        userBySession.remove(safeSessionId);
        Set<String> sessions = activeSessionsByUser.get(userId);
        if (sessions != null) {
            sessions.remove(safeSessionId);
            if (sessions.isEmpty()) {
                activeSessionsByUser.remove(userId);
            }
        }
        boolean nowOffline = sessions == null || sessions.isEmpty();
        if (!nowOffline) {
            return false;
        }
        long now = firebase.now();
        firebase.save(FirebaseService.PRESENCE, userId, Map.of("userId", userId, "isOnline", false, "lastSeenAt", now));
        firebase.update(FirebaseService.USERS, userId, Map.of("isOnline", false, "lastSeenAt", now, "updatedAt", now));
        return true;
    }

    public boolean offlineSession(String sessionId) {
        String safeSessionId = safeSessionId(sessionId);
        String userId = userBySession.get(safeSessionId);
        if (userId == null) {
            return false;
        }
        return offline(userId, safeSessionId);
    }

    public String userIdForSession(String sessionId) {
        return userBySession.get(safeSessionId(sessionId));
    }

    public boolean isOnline(String userId) {
        Set<String> sessions = activeSessionsByUser.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    private String safeSessionId(String sessionId) {
        return sessionId == null ? "unknown" : sessionId;
    }
}
