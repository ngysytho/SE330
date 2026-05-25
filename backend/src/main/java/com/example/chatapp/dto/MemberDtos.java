package com.example.chatapp.dto;

import com.example.chatapp.enums.ChatRoomMemberRole;
import com.example.chatapp.enums.ChatRoomMemberStatus;
import com.example.chatapp.model.ChatRoomMember;
import com.example.chatapp.model.User;

public final class MemberDtos {
    private MemberDtos() {
    }

    public record AddMemberRequest(String userId) {
    }

    public record UpdateMemberRoleRequest(ChatRoomMemberRole role) {
    }

    public record UpdateMemberStatusRequest(ChatRoomMemberStatus status) {
    }

    public record MemberResponse(
            String id,
            String roomId,
            String userId,
            String userName,
            String userEmail,
            String userAvatar,
            Boolean userOnline,
            Long userLastSeenAt,
            ChatRoomMemberRole role,
            ChatRoomMemberStatus status,
            String lastReadMessageId,
            Long lastReadAt,
            Long joinedAt,
            Long leftAt,
            Long createdAt,
            Long updatedAt
    ) {
        public static MemberResponse from(ChatRoomMember member, User user) {
            return from(member, user, user == null ? null : user.getIsOnline());
        }

        public static MemberResponse from(ChatRoomMember member, User user, Boolean userOnline) {
            return new MemberResponse(
                    member.getId(),
                    member.getRoomId(),
                    member.getUserId(),
                    user == null ? null : user.getName(),
                    user == null ? null : user.getGmail(),
                    user == null ? null : user.getAvatarImage(),
                    userOnline,
                    user == null ? null : user.getLastSeenAt(),
                    member.getRole(),
                    member.getStatus(),
                    member.getLastReadMessageId(),
                    member.getLastReadAt(),
                    member.getJoinedAt(),
                    member.getLeftAt(),
                    member.getCreatedAt(),
                    member.getUpdatedAt()
            );
        }
    }
}
