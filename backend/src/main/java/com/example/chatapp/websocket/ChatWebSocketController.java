package com.example.chatapp.websocket;

import com.example.chatapp.dto.MessageDtos.CreateMessageRequest;
import com.example.chatapp.dto.MessageDtos.DeleteMessageRequest;
import com.example.chatapp.dto.MessageDtos.TypingRequest;
import com.example.chatapp.dto.MessageDtos.UpdateMessageRequest;
import com.example.chatapp.enums.ChatRoomMemberStatus;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Message;
import com.example.chatapp.security.CustomUserPrincipal;
import com.example.chatapp.service.ChatRoomMemberService;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.MessageService;
import java.security.Principal;
import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {
    private final MessageService messageService;
    private final ChatRoomMemberService memberService;
    private final ChatRoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService, ChatRoomMemberService memberService, ChatRoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.memberService = memberService;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    public void send(CreateMessageRequest request, Principal principal) {
        String actorId = currentUser(principal);
        Message message = messageService.send(request.roomId(), request, actorId);
        ChatRoom room = roomService.get(request.roomId(), actorId);
        broadcastRoomEvent(request.roomId(), actorId, Map.of("type", "MESSAGE_CREATED", "roomId", request.roomId(), "message", message, "room", room));
    }

    @MessageMapping("/chat.editMessage")
    public void edit(UpdateMessageRequest request, Principal principal) {
        String actorId = currentUser(principal);
        Message message = messageService.update(request.messageId(), request, actorId);
        broadcastRoomEvent(message.getRoomId(), actorId, Map.of("type", "MESSAGE_UPDATED", "roomId", message.getRoomId(), "message", message));
    }

    @MessageMapping("/chat.deleteMessage")
    public void delete(DeleteMessageRequest request, Principal principal) {
        String actorId = currentUser(principal);
        Message message = messageService.delete(request.messageId(), actorId);
        broadcastRoomEvent(message.getRoomId(), actorId, Map.of("type", "MESSAGE_DELETED", "roomId", message.getRoomId(), "messageId", request.messageId()));
    }

    @MessageMapping("/chat.typing")
    public void typing(TypingRequest request, Principal principal) {
        messagingTemplate.convertAndSend("/topic/rooms/" + request.roomId() + "/typing", Map.of(
                "userId", currentUser(principal),
                "typing", Boolean.TRUE.equals(request.typing())
        ));
    }

    private String currentUser(Principal principal) {
        if (principal instanceof CustomUserPrincipal userPrincipal) {
            return userPrincipal.id();
        }
        throw new IllegalArgumentException("Authentication required");
    }

    private void broadcastRoomEvent(String roomId, String actorId, Map<String, Object> event) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
        memberService.list(roomId, actorId).stream()
                .filter(member -> member.getStatus() == ChatRoomMemberStatus.ACTIVE)
                .map(member -> member.getUserId())
                .distinct()
                .forEach(userId -> messagingTemplate.convertAndSendToUser(userId, "/queue/rooms", event));
    }
}
