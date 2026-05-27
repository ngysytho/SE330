package com.example.chatapp.websocket;

import com.example.chatapp.enums.ChatRoomMemberStatus;
import com.example.chatapp.service.ChatRoomMemberService;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomEventPublisher {
    private final ChatRoomMemberService memberService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomEventPublisher(ChatRoomMemberService memberService, SimpMessagingTemplate messagingTemplate) {
        this.memberService = memberService;
        this.messagingTemplate = messagingTemplate;
    }

    public void publishRoomEvent(String roomId, String actorId, Map<String, Object> event) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
        memberService.list(roomId, actorId).stream()
                .filter(member -> member.getStatus() == ChatRoomMemberStatus.ACTIVE)
                .map(member -> member.getUserId())
                .distinct()
                .forEach(userId -> messagingTemplate.convertAndSendToUser(userId, "/queue/rooms", event));
    }

    public void publishTyping(String roomId, String userId, boolean typing) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/typing", Map.of(
                "userId", userId,
                "typing", typing
        ));
    }
}
