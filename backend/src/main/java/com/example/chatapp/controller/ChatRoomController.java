package com.example.chatapp.controller;

import com.example.chatapp.dto.RoomDtos.CreatePrivateRoomRequest;
import com.example.chatapp.dto.RoomDtos.CreateRoomRequest;
import com.example.chatapp.dto.RoomDtos.UpdateRoomRequest;
import com.example.chatapp.enums.ChatRoomType;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.security.SecurityUtils;
import com.example.chatapp.service.ChatRoomService;
import java.util.Collections;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatRoomController(ChatRoomService chatRoomService, SimpMessagingTemplate messagingTemplate) {
        this.chatRoomService = chatRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/private")
    public ChatRoom createPrivate(@RequestBody CreatePrivateRoomRequest request) {
        ChatRoom room = chatRoomService.createPrivate(request, SecurityUtils.currentUserId());
        broadcastRoomUpsert(room);
        return room;
    }

    @PostMapping("/group")
    public ChatRoom createGroup(@RequestBody CreateRoomRequest request) {
        ChatRoom room = chatRoomService.createGroup(request, SecurityUtils.currentUserId());
        broadcastRoomUpsert(room);
        return room;
    }

    @PostMapping("/forum")
    public ChatRoom createForum(@RequestBody CreateRoomRequest request) {
        ChatRoom room = chatRoomService.createForum(request, SecurityUtils.currentUserId());
        broadcastRoomUpsert(room);
        return room;
    }

    @GetMapping("/forum/default")
    public ChatRoom defaultForum() {
        return chatRoomService.defaultForum(SecurityUtils.currentUserId());
    }

    @GetMapping("/my")
    public List<ChatRoom> myRooms() {
        return chatRoomService.myRooms(SecurityUtils.currentUserId());
    }

    @GetMapping("/private")
    public List<ChatRoom> privateRooms() {
        return chatRoomService.myRoomsByType(SecurityUtils.currentUserId(), ChatRoomType.PRIVATE);
    }

    @GetMapping("/groups")
    public List<ChatRoom> groups() {
        return chatRoomService.myRoomsByType(SecurityUtils.currentUserId(), ChatRoomType.GROUP);
    }

    @GetMapping("/forums")
    public List<ChatRoom> forums() {
        return chatRoomService.myRoomsByType(SecurityUtils.currentUserId(), ChatRoomType.FORUM);
    }

    @GetMapping("/{roomId}")
    public ChatRoom get(@PathVariable String roomId) {
        return chatRoomService.get(roomId, SecurityUtils.currentUserId());
    }

    @PatchMapping("/{roomId}")
    public ChatRoom update(@PathVariable String roomId, @RequestBody UpdateRoomRequest request) {
        ChatRoom room = chatRoomService.update(roomId, request, SecurityUtils.currentUserId());
        broadcastRoomUpsert(room);
        return room;
    }

    @DeleteMapping("/{roomId}")
    public void delete(@PathVariable String roomId) {
        String actorId = SecurityUtils.currentUserId();
        ChatRoom room = chatRoomService.get(roomId, actorId);
        chatRoomService.delete(roomId, actorId);
        broadcastRoomDelete(room);
    }

    private void broadcastRoomUpsert(ChatRoom room) {
        Map<String, Object> event = Map.of("type", "ROOM_UPSERTED", "roomId", room.getId(), "room", room);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), event);
        memberIds(room).stream()
                .distinct()
                .forEach(userId -> messagingTemplate.convertAndSendToUser(userId, "/queue/rooms", event));
    }

    private void broadcastRoomDelete(ChatRoom room) {
        Map<String, Object> event = Map.of("type", "ROOM_DELETED", "roomId", room.getId());
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), event);
        memberIds(room).stream()
                .distinct()
                .forEach(userId -> messagingTemplate.convertAndSendToUser(userId, "/queue/rooms", event));
    }

    private List<String> memberIds(ChatRoom room) {
        return room.getMemberIds() == null ? Collections.emptyList() : room.getMemberIds();
    }
}
