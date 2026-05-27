package com.example.chatapp.websocket;

import com.example.chatapp.dto.MessageDtos.CreateMessageRequest;
import com.example.chatapp.dto.MessageDtos.DeleteMessageRequest;
import com.example.chatapp.dto.MessageDtos.TypingRequest;
import com.example.chatapp.dto.MessageDtos.UpdateMessageRequest;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Message;
import com.example.chatapp.security.CustomUserPrincipal;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.PushNotificationService;
import java.security.Principal;
import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {
    private final MessageService messageService;
    private final ChatRoomService roomService;
    private final PushNotificationService pushNotificationService;
    private final RoomEventPublisher roomEventPublisher;

    public ChatWebSocketController(MessageService messageService, ChatRoomService roomService, PushNotificationService pushNotificationService, RoomEventPublisher roomEventPublisher) {
        this.messageService = messageService;
        this.roomService = roomService;
        this.pushNotificationService = pushNotificationService;
        this.roomEventPublisher = roomEventPublisher;
    }

    @MessageMapping("/chat.sendMessage")
    public void send(CreateMessageRequest request, Principal principal, SimpMessageHeaderAccessor headers) {
        String actorId = currentUser(principal, headers);
        Message message = messageService.send(request.roomId(), request, actorId);
        ChatRoom room = roomService.get(request.roomId(), actorId);
        roomEventPublisher.publishRoomEvent(request.roomId(), actorId, Map.of("type", "MESSAGE_CREATED", "roomId", request.roomId(), "message", message, "room", room));
        pushNotificationService.notifyNewMessage(room, message, actorId);
    }

    @MessageMapping("/chat.editMessage")
    public void edit(UpdateMessageRequest request, Principal principal, SimpMessageHeaderAccessor headers) {
        String actorId = currentUser(principal, headers);
        Message message = messageService.update(request.messageId(), request, actorId);
        roomEventPublisher.publishRoomEvent(message.getRoomId(), actorId, Map.of("type", "MESSAGE_UPDATED", "roomId", message.getRoomId(), "message", message));
    }

    @MessageMapping("/chat.deleteMessage")
    public void delete(DeleteMessageRequest request, Principal principal, SimpMessageHeaderAccessor headers) {
        String actorId = currentUser(principal, headers);
        Message message = messageService.delete(request.messageId(), actorId);
        roomEventPublisher.publishRoomEvent(message.getRoomId(), actorId, Map.of("type", "MESSAGE_DELETED", "roomId", message.getRoomId(), "messageId", request.messageId()));
    }

    @MessageMapping("/chat.typing")
    public void typing(TypingRequest request, Principal principal, SimpMessageHeaderAccessor headers) {
        String userId = currentUserOrNull(principal, headers);
        if (userId == null) {
            return;
        }
        roomEventPublisher.publishTyping(request.roomId(), userId, Boolean.TRUE.equals(request.typing()));
    }

    private String currentUser(Principal principal, SimpMessageHeaderAccessor headers) {
        String userId = currentUserOrNull(principal, headers);
        if (userId == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return userId;
    }

    private String currentUserOrNull(Principal principal, SimpMessageHeaderAccessor headers) {
        if (principal instanceof CustomUserPrincipal userPrincipal) {
            return userPrincipal.id();
        }
        if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof CustomUserPrincipal userPrincipal) {
            return userPrincipal.id();
        }
        if (headers != null && headers.getSessionAttributes() != null) {
            Object stored = headers.getSessionAttributes().get(WebSocketAuthChannelInterceptor.SESSION_USER_ATTRIBUTE);
            if (stored instanceof CustomUserPrincipal userPrincipal) {
                return userPrincipal.id();
            }
        }
        return null;
    }

}
