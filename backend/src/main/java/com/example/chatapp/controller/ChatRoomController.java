package com.example.chatapp.controller;

import com.example.chatapp.dto.RoomDtos.CreatePrivateRoomRequest;
import com.example.chatapp.dto.RoomDtos.CreateRoomRequest;
import com.example.chatapp.dto.RoomDtos.RoomResponse;
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
    public RoomResponse createPrivate(@RequestBody CreatePrivateRoomRequest request) {
        String actorId = SecurityUtils.currentUserId();
        ChatRoom room = chatRoomService.createPrivate(request, actorId);
        broadcastRoomUpsert(room);
        return chatRoomService.toResponse(room, actorId);
    }

    @PostMapping("/group")
    public RoomResponse createGroup(@RequestBody CreateRoomRequest request) {
        String actorId = SecurityUtils.currentUserId();
        ChatRoom room = chatRoomService.createGroup(request, actorId);
        broadcastRoomUpsert(room);
        return chatRoomService.toResponse(room, actorId);
    }

    @PostMapping("/forum")
    public RoomResponse createForum(@RequestBody CreateRoomRequest request) {
        String actorId = SecurityUtils.currentUserId();
        ChatRoom room = chatRoomService.createForum(request, actorId);
        broadcastRoomUpsert(room);
        return chatRoomService.toResponse(room, actorId);
    }

    @GetMapping("/forum/default")
    public RoomResponse defaultForum() {
        String actorId = SecurityUtils.currentUserId();
        return chatRoomService.toResponse(chatRoomService.defaultForum(actorId), actorId);
    }

    @GetMapping("/my")
    public List<RoomResponse> myRooms() {
        return chatRoomService.myRoomResponses(SecurityUtils.currentUserId());
    }

    @GetMapping("/private")
    public List<RoomResponse> privateRooms() {
        return chatRoomService.myRoomResponsesByType(SecurityUtils.currentUserId(), ChatRoomType.PRIVATE);
    }

    @GetMapping("/groups")
    public List<RoomResponse> groups() {
        return chatRoomService.myRoomResponsesByType(SecurityUtils.currentUserId(), ChatRoomType.GROUP);
    }

    @GetMapping("/forums")
    public List<RoomResponse> forums() {
        return chatRoomService.myRoomResponsesByType(SecurityUtils.currentUserId(), ChatRoomType.FORUM);
    }

    @GetMapping("/{roomId}")
    public RoomResponse get(@PathVariable String roomId) {
        return chatRoomService.getResponse(roomId, SecurityUtils.currentUserId());
    }

    @PatchMapping("/{roomId}")
    public RoomResponse update(@PathVariable String roomId, @RequestBody UpdateRoomRequest request) {
        String actorId = SecurityUtils.currentUserId();
        ChatRoom room = chatRoomService.update(roomId, request, actorId);
        broadcastRoomUpsert(room);
        return chatRoomService.toResponse(room, actorId);
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
