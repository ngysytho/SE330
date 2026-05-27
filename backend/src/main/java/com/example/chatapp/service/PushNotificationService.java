package com.example.chatapp.service;

import com.example.chatapp.enums.ChatRoomMemberStatus;
import com.example.chatapp.enums.ChatRoomType;
import com.example.chatapp.enums.MessageType;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.ChatRoomMember;
import com.example.chatapp.model.Message;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService {
    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final ChatRoomMemberService memberService;
    private final UserService userService;
    private final FcmPushGateway fcmPushGateway;

    public PushNotificationService(ChatRoomMemberService memberService, UserService userService, FcmPushGateway fcmPushGateway) {
        this.memberService = memberService;
        this.userService = userService;
        this.fcmPushGateway = fcmPushGateway;
    }

    public void notifyNewMessage(ChatRoom room, Message message, String senderId) {
        log.info("[PUSH] New message push requested roomId={} roomType={} messageId={} senderId={}",
                room.getId(), room.getType(), message.getId(), senderId);
        if (room.getType() != ChatRoomType.PRIVATE && room.getType() != ChatRoomType.GROUP) {
            log.info("[PUSH] Skip roomId={} because roomType={} is not push-enabled", room.getId(), room.getType());
            return;
        }

        List<ChatRoomMember> recipients = memberService.list(room.getId(), senderId).stream()
                .filter(member -> member.getStatus() == ChatRoomMemberStatus.ACTIVE)
                .filter(member -> !senderId.equals(member.getUserId()))
                .toList();
        log.info("[PUSH] roomId={} active recipients excluding sender={}", room.getId(), recipients.size());
        if (recipients.isEmpty()) {
            log.info("[PUSH] Skip roomId={} because no recipient remains after filtering sender", room.getId());
            return;
        }

        Map<String, String> tokenOwners = new LinkedHashMap<>();
        recipients.forEach(member -> {
            List<String> memberTokens = userService.fcmTokens(member.getUserId());
            log.info("[PUSH] recipient userId={} fcmTokenCount={}", member.getUserId(), memberTokens.size());
            memberTokens.forEach(token -> tokenOwners.putIfAbsent(token, member.getUserId()));
        });
        log.info("[PUSH] roomId={} unique FCM tokens to send={}", room.getId(), tokenOwners.size());
        if (tokenOwners.isEmpty()) {
            log.info("[PUSH] Skip roomId={} because recipients have no registered FCM tokens", room.getId());
            return;
        }

        String title = title(room, message);
        String body = body(room, message);
        Map<String, String> data = data(room, message, title, body);
        log.info("[PUSH] Sending notification title='{}' body='{}' roomId={} messageId={}",
                title, body, room.getId(), message.getId());
        List<String> tokens = new ArrayList<>(tokenOwners.keySet());
        fcmPushGateway.sendNewMessageNotification(tokens, tokenOwners, data);
    }

    private String title(ChatRoom room, Message message) {
        if (room.getType() == ChatRoomType.PRIVATE) {
            return safe(message.getSenderName(), "Tin nhắn mới");
        }
        return safe(room.getName(), "Nhóm chat");
    }

    private String body(ChatRoom room, Message message) {
        String content = preview(message);
        if (room.getType() == ChatRoomType.GROUP) {
            return trim(safe(message.getSenderName(), "Ai đó") + ": " + content, 160);
        }
        return trim(content, 160);
    }

    private String preview(Message message) {
        if (message.getContent() != null && !message.getContent().isBlank()) {
            return message.getContent().trim();
        }
        if (message.getMessageType() == MessageType.IMAGE) {
            return "Đã gửi hình ảnh";
        }
        if (message.getAttachmentIds() != null && !message.getAttachmentIds().isEmpty()) {
            return "Đã gửi tệp đính kèm";
        }
        return "Tin nhắn mới";
    }

    private Map<String, String> data(ChatRoom room, Message message, String title, String body) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "MESSAGE_CREATED");
        data.put("title", title);
        data.put("body", body);
        data.put("roomId", room.getId());
        data.put("roomType", room.getType().name());
        data.put("messageId", message.getId());
        data.put("senderId", message.getSenderId());
        data.put("senderName", safe(message.getSenderName(), ""));
        data.put("url", "/chat?roomId=" + room.getId());
        data.put("createdAt", String.valueOf(message.getCreatedAt() == null ? System.currentTimeMillis() : message.getCreatedAt()));
        return data;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trim(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "...";
    }

}
