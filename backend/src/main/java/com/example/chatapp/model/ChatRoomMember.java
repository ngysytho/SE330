package com.example.chatapp.model;

import com.example.chatapp.enums.ChatRoomMemberRole;
import com.example.chatapp.enums.ChatRoomMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMember {
    private String id;
    private String roomId;
    private String userId;
    private ChatRoomMemberRole role;
    private ChatRoomMemberStatus status;
    private String lastReadMessageId;
    private Long lastReadAt;
    private Long joinedAt;
    private Long leftAt;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
