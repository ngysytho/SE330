package com.example.chatapp.service;

import com.example.chatapp.dto.MessageDtos.CreateMessageRequest;
import com.example.chatapp.dto.MessageDtos.UpdateMessageRequest;
import com.example.chatapp.enums.ChatRoomMemberRole;
import com.example.chatapp.enums.MessageType;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.ForbiddenException;
import com.example.chatapp.model.ChatRoomMember;
import com.example.chatapp.model.Document;
import com.example.chatapp.model.Message;
import com.example.chatapp.model.User;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final FirebaseService firebase;
    private final ChatRoomMemberService memberService;
    private final ChatRoomService roomService;
    private final UserService userService;
    private final DocumentService documentService;

    public MessageService(FirebaseService firebase, ChatRoomMemberService memberService, ChatRoomService roomService, UserService userService, DocumentService documentService) {
        this.firebase = firebase;
        this.memberService = memberService;
        this.roomService = roomService;
        this.userService = userService;
        this.documentService = documentService;
    }

    public List<Message> list(String roomId, int pageSize, String lastMessageId, String actorId) {
        memberService.requireActive(roomId, actorId);
        Long cursor = null;
        if (lastMessageId != null && !lastMessageId.isBlank()) {
            cursor = firebase.get(FirebaseService.MESSAGES, lastMessageId, Message.class).getCreatedAt();
        }
        Long finalCursor = cursor;
        List<Message> messages = firebase.run(firebase.collection(FirebaseService.MESSAGES).whereEqualTo("roomId", roomId).get())
                .stream()
                .map(snapshot -> snapshot.toObject(Message.class))
                .filter(message -> message.getDeletedAt() == null)
                .filter(message -> finalCursor == null || message.getCreatedAt() < finalCursor)
                .sorted(Comparator.comparing(Message::getCreatedAt).reversed())
                .limit(Math.max(1, Math.min(pageSize, 100)))
                .toList();
        messages.forEach(this::hydrateAttachments);
        return messages;
    }

    public Message send(String roomId, CreateMessageRequest request, String actorId) {
        memberService.requireActive(roomId, actorId);
        List<Document> attachments = documentService.requireOwned(request.attachmentIds(), actorId);
        String content = request.content() == null ? "" : request.content();
        if (content.isBlank() && attachments.isEmpty()) {
            throw new BadRequestException("Message content or attachment is required");
        }
        User sender = userService.get(actorId);
        long now = firebase.now();
        String id = firebase.newId(FirebaseService.MESSAGES);
        Message message = Message.builder()
                .id(id)
                .roomId(roomId)
                .senderId(actorId)
                .senderName(sender.getName())
                .senderAvatar(sender.getAvatarImage())
                .content(content)
                .messageType(request.messageType() == null ? inferType(content, attachments) : request.messageType())
                .attachmentIds(attachments.stream().map(Document::getId).toList())
                .attachments(attachments)
                .createdAt(now)
                .updatedAt(now)
                .build();
        firebase.save(FirebaseService.MESSAGES, id, message);
        roomService.touchLastMessage(roomId, id, previewContent(content, attachments), now);
        return message;
    }

    public Message update(String messageId, UpdateMessageRequest request, String actorId) {
        Message message = firebase.get(FirebaseService.MESSAGES, messageId, Message.class);
        if (!actorId.equals(message.getSenderId())) {
            throw new ForbiddenException("Only the sender can edit this message");
        }
        long now = firebase.now();
        firebase.update(FirebaseService.MESSAGES, messageId, Map.of("content", request.content(), "editedAt", now, "updatedAt", now));
        message.setContent(request.content());
        message.setEditedAt(now);
        message.setUpdatedAt(now);
        return message;
    }

    public Message delete(String messageId, String actorId) {
        Message message = firebase.get(FirebaseService.MESSAGES, messageId, Message.class);
        ChatRoomMember member = memberService.requireActive(message.getRoomId(), actorId);
        boolean canDelete = actorId.equals(message.getSenderId())
                || member.getRole() == ChatRoomMemberRole.OWNER
                || member.getRole() == ChatRoomMemberRole.ADMIN;
        if (!canDelete) {
            throw new ForbiddenException("You cannot delete this message");
        }
        long now = firebase.now();
        firebase.update(FirebaseService.MESSAGES, messageId, Map.of("deletedAt", now, "updatedAt", now));
        message.setDeletedAt(now);
        message.setUpdatedAt(now);
        return message;
    }

    private void hydrateAttachments(Message message) {
        if (message.getAttachmentIds() == null || message.getAttachmentIds().isEmpty()) {
            return;
        }
        message.setAttachments(documentService.getMany(message.getAttachmentIds()));
    }

    private MessageType inferType(String content, List<Document> attachments) {
        if (attachments.isEmpty()) {
            return MessageType.TEXT;
        }
        if (attachments.stream().allMatch(document -> document.getDocumentType() != null && document.getDocumentType().startsWith("image/"))) {
            return MessageType.IMAGE;
        }
        return MessageType.FILE;
    }

    private String previewContent(String content, List<Document> attachments) {
        if (content != null && !content.isBlank()) {
            return content;
        }
        if (attachments.size() == 1) {
            return attachments.get(0).getFileName();
        }
        return attachments.size() + " attachments";
    }
}
