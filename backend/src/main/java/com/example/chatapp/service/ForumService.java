package com.example.chatapp.service;

import com.example.chatapp.dto.ForumDtos.CreateForumCommentRequest;
import com.example.chatapp.dto.ForumDtos.CreateForumPostRequest;
import com.example.chatapp.dto.ForumDtos.UpdateForumCommentRequest;
import com.example.chatapp.dto.ForumDtos.UpdateForumPostRequest;
import com.example.chatapp.enums.ChatRoomMemberRole;
import com.example.chatapp.enums.ChatRoomType;
import com.example.chatapp.enums.ForumPostStatus;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.ForbiddenException;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.ChatRoomMember;
import com.example.chatapp.model.Document;
import com.example.chatapp.model.ForumComment;
import com.example.chatapp.model.ForumPost;
import com.example.chatapp.model.User;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ForumService {
    private final FirebaseService firebase;
    private final ChatRoomMemberService memberService;
    private final UserService userService;
    private final DocumentService documentService;

    public ForumService(FirebaseService firebase, ChatRoomMemberService memberService, UserService userService, DocumentService documentService) {
        this.firebase = firebase;
        this.memberService = memberService;
        this.userService = userService;
        this.documentService = documentService;
    }

    public ForumPost createPost(String roomId, CreateForumPostRequest request, String actorId) {
        ChatRoom room = requireForum(roomId);
        requireForumAccess(room, actorId);
        List<Document> attachments = documentService.requireOwned(request.attachmentIds(), actorId);
        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("Post title is required");
        }
        if ((request.content() == null || request.content().isBlank()) && attachments.isEmpty()) {
            throw new BadRequestException("Post content or attachment is required");
        }
        User author = userService.get(actorId);
        long now = firebase.now();
        String id = firebase.newId(FirebaseService.FORUM_POSTS);
        ForumPost post = ForumPost.builder()
                .id(id)
                .roomId(roomId)
                .authorId(actorId)
                .authorName(author.getName())
                .authorAvatar(author.getAvatarImage())
                .title(request.title())
                .content(request.content())
                .attachmentIds(attachments.stream().map(Document::getId).toList())
                .attachments(attachments)
                .status(ForumPostStatus.OPEN)
                .commentCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        firebase.save(FirebaseService.FORUM_POSTS, id, post);
        firebase.update(FirebaseService.CHAT_ROOMS, roomId, Map.of(
                "forumPostCount", nullSafe(room.getForumPostCount()) + 1,
                "updatedAt", now
        ));
        return post;
    }

    public List<ForumPost> listPosts(String roomId, int pageSize, String lastPostId, String actorId) {
        ChatRoom room = requireForum(roomId);
        requireForumAccess(room, actorId);
        Long cursor = lastPostId == null || lastPostId.isBlank()
                ? null
                : firebase.get(FirebaseService.FORUM_POSTS, lastPostId, ForumPost.class).getCreatedAt();
        List<ForumPost> posts = firebase.run(firebase.collection(FirebaseService.FORUM_POSTS).whereEqualTo("roomId", roomId).get())
                .stream()
                .map(snapshot -> snapshot.toObject(ForumPost.class))
                .filter(post -> post.getDeletedAt() == null)
                .filter(post -> cursor == null || post.getCreatedAt() < cursor)
                .sorted(Comparator.comparing(ForumPost::getCreatedAt).reversed())
                .limit(Math.max(1, Math.min(pageSize, 100)))
                .toList();
        posts.forEach(this::hydratePostAttachments);
        return posts;
    }

    public ForumPost getPost(String postId, String actorId) {
        ForumPost post = firebase.get(FirebaseService.FORUM_POSTS, postId, ForumPost.class);
        if (post.getDeletedAt() != null) {
            throw new com.example.chatapp.exception.ResourceNotFoundException("Forum post not found");
        }
        ChatRoom room = requireForum(post.getRoomId());
        requireForumAccess(room, actorId);
        hydratePostAttachments(post);
        return post;
    }

    public ForumPost updatePost(String postId, UpdateForumPostRequest request, String actorId) {
        ForumPost post = getPost(postId, actorId);
        ensureAuthorOrAdmin(post.getRoomId(), post.getAuthorId(), actorId);
        Map<String, Object> updates = new HashMap<>();
        if (request.title() != null) {
            updates.put("title", request.title());
            post.setTitle(request.title());
        }
        if (request.content() != null) {
            updates.put("content", request.content());
            post.setContent(request.content());
        }
        if (request.status() != null) {
            updates.put("status", request.status());
            post.setStatus(request.status());
        }
        updates.put("updatedAt", firebase.now());
        firebase.update(FirebaseService.FORUM_POSTS, postId, updates);
        hydratePostAttachments(post);
        return post;
    }

    public void deletePost(String postId, String actorId) {
        ForumPost post = getPost(postId, actorId);
        ensureAuthorOrAdmin(post.getRoomId(), post.getAuthorId(), actorId);
        long now = firebase.now();
        firebase.update(FirebaseService.FORUM_POSTS, postId, Map.of("deletedAt", now, "updatedAt", now));
    }

    public ForumComment createComment(String postId, CreateForumCommentRequest request, String actorId) {
        ForumPost post = getPost(postId, actorId);
        if (post.getStatus() == ForumPostStatus.CLOSED || post.getStatus() == ForumPostStatus.ARCHIVED) {
            throw new BadRequestException("This forum post is not open for comments");
        }
        List<Document> attachments = documentService.requireOwned(request.attachmentIds(), actorId);
        if ((request.content() == null || request.content().isBlank()) && attachments.isEmpty()) {
            throw new BadRequestException("Comment content or attachment is required");
        }
        User author = userService.get(actorId);
        long now = firebase.now();
        String id = firebase.newId(FirebaseService.FORUM_COMMENTS);
        ForumComment comment = ForumComment.builder()
                .id(id)
                .postId(postId)
                .roomId(post.getRoomId())
                .authorId(actorId)
                .authorName(author.getName())
                .authorAvatar(author.getAvatarImage())
                .content(request.content())
                .attachmentIds(attachments.stream().map(Document::getId).toList())
                .attachments(attachments)
                .parentCommentId(request.parentCommentId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        firebase.save(FirebaseService.FORUM_COMMENTS, id, comment);
        firebase.update(FirebaseService.FORUM_POSTS, postId, Map.of(
                "commentCount", nullSafe(post.getCommentCount()) + 1,
                "lastCommentAt", now,
                "updatedAt", now
        ));
        ChatRoom room = firebase.get(FirebaseService.CHAT_ROOMS, post.getRoomId(), ChatRoom.class);
        firebase.update(FirebaseService.CHAT_ROOMS, post.getRoomId(), Map.of(
                "forumCommentCount", nullSafe(room.getForumCommentCount()) + 1,
                "updatedAt", now
        ));
        return comment;
    }

    public List<ForumComment> listComments(String postId, int pageSize, String lastCommentId, String actorId) {
        ForumPost post = getPost(postId, actorId);
        Long cursor = lastCommentId == null || lastCommentId.isBlank()
                ? null
                : firebase.get(FirebaseService.FORUM_COMMENTS, lastCommentId, ForumComment.class).getCreatedAt();
        List<ForumComment> comments = firebase.run(firebase.collection(FirebaseService.FORUM_COMMENTS).whereEqualTo("postId", post.getId()).get())
                .stream()
                .map(snapshot -> snapshot.toObject(ForumComment.class))
                .filter(comment -> comment.getDeletedAt() == null)
                .filter(comment -> cursor == null || comment.getCreatedAt() < cursor)
                .sorted(Comparator.comparing(ForumComment::getCreatedAt))
                .limit(Math.max(1, Math.min(pageSize, 100)))
                .toList();
        comments.forEach(this::hydrateCommentAttachments);
        return comments;
    }

    public ForumComment updateComment(String commentId, UpdateForumCommentRequest request, String actorId) {
        ForumComment comment = firebase.get(FirebaseService.FORUM_COMMENTS, commentId, ForumComment.class);
        ensureAuthorOrAdmin(comment.getRoomId(), comment.getAuthorId(), actorId);
        long now = firebase.now();
        firebase.update(FirebaseService.FORUM_COMMENTS, commentId, Map.of("content", request.content(), "updatedAt", now));
        comment.setContent(request.content());
        comment.setUpdatedAt(now);
        hydrateCommentAttachments(comment);
        return comment;
    }

    private void hydratePostAttachments(ForumPost post) {
        if (post.getAttachmentIds() != null && !post.getAttachmentIds().isEmpty()) {
            post.setAttachments(documentService.getMany(post.getAttachmentIds()));
        }
    }

    private void hydrateCommentAttachments(ForumComment comment) {
        if (comment.getAttachmentIds() != null && !comment.getAttachmentIds().isEmpty()) {
            comment.setAttachments(documentService.getMany(comment.getAttachmentIds()));
        }
    }

    public void deleteComment(String commentId, String actorId) {
        ForumComment comment = firebase.get(FirebaseService.FORUM_COMMENTS, commentId, ForumComment.class);
        ensureAuthorOrAdmin(comment.getRoomId(), comment.getAuthorId(), actorId);
        long now = firebase.now();
        firebase.update(FirebaseService.FORUM_COMMENTS, commentId, Map.of("deletedAt", now, "updatedAt", now));
    }

    private ChatRoom requireForum(String roomId) {
        ChatRoom room = firebase.get(FirebaseService.CHAT_ROOMS, roomId, ChatRoom.class);
        if (room.getType() != ChatRoomType.FORUM || room.getDeletedAt() != null) {
            throw new BadRequestException("Room is not a forum");
        }
        return room;
    }

    private void requireForumAccess(ChatRoom room, String actorId) {
        if (!Boolean.TRUE.equals(room.getIsPublic())) {
            memberService.requireActive(room.getId(), actorId);
        }
    }

    private void ensureAuthorOrAdmin(String roomId, String authorId, String actorId) {
        if (actorId.equals(authorId)) {
            return;
        }
        ChatRoomMember member = memberService.requireActive(roomId, actorId);
        boolean allowed = member.getRole() == ChatRoomMemberRole.OWNER
                || member.getRole() == ChatRoomMemberRole.ADMIN;
        if (!allowed) {
            throw new ForbiddenException("You cannot modify this forum item");
        }
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
