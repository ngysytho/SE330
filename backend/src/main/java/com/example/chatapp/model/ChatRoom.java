package com.example.chatapp.model;

import com.example.chatapp.enums.ChatRoomType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    private String id;
    private String name;
    private String description;
    private ChatRoomType type;
    private String avatarUrl;
    private String createdById;
    private Boolean isPublic;
    private List<String> memberIds;
    private String privateKey;
    private String lastMessageId;
    private String lastMessageContent;
    private Long lastMessageAt;
    private Integer forumPostCount;
    private Integer forumCommentCount;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
