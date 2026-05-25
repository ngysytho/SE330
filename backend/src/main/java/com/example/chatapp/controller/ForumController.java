package com.example.chatapp.controller;

import com.example.chatapp.dto.ForumDtos.CreateForumCommentRequest;
import com.example.chatapp.dto.ForumDtos.CreateForumPostRequest;
import com.example.chatapp.dto.ForumDtos.UpdateForumCommentRequest;
import com.example.chatapp.dto.ForumDtos.UpdateForumPostRequest;
import com.example.chatapp.model.ForumComment;
import com.example.chatapp.model.ForumPost;
import com.example.chatapp.security.SecurityUtils;
import com.example.chatapp.service.ForumService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@RestController
public class ForumController {
    private final ForumService forumService;
    private final SimpMessagingTemplate messagingTemplate;

    public ForumController(ForumService forumService, SimpMessagingTemplate messagingTemplate) {
        this.forumService = forumService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/forums/{roomId}/posts")
    public ForumPost createPost(@PathVariable String roomId, @RequestBody CreateForumPostRequest request) {
        ForumPost post = forumService.createPost(roomId, request, SecurityUtils.currentUserId());
        messagingTemplate.convertAndSend("/topic/forums/" + roomId + "/posts", Map.of("type", "FORUM_POST_CREATED", "post", post));
        return post;
    }

    @GetMapping("/api/forums/{roomId}/posts")
    public List<ForumPost> listPosts(@PathVariable String roomId, @RequestParam(defaultValue = "30") int pageSize, @RequestParam(required = false) String lastPostId) {
        return forumService.listPosts(roomId, pageSize, lastPostId, SecurityUtils.currentUserId());
    }

    @GetMapping("/api/forums/posts/{postId}")
    public ForumPost getPost(@PathVariable String postId) {
        return forumService.getPost(postId, SecurityUtils.currentUserId());
    }

    @PatchMapping("/api/forums/posts/{postId}")
    public ForumPost updatePost(@PathVariable String postId, @RequestBody UpdateForumPostRequest request) {
        ForumPost post = forumService.updatePost(postId, request, SecurityUtils.currentUserId());
        messagingTemplate.convertAndSend("/topic/forums/" + post.getRoomId() + "/posts", Map.of("type", "FORUM_POST_UPDATED", "post", post));
        return post;
    }

    @DeleteMapping("/api/forums/posts/{postId}")
    public void deletePost(@PathVariable String postId) {
        ForumPost post = forumService.getPost(postId, SecurityUtils.currentUserId());
        forumService.deletePost(postId, SecurityUtils.currentUserId());
        messagingTemplate.convertAndSend("/topic/forums/" + post.getRoomId() + "/posts", Map.of("type", "FORUM_POST_DELETED", "postId", postId));
    }

    @PostMapping("/api/forums/posts/{postId}/comments")
    public ForumComment createComment(@PathVariable String postId, @RequestBody CreateForumCommentRequest request) {
        ForumComment comment = forumService.createComment(postId, request, SecurityUtils.currentUserId());
        messagingTemplate.convertAndSend("/topic/forums/posts/" + postId + "/comments", Map.of("type", "FORUM_COMMENT_CREATED", "comment", comment));
        return comment;
    }

    @GetMapping("/api/forums/posts/{postId}/comments")
    public List<ForumComment> listComments(@PathVariable String postId, @RequestParam(defaultValue = "50") int pageSize, @RequestParam(required = false) String lastCommentId) {
        return forumService.listComments(postId, pageSize, lastCommentId, SecurityUtils.currentUserId());
    }

    @PatchMapping("/api/forums/comments/{commentId}")
    public ForumComment updateComment(@PathVariable String commentId, @RequestBody UpdateForumCommentRequest request) {
        ForumComment comment = forumService.updateComment(commentId, request, SecurityUtils.currentUserId());
        messagingTemplate.convertAndSend("/topic/forums/posts/" + comment.getPostId() + "/comments", Map.of("type", "FORUM_COMMENT_UPDATED", "comment", comment));
        return comment;
    }

    @DeleteMapping("/api/forums/comments/{commentId}")
    public void deleteComment(@PathVariable String commentId) {
        forumService.deleteComment(commentId, SecurityUtils.currentUserId());
    }
}
