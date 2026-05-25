package com.example.chatapp.dto;

import com.example.chatapp.enums.ForumPostStatus;
import java.util.List;

public final class ForumDtos {
    private ForumDtos() {
    }

    public record CreateForumPostRequest(
            String title,
            String content,
            List<String> attachmentIds
    ) {
    }

    public record UpdateForumPostRequest(
            String title,
            String content,
            ForumPostStatus status
    ) {
    }

    public record CreateForumCommentRequest(
            String content,
            List<String> attachmentIds,
            String parentCommentId
    ) {
    }

    public record UpdateForumCommentRequest(String content) {
    }
}
