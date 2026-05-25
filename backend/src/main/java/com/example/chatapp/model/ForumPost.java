package com.example.chatapp.model;

import com.example.chatapp.enums.ForumPostStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPost {
    private String id;
    private String roomId;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String content;
    private List<String> attachmentIds;
    private List<Document> attachments;
    private ForumPostStatus status;
    private Integer commentCount;
    private Long lastCommentAt;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
