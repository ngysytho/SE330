package com.example.chatapp.controller;

import com.example.chatapp.dto.MemberDtos.AddMemberRequest;
import com.example.chatapp.dto.MemberDtos.MemberResponse;
import com.example.chatapp.dto.MemberDtos.UpdateMemberRoleRequest;
import com.example.chatapp.dto.MemberDtos.UpdateMemberStatusRequest;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.ChatRoomMember;
import com.example.chatapp.security.SecurityUtils;
import com.example.chatapp.service.ChatRoomMemberService;
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
@RequestMapping("/api/rooms/{roomId}/members")
public class ChatRoomMemberController {
    private final ChatRoomMemberService memberService;
    private final ChatRoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatRoomMemberController(ChatRoomMemberService memberService, ChatRoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.memberService = memberService;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ChatRoomMember add(@PathVariable String roomId, @RequestBody AddMemberRequest request) {
        String actorId = SecurityUtils.currentUserId();
        ChatRoomMember member = memberService.addMember(roomId, request.userId(), actorId);
        broadcastRoomUpsert(roomService.get(roomId, actorId));
        return member;
    }

    @GetMapping
    public List<MemberResponse> list(@PathVariable String roomId) {
        return memberService.listWithUsers(roomId, SecurityUtils.currentUserId());
    }

    @PatchMapping("/{userId}/role")
    public ChatRoomMember role(@PathVariable String roomId, @PathVariable String userId, @RequestBody UpdateMemberRoleRequest request) {
        return memberService.updateRole(roomId, userId, request.role(), SecurityUtils.currentUserId());
    }

    @PatchMapping("/{userId}/status")
    public ChatRoomMember status(@PathVariable String roomId, @PathVariable String userId, @RequestBody UpdateMemberStatusRequest request) {
        return memberService.updateStatus(roomId, userId, request.status(), SecurityUtils.currentUserId());
    }

    @DeleteMapping("/{userId}")
    public void remove(@PathVariable String roomId, @PathVariable String userId) {
        String actorId = SecurityUtils.currentUserId();
        memberService.removeMember(roomId, userId, actorId);
        Map<String, Object> removedEvent = Map.of("type", "ROOM_DELETED", "roomId", roomId);
        messagingTemplate.convertAndSendToUser(userId, "/queue/rooms", removedEvent);
        broadcastRoomUpsert(roomService.get(roomId, actorId));
    }

    private void broadcastRoomUpsert(ChatRoom room) {
        Map<String, Object> event = Map.of("type", "ROOM_UPSERTED", "roomId", room.getId(), "room", room);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), event);
        memberIds(room).stream()
                .distinct()
                .forEach(userId -> messagingTemplate.convertAndSendToUser(userId, "/queue/rooms", event));
    }

    private List<String> memberIds(ChatRoom room) {
        return room.getMemberIds() == null ? Collections.emptyList() : room.getMemberIds();
    }
}
