package com.example.chatapp.service;

import com.example.chatapp.dto.MemberDtos.MemberResponse;
import com.example.chatapp.enums.ChatRoomMemberRole;
import com.example.chatapp.enums.ChatRoomMemberStatus;
import com.example.chatapp.exception.ForbiddenException;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.ChatRoomMember;
import com.example.chatapp.model.User;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatRoomMemberService {
    private final FirebaseService firebase;
    private final UserService userService;
    private final PresenceService presenceService;

    public ChatRoomMemberService(FirebaseService firebase, UserService userService, PresenceService presenceService) {
        this.firebase = firebase;
        this.userService = userService;
        this.presenceService = presenceService;
    }

    public ChatRoomMember addMember(String roomId, String userId, String actorId) {
        requireOwnerOrAdmin(roomId, actorId);
        ChatRoom room = firebase.get(FirebaseService.CHAT_ROOMS, roomId, ChatRoom.class);
        ChatRoomMember member = findMember(roomId, userId).map(existing -> {
            Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("status", ChatRoomMemberStatus.ACTIVE);
            updates.put("leftAt", null);
            updates.put("deletedAt", null);
            updates.put("updatedAt", firebase.now());
            firebase.update(FirebaseService.CHAT_ROOM_MEMBERS, existing.getId(), updates);
            existing.setStatus(ChatRoomMemberStatus.ACTIVE);
            existing.setLeftAt(null);
            existing.setDeletedAt(null);
            return existing;
        }).orElseGet(() -> createMember(roomId, userId, ChatRoomMemberRole.MEMBER));
        List<String> ids = new ArrayList<>(room.getMemberIds() == null ? List.of() : room.getMemberIds());
        if (!ids.contains(userId)) {
            ids.add(userId);
        }
        firebase.update(FirebaseService.CHAT_ROOMS, roomId, Map.of("memberIds", ids, "updatedAt", firebase.now()));
        return member;
    }

    public List<ChatRoomMember> list(String roomId, String actorId) {
        ChatRoom room = firebase.get(FirebaseService.CHAT_ROOMS, roomId, ChatRoom.class);
        if (!Boolean.TRUE.equals(room.getIsPublic())) {
            requireActive(roomId, actorId);
        }
        return firebase.run(firebase.collection(FirebaseService.CHAT_ROOM_MEMBERS).whereEqualTo("roomId", roomId).get())
                .stream()
                .map(snapshot -> snapshot.toObject(ChatRoomMember.class))
                .filter(member -> member.getDeletedAt() == null)
                .toList();
    }

    public List<MemberResponse> listWithUsers(String roomId, String actorId) {
        return uniqueMembers(list(roomId, actorId)).stream()
                .map(member -> {
                    User user = findUser(member.getUserId());
                    return MemberResponse.from(member, user, presenceService.isOnline(member.getUserId()));
                })
                .toList();
    }

    public ChatRoomMember updateRole(String roomId, String userId, ChatRoomMemberRole role, String actorId) {
        requireOwner(roomId, actorId);
        ChatRoomMember member = findMember(roomId, userId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        firebase.update(FirebaseService.CHAT_ROOM_MEMBERS, member.getId(), Map.of("role", role, "updatedAt", firebase.now()));
        member.setRole(role);
        return member;
    }

    public ChatRoomMember updateStatus(String roomId, String userId, ChatRoomMemberStatus status, String actorId) {
        requireOwnerOrAdmin(roomId, actorId);
        ChatRoomMember member = findMember(roomId, userId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        firebase.update(FirebaseService.CHAT_ROOM_MEMBERS, member.getId(), Map.of("status", status, "updatedAt", firebase.now()));
        member.setStatus(status);
        return member;
    }

    public void removeMember(String roomId, String userId, String actorId) {
        requireOwner(roomId, actorId);
        ChatRoomMember member = findMember(roomId, userId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        long now = firebase.now();
        firebase.update(FirebaseService.CHAT_ROOM_MEMBERS, member.getId(), Map.of(
                "status", ChatRoomMemberStatus.LEFT,
                "leftAt", now,
                "deletedAt", now,
                "updatedAt", now
        ));
        ChatRoom room = firebase.get(FirebaseService.CHAT_ROOMS, roomId, ChatRoom.class);
        List<String> ids = new ArrayList<>(room.getMemberIds() == null ? List.of() : room.getMemberIds());
        ids.remove(userId);
        firebase.update(FirebaseService.CHAT_ROOMS, roomId, Map.of("memberIds", ids, "updatedAt", now));
    }

    public void markRead(String roomId, String messageId, String userId) {
        ChatRoomMember member = requireActive(roomId, userId);
        firebase.update(FirebaseService.CHAT_ROOM_MEMBERS, member.getId(), Map.of(
                "lastReadMessageId", messageId,
                "lastReadAt", firebase.now(),
                "updatedAt", firebase.now()
        ));
    }

    public ChatRoomMember requireActive(String roomId, String userId) {
        ChatRoomMember member = findMember(roomId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));
        if (member.getDeletedAt() != null || member.getStatus() != ChatRoomMemberStatus.ACTIVE) {
            throw new ForbiddenException("You cannot access this room");
        }
        return member;
    }

    public void requireOwner(String roomId, String userId) {
        ChatRoomMember member = requireActive(roomId, userId);
        if (member.getRole() != ChatRoomMemberRole.OWNER) {
            throw new ForbiddenException("Owner role required");
        }
    }

    public void requireOwnerOrAdmin(String roomId, String userId) {
        ChatRoomMember member = requireActive(roomId, userId);
        if (member.getRole() != ChatRoomMemberRole.OWNER && member.getRole() != ChatRoomMemberRole.ADMIN) {
            throw new ForbiddenException("Owner or admin role required");
        }
    }

    public java.util.Optional<ChatRoomMember> findMember(String roomId, String userId) {
        return firebase.run(firebase.collection(FirebaseService.CHAT_ROOM_MEMBERS)
                        .whereEqualTo("roomId", roomId)
                        .whereEqualTo("userId", userId)
                        .get())
                .stream()
                .map(snapshot -> snapshot.toObject(ChatRoomMember.class))
                .sorted(Comparator
                        .comparing((ChatRoomMember member) -> member.getDeletedAt() == null ? 0 : 1)
                        .thenComparing(member -> member.getStatus() == ChatRoomMemberStatus.ACTIVE ? 0 : 1)
                        .thenComparing((ChatRoomMember member) -> member.getUpdatedAt() == null ? 0L : member.getUpdatedAt(), Comparator.reverseOrder()))
                .findFirst();
    }

    public ChatRoomMember createMember(String roomId, String userId, ChatRoomMemberRole role) {
        long now = firebase.now();
        String id = firebase.newId(FirebaseService.CHAT_ROOM_MEMBERS);
        ChatRoomMember member = ChatRoomMember.builder()
                .id(id)
                .roomId(roomId)
                .userId(userId)
                .role(role)
                .status(ChatRoomMemberStatus.ACTIVE)
                .joinedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        firebase.save(FirebaseService.CHAT_ROOM_MEMBERS, id, member);
        return member;
    }

    public ChatRoomMember ensureMember(String roomId, String userId, ChatRoomMemberRole role) {
        return findMember(roomId, userId).map(existing -> {
            Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("status", ChatRoomMemberStatus.ACTIVE);
            updates.put("leftAt", null);
            updates.put("deletedAt", null);
            updates.put("updatedAt", firebase.now());
            firebase.update(FirebaseService.CHAT_ROOM_MEMBERS, existing.getId(), updates);
            existing.setStatus(ChatRoomMemberStatus.ACTIVE);
            existing.setLeftAt(null);
            existing.setDeletedAt(null);
            return existing;
        }).orElseGet(() -> createMember(roomId, userId, role));
    }

    public ChatRoomMember ensureMemberRole(String roomId, String userId, ChatRoomMemberRole role) {
        ChatRoomMember member = ensureMember(roomId, userId, role);
        if (member.getRole() != role) {
            firebase.update(FirebaseService.CHAT_ROOM_MEMBERS, member.getId(), Map.of("role", role, "updatedAt", firebase.now()));
            member.setRole(role);
        }
        return member;
    }

    private User findUser(String userId) {
        try {
            return userService.get(userId);
        } catch (ResourceNotFoundException ignored) {
            return null;
        }
    }

    private List<ChatRoomMember> uniqueMembers(List<ChatRoomMember> members) {
        Map<String, ChatRoomMember> byUserId = new LinkedHashMap<>();
        members.forEach(member -> byUserId.merge(member.getUserId(), member, this::preferredMember));
        return byUserId.values().stream()
                .sorted(Comparator
                        .comparing((ChatRoomMember member) -> member.getRole() == ChatRoomMemberRole.OWNER ? 0 : member.getRole() == ChatRoomMemberRole.ADMIN ? 1 : 2)
                        .thenComparing(member -> member.getJoinedAt() == null ? Long.MAX_VALUE : member.getJoinedAt()))
                .toList();
    }

    private ChatRoomMember preferredMember(ChatRoomMember current, ChatRoomMember candidate) {
        int currentRank = roleRank(current.getRole());
        int candidateRank = roleRank(candidate.getRole());
        if (candidateRank < currentRank) {
            return candidate;
        }
        if (candidateRank > currentRank) {
            return current;
        }
        long currentUpdatedAt = current.getUpdatedAt() == null ? 0 : current.getUpdatedAt();
        long candidateUpdatedAt = candidate.getUpdatedAt() == null ? 0 : candidate.getUpdatedAt();
        return candidateUpdatedAt > currentUpdatedAt ? candidate : current;
    }

    private int roleRank(ChatRoomMemberRole role) {
        if (role == ChatRoomMemberRole.OWNER) {
            return 0;
        }
        if (role == ChatRoomMemberRole.ADMIN) {
            return 1;
        }
        return 2;
    }
}
