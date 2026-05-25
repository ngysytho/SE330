package com.example.chatapp.model;

import com.example.chatapp.enums.MessageType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String id;
    private String roomId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private MessageType messageType;
    private List<String> attachmentIds;
    private List<Document> attachments;
    private Long editedAt;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
