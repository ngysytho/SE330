package com.example.chatapp.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumComment {
    private String id;
    private String postId;
    private String roomId;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private List<String> attachmentIds;
    private List<Document> attachments;
    private String parentCommentId;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
