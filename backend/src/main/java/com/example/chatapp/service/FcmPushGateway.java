package com.example.chatapp.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmPushGateway {
    private static final Logger log = LoggerFactory.getLogger(FcmPushGateway.class);
    private static final int FCM_BATCH_SIZE = 500;

    private final UserService userService;

    public FcmPushGateway(UserService userService) {
        this.userService = userService;
    }

    public void sendNewMessageNotification(List<String> tokens, Map<String, String> tokenOwners, Map<String, String> data) {
        for (int start = 0; start < tokens.size(); start += FCM_BATCH_SIZE) {
            List<String> batchTokens = tokens.subList(start, Math.min(start + FCM_BATCH_SIZE, tokens.size()));
            sendBatch(batchTokens, tokenOwners, data);
        }
    }

    private void sendBatch(List<String> tokens, Map<String, String> tokenOwners, Map<String, String> data) {
        MulticastMessage notification = MulticastMessage.builder()
                .addAllTokens(tokens)
                .putAllData(data)
                .setWebpushConfig(webpush(data))
                .build();
        try {
            log.info("[PUSH] Sending FCM batch size={} tokens={}", tokens.size(), maskedTokens(tokens));
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(notification);
            log.info("[PUSH] FCM batch result successCount={} failureCount={}", response.getSuccessCount(), response.getFailureCount());
            List<SendResponse> responses = response.getResponses();
            for (int index = 0; index < responses.size(); index++) {
                SendResponse item = responses.get(index);
                if (item.isSuccessful()) {
                    log.info("[PUSH] FCM token sent token={} userId={} messageId={}",
                            maskToken(tokens.get(index)),
                            tokenOwners.get(tokens.get(index)),
                            data.get("messageId"));
                } else {
                    log.warn("[PUSH] FCM token failed token={} userId={} errorCode={} message={}",
                            maskToken(tokens.get(index)),
                            tokenOwners.get(tokens.get(index)),
                            item.getException() == null ? null : item.getException().getMessagingErrorCode(),
                            item.getException() == null ? "unknown" : item.getException().getMessage());
                    removeDeadToken(tokenOwners.get(tokens.get(index)), tokens.get(index), item.getException());
                }
            }
        } catch (FirebaseMessagingException error) {
            log.warn("[PUSH] Could not send FCM message notification tokens={}", maskedTokens(tokens), error);
        }
    }

    private WebpushConfig webpush(Map<String, String> data) {
        return WebpushConfig.builder()
                .putHeader("TTL", "86400")
                .setNotification(WebpushNotification.builder()
                        .setTitle(data.get("title"))
                        .setBody(data.get("body"))
                        .setIcon("/logo192.png")
                        .setBadge("/logo192.png")
                        .setTag(data.get("messageId"))
                        .putCustomData("url", data.get("url"))
                        .putCustomData("roomId", data.get("roomId"))
                        .build())
                .build();
    }

    private void removeDeadToken(String userId, String token, FirebaseMessagingException error) {
        MessagingErrorCode code = error == null ? null : error.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
            if (userId == null) {
                return;
            }
            try {
                userService.removeFcmToken(userId, token);
                log.info("[PUSH] Removed stale FCM token={} for userId={}", maskToken(token), userId);
            } catch (RuntimeException cleanupError) {
                log.warn("Could not remove stale FCM token for user {}", userId, cleanupError);
            }
            return;
        }
        log.warn("FCM token failed for user {}: {}", userId, error == null ? "unknown" : error.getMessage());
    }

    private List<String> maskedTokens(List<String> tokens) {
        return tokens.stream().map(this::maskToken).toList();
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
