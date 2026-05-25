package com.example.chatapp.dto;

import com.example.chatapp.enums.ChatRoomType;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.ChatRoomMember;
import java.util.List;

public final class RoomDtos {
    private RoomDtos() {
    }

    public record CreatePrivateRoomRequest(String otherUserId) {
    }

    public record CreateRoomRequest(
            String name,
            String description,
            String avatarUrl,
            Boolean isPublic,
            List<String> memberIds
    ) {
    }

    public record UpdateRoomRequest(
            String name,
            String description,
            String avatarUrl,
            Boolean isPublic
    ) {
    }

    public record RoomResponse(
            String id,
            String name,
            String description,
            ChatRoomType type,
            String avatarUrl,
            String createdById,
            Boolean isPublic,
            List<String> memberIds,
            String privateKey,
            String lastMessageId,
            String lastMessageContent,
            Long lastMessageAt,
            Integer forumPostCount,
            Integer forumCommentCount,
            Long createdAt,
            Long updatedAt,
            Long deletedAt,
            Integer unreadCount,
            String lastReadMessageId,
            Long lastReadAt
    ) {
        public static RoomResponse from(ChatRoom room, ChatRoomMember member, int unreadCount) {
            return new RoomResponse(
                    room.getId(),
                    room.getName(),
                    room.getDescription(),
                    room.getType(),
                    room.getAvatarUrl(),
                    room.getCreatedById(),
                    room.getIsPublic(),
                    room.getMemberIds(),
                    room.getPrivateKey(),
                    room.getLastMessageId(),
                    room.getLastMessageContent(),
                    room.getLastMessageAt(),
                    room.getForumPostCount(),
                    room.getForumCommentCount(),
                    room.getCreatedAt(),
                    room.getUpdatedAt(),
                    room.getDeletedAt(),
                    unreadCount,
                    member == null ? null : member.getLastReadMessageId(),
                    member == null ? null : member.getLastReadAt()
            );
        }
    }
}
