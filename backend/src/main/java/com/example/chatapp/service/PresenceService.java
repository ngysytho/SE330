package com.example.chatapp.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {
    private final FirebaseService firebase;
    private final Map<String, Set<String>> activeSessionsByUser = new ConcurrentHashMap<>();

    public PresenceService(FirebaseService firebase) {
        this.firebase = firebase;
    }

    public boolean online(String userId, String sessionId) {
        Set<String> sessions = activeSessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = sessions.isEmpty();
        sessions.add(sessionId == null ? "unknown" : sessionId);
        long now = firebase.now();
        firebase.save(FirebaseService.PRESENCE, userId, Map.of("userId", userId, "isOnline", true, "lastSeenAt", now));
        firebase.update(FirebaseService.USERS, userId, Map.of("isOnline", true, "lastSeenAt", now, "updatedAt", now));
        return wasOffline;
    }

    public boolean offline(String userId, String sessionId) {
        Set<String> sessions = activeSessionsByUser.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId == null ? "unknown" : sessionId);
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

    public boolean isOnline(String userId) {
        Set<String> sessions = activeSessionsByUser.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
}
