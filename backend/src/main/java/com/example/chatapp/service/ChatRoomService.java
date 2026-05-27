package com.example.chatapp.service;

import com.example.chatapp.dto.RoomDtos.CreatePrivateRoomRequest;
import com.example.chatapp.dto.RoomDtos.CreateRoomRequest;
import com.example.chatapp.dto.RoomDtos.RoomResponse;
import com.example.chatapp.dto.RoomDtos.UpdateRoomRequest;
import com.example.chatapp.enums.ChatRoomMemberRole;
import com.example.chatapp.enums.ChatRoomType;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.ForbiddenException;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.ChatRoomMember;
import com.example.chatapp.model.Message;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ChatRoomService {
    private static final String DEFAULT_FORUM_NAME = "Server Forum";
    private static final String DEFAULT_FORUM_DESCRIPTION = "Moi nguoi trong server co the nhan chung tai day.";

    private final FirebaseService firebase;
    private final UserService userService;
    private final ChatRoomMemberService memberService;

    public ChatRoomService(FirebaseService firebase, UserService userService, ChatRoomMemberService memberService) {
        this.firebase = firebase;
        this.userService = userService;
        this.memberService = memberService;
    }

    public ChatRoom createPrivate(CreatePrivateRoomRequest request, String actorId) {
        if (request.otherUserId() == null || request.otherUserId().equals(actorId)) {
            throw new BadRequestException("Private chat requires another user");
        }
        userService.get(request.otherUserId());
        List<String> sorted = new ArrayList<>(List.of(actorId, request.otherUserId()));
        sorted.sort(String::compareTo);
        String privateKey = String.join("_", sorted);

        ChatRoom room = firebase.run(firebase.collection(FirebaseService.CHAT_ROOMS)
                        .whereEqualTo("privateKey", privateKey)
                        .limit(1)
                .get())
                .stream()
                .map(snapshot -> snapshot.toObject(ChatRoom.class))
                .filter(existingRoom -> existingRoom.getDeletedAt() == null)
                .findFirst()
                .orElseGet(() -> {
                    ChatRoom createdRoom = baseRoom(null, "Private chat", ChatRoomType.PRIVATE, actorId, false, sorted);
                    createdRoom.setPrivateKey(privateKey);
                    firebase.save(FirebaseService.CHAT_ROOMS, createdRoom.getId(), createdRoom);
                    memberService.createMember(createdRoom.getId(), actorId, ChatRoomMemberRole.MEMBER);
                    memberService.createMember(createdRoom.getId(), request.otherUserId(), ChatRoomMemberRole.MEMBER);
                    return createdRoom;
                });
        syncPrivateMembers(room, sorted);
        return room;
    }

    public ChatRoom createGroup(CreateRoomRequest request, String actorId) {
        return createRoom(request, actorId, ChatRoomType.GROUP);
    }

    public ChatRoom createForum(CreateRoomRequest request, String actorId) {
        return defaultForum(actorId);
    }

    public List<ChatRoom> myRooms(String userId) {
        defaultForum(userId);
        return firebase.all(FirebaseService.CHAT_ROOMS, ChatRoom.class).stream()
                .filter(room -> room.getDeletedAt() == null)
                .filter(room -> room.getType() != ChatRoomType.FORUM || DEFAULT_FORUM_NAME.equals(room.getName()))
                .filter(room -> Boolean.TRUE.equals(room.getIsPublic()) || room.getMemberIds() != null && room.getMemberIds().contains(userId))
                .sorted(Comparator.comparing((ChatRoom room) -> room.getLastMessageAt() == null ? room.getUpdatedAt() : room.getLastMessageAt(), Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
    }

    public List<RoomResponse> myRoomResponses(String userId) {
        return myRooms(userId).stream()
                .map(room -> toResponse(room, userId))
                .toList();
    }

    public List<ChatRoom> myRoomsByType(String userId, ChatRoomType type) {
        return myRooms(userId).stream().filter(room -> room.getType() == type).toList();
    }

    public List<RoomResponse> myRoomResponsesByType(String userId, ChatRoomType type) {
        return myRoomsByType(userId, type).stream()
                .map(room -> toResponse(room, userId))
                .toList();
    }

    public ChatRoom get(String roomId, String actorId) {
        ChatRoom room = firebase.get(FirebaseService.CHAT_ROOMS, roomId, ChatRoom.class);
        if (room.getDeletedAt() != null) {
            throw new com.example.chatapp.exception.ResourceNotFoundException("Room not found");
        }
        if (!Boolean.TRUE.equals(room.getIsPublic())) {
            memberService.requireActive(roomId, actorId);
        }
        return room;
    }

    public RoomResponse getResponse(String roomId, String actorId) {
        return toResponse(get(roomId, actorId), actorId);
    }

    public ChatRoom update(String roomId, UpdateRoomRequest request, String actorId) {
        ChatRoom room = firebase.get(FirebaseService.CHAT_ROOMS, roomId, ChatRoom.class);
        memberService.requireActive(roomId, actorId);
        Map<String, Object> updates = new HashMap<>();
        if (room.getType() != ChatRoomType.FORUM) {
            put(updates, "name", request.name());
            put(updates, "isPublic", request.isPublic());
        } else {
            updates.put("isPublic", true);
        }
        put(updates, "description", request.description());
        put(updates, "avatarUrl", request.avatarUrl());
        updates.put("updatedAt", firebase.now());
        firebase.update(FirebaseService.CHAT_ROOMS, roomId, updates);
        return get(roomId, actorId);
    }

    public void delete(String roomId, String actorId) {
        memberService.requireOwner(roomId, actorId);
        firebase.update(FirebaseService.CHAT_ROOMS, roomId, Map.of("deletedAt", firebase.now(), "updatedAt", firebase.now()));
    }

    public void touchLastMessage(String roomId, String messageId, String content, long createdAt) {
        firebase.update(FirebaseService.CHAT_ROOMS, roomId, Map.of(
                "lastMessageId", messageId,
                "lastMessageContent", content == null ? "" : content,
                "lastMessageAt", createdAt,
                "updatedAt", firebase.now()
        ));
    }

    public ChatRoom defaultForum(String actorId) {
        ChatRoom room = firebase.all(FirebaseService.CHAT_ROOMS, ChatRoom.class).stream()
                .filter(item -> item.getDeletedAt() == null)
                .filter(item -> item.getType() == ChatRoomType.FORUM)
                .filter(item -> DEFAULT_FORUM_NAME.equals(item.getName()))
                .findFirst()
                .orElseGet(() -> {
                    ChatRoom created = baseRoom(DEFAULT_FORUM_NAME, DEFAULT_FORUM_DESCRIPTION, ChatRoomType.FORUM, actorId, true, new ArrayList<>(List.of(actorId)));
                    firebase.save(FirebaseService.CHAT_ROOMS, created.getId(), created);
                    memberService.createMember(created.getId(), actorId, ChatRoomMemberRole.OWNER);
                    return created;
                });
        normalizeDefaultForum(room);
        ensureDefaultForumMembers(room, actorId);
        return room;
    }

    private ChatRoom createRoom(CreateRoomRequest request, String actorId, ChatRoomType type) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Room name is required");
        }
        Set<String> members = new LinkedHashSet<>();
        members.add(actorId);
        if (request.memberIds() != null) {
            request.memberIds().forEach(userId -> {
                userService.get(userId);
                members.add(userId);
            });
        }

        boolean isPublic = type == ChatRoomType.FORUM || Boolean.TRUE.equals(request.isPublic());
        ChatRoom room = baseRoom(request.name(), request.description(), type, actorId, isPublic, new ArrayList<>(members));
        room.setAvatarUrl(request.avatarUrl());
        firebase.save(FirebaseService.CHAT_ROOMS, room.getId(), room);
        memberService.createMember(room.getId(), actorId, ChatRoomMemberRole.OWNER);
        members.stream()
                .filter(userId -> !userId.equals(actorId))
                .forEach(userId -> memberService.createMember(room.getId(), userId, ChatRoomMemberRole.MEMBER));
        return room;
    }

    private ChatRoom baseRoom(String name, String description, ChatRoomType type, String createdById, boolean isPublic, List<String> members) {
        long now = firebase.now();
        String id = firebase.newId(FirebaseService.CHAT_ROOMS);
        return ChatRoom.builder()
                .id(id)
                .name(name)
                .description(description)
                .type(type)
                .createdById(createdById)
                .isPublic(isPublic)
                .memberIds(members)
                .forumPostCount(0)
                .forumCommentCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void put(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void normalizeDefaultForum(ChatRoom room) {
        Map<String, Object> updates = new HashMap<>();
        if (!DEFAULT_FORUM_NAME.equals(room.getName())) {
            updates.put("name", DEFAULT_FORUM_NAME);
            room.setName(DEFAULT_FORUM_NAME);
        }
        if (room.getDescription() == null) {
            updates.put("description", DEFAULT_FORUM_DESCRIPTION);
            room.setDescription(DEFAULT_FORUM_DESCRIPTION);
        }
        if (!Boolean.TRUE.equals(room.getIsPublic())) {
            updates.put("isPublic", true);
            room.setIsPublic(true);
        }
        if (!updates.isEmpty()) {
            updates.put("updatedAt", firebase.now());
            firebase.update(FirebaseService.CHAT_ROOMS, room.getId(), updates);
        }
    }

    private void ensureDefaultForumMembers(ChatRoom room, String actorId) {
        Set<String> ids = new LinkedHashSet<>(room.getMemberIds() == null ? List.of() : room.getMemberIds());
        ids.add(actorId);
        firebase.all(FirebaseService.USERS, com.example.chatapp.model.User.class).stream()
                .filter(user -> user.getDeletedAt() == null)
                .map(com.example.chatapp.model.User::getId)
                .forEach(ids::add);

        ids.forEach(userId -> memberService.ensureMember(room.getId(), userId, ChatRoomMemberRole.MEMBER));

        List<String> nextIds = new ArrayList<>(ids);
        if (!nextIds.equals(room.getMemberIds())) {
            firebase.update(FirebaseService.CHAT_ROOMS, room.getId(), Map.of("memberIds", nextIds, "updatedAt", firebase.now()));
            room.setMemberIds(nextIds);
        }
    }

    private void syncPrivateMembers(ChatRoom room, List<String> userIds) {
        userIds.forEach(userId -> memberService.ensureMemberRole(room.getId(), userId, ChatRoomMemberRole.MEMBER));
        if (!userIds.equals(room.getMemberIds())) {
            firebase.update(FirebaseService.CHAT_ROOMS, room.getId(), Map.of("memberIds", userIds, "updatedAt", firebase.now()));
            room.setMemberIds(userIds);
        }
    }

    public RoomResponse toResponse(ChatRoom room, String userId) {
        ChatRoomMember member = memberService.findMember(room.getId(), userId).orElse(null);
        return RoomResponse.from(room, member, unreadCount(room, member, userId));
    }

    private int unreadCount(ChatRoom room, ChatRoomMember member, String userId) {
        if (room.getLastMessageAt() == null || member == null) {
            return 0;
        }
        Long baseline = member.getLastReadAt() != null ? member.getLastReadAt() : member.getJoinedAt();
        if (baseline != null && room.getLastMessageAt() <= baseline) {
            return 0;
        }
        return (int) firebase.run(firebase.collection(FirebaseService.MESSAGES)
                        .whereEqualTo("roomId", room.getId())
                        .get())
                .stream()
                .map(snapshot -> snapshot.toObject(Message.class))
                .filter(message -> message.getDeletedAt() == null)
                .filter(message -> !userId.equals(message.getSenderId()))
                .filter(message -> {
                    Long createdAt = message.getCreatedAt();
                    return createdAt != null && (baseline == null || createdAt > baseline);
                })
                .count();
    }
}
