package com.example.chatapp.controller;

import com.example.chatapp.dto.MessageDtos.CreateMessageRequest;
import com.example.chatapp.dto.MessageDtos.UpdateMessageRequest;
import com.example.chatapp.enums.ChatRoomMemberStatus;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Message;
import com.example.chatapp.security.SecurityUtils;
import com.example.chatapp.service.ChatRoomMemberService;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.MessageService;
import java.util.List;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {
    private final MessageService messageService;
    private final ChatRoomMemberService memberService;
    private final ChatRoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(MessageService messageService, ChatRoomMemberService memberService, ChatRoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.memberService = memberService;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/api/rooms/{roomId}/messages")
    public List<Message> list(@PathVariable String roomId, @RequestParam(defaultValue = "50") int pageSize, @RequestParam(required = false) String lastMessageId) {
        return messageService.list(roomId, pageSize, lastMessageId, SecurityUtils.currentUserId());
    }

    @PostMapping("/api/rooms/{roomId}/messages")
    public Message send(@PathVariable String roomId, @RequestBody CreateMessageRequest request) {
        String actorId = SecurityUtils.currentUserId();
        Message message = messageService.send(roomId, request, actorId);
        ChatRoom room = roomService.get(roomId, actorId);
        broadcastRoomEvent(roomId, actorId, Map.of("type", "MESSAGE_CREATED", "roomId", roomId, "message", message, "room", room));
        return message;
    }

    @PatchMapping("/api/messages/{messageId}")
    public Message update(@PathVariable String messageId, @RequestBody UpdateMessageRequest request) {
        String actorId = SecurityUtils.currentUserId();
        Message message = messageService.update(messageId, request, actorId);
        broadcastRoomEvent(message.getRoomId(), actorId, Map.of("type", "MESSAGE_UPDATED", "roomId", message.getRoomId(), "message", message));
        return message;
    }

    @DeleteMapping("/api/messages/{messageId}")
    public void delete(@PathVariable String messageId) {
        String actorId = SecurityUtils.currentUserId();
        Message message = messageService.delete(messageId, actorId);
        broadcastRoomEvent(message.getRoomId(), actorId, Map.of("type", "MESSAGE_DELETED", "roomId", message.getRoomId(), "messageId", messageId));
    }

    @PostMapping("/api/rooms/{roomId}/read/{messageId}")
    public void read(@PathVariable String roomId, @PathVariable String messageId) {
        memberService.markRead(roomId, messageId, SecurityUtils.currentUserId());
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
