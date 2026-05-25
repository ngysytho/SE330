package com.example.chatapp.service;

import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.model.Document;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageException;
import com.google.firebase.cloud.StorageClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
    private final FirebaseService firebase;
    private final StorageClient storageClient;

    @Value("${firebase.upload.max-size-bytes:26214400}")
    private long maxFileSizeBytes;

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.storage-bucket:}")
    private String storageBucket;

    public DocumentService(FirebaseService firebase, StorageClient storageClient) {
        this.firebase = firebase;
        this.storageClient = storageClient;
    }

    public Document upload(MultipartFile file, String note, String userId) {
        validateFile(file);
        try {
            long now = firebase.now();
            String id = firebase.newId(FirebaseService.DOCUMENTS);
            String safeName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename().replace("\\", "_").replace("/", "_");
            String path = "documents/" + userId + "/" + id + "-" + safeName;
            String contentType = file.getContentType() == null || file.getContentType().isBlank()
                    ? "application/octet-stream"
                    : file.getContentType();
            String downloadToken = UUID.randomUUID().toString();

            Bucket bucket = resolveBucket();
            Blob blob = bucket.create(path, file.getBytes(), contentType);
            blob = blob.toBuilder()
                    .setMetadata(Map.of("firebaseStorageDownloadTokens", downloadToken))
                    .setContentDisposition("inline; filename=\"" + safeName.replace("\"", "") + "\"")
                    .build()
                    .update();
            try {
                blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
            } catch (Exception ignored) {
            }
            String url = firebaseDownloadUrl(bucket.getName(), path, downloadToken);
            Document document = Document.builder()
                    .id(id)
                    .userId(userId)
                    .fileName(safeName)
                    .fileUrl(url)
                    .storagePath(path)
                    .documentType(contentType)
                    .size(file.getSize())
                    .note(note)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            firebase.save(FirebaseService.DOCUMENTS, id, document);
            return document;
        } catch (Exception e) {
            throw new IllegalStateException("Could not upload file", e);
        }
    }

    public List<Document> getMany(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new LinkedHashSet<>(ids).stream()
                .map(this::find)
                .filter(Objects::nonNull)
                .filter(document -> document.getDeletedAt() == null)
                .toList();
    }

    public List<Document> requireOwned(List<String> ids, String userId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new LinkedHashSet<>(ids).stream()
                .map(id -> {
                    Document document = firebase.get(FirebaseService.DOCUMENTS, id, Document.class);
                    if (document.getDeletedAt() != null || !userId.equals(document.getUserId())) {
                        throw new BadRequestException("Attachment is not available: " + id);
                    }
                    return document;
                })
                .toList();
    }

    public List<Document> my(String userId) {
        return firebase.run(firebase.collection(FirebaseService.DOCUMENTS).whereEqualTo("userId", userId).get())
                .stream()
                .map(snapshot -> snapshot.toObject(Document.class))
                .filter(document -> document.getDeletedAt() == null)
                .sorted(Comparator.comparing(Document::getCreatedAt).reversed())
                .toList();
    }

    public Document get(String id, String userId) {
        Document document = firebase.get(FirebaseService.DOCUMENTS, id, Document.class);
        if (!userId.equals(document.getUserId()) || document.getDeletedAt() != null) {
            throw new com.example.chatapp.exception.ResourceNotFoundException("Document not found");
        }
        return document;
    }

    public void delete(String id, String userId) {
        Document document = get(id, userId);
        try {
            Blob blob = storageClient.bucket().get(document.getStoragePath());
            if (blob != null) {
                blob.delete();
            }
        } catch (Exception ignored) {
        }
        firebase.update(FirebaseService.DOCUMENTS, id, Map.of("deletedAt", firebase.now(), "updatedAt", firebase.now()));
    }

    private Document find(String id) {
        try {
            return firebase.get(FirebaseService.DOCUMENTS, id, Document.class);
        } catch (com.example.chatapp.exception.ResourceNotFoundException ignored) {
            return null;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File is too large");
        }
    }

    private Bucket resolveBucket() {
        RuntimeException lastError = null;
        for (String bucketName : bucketCandidates()) {
            try {
                return storageClient.bucket(bucketName);
            } catch (IllegalArgumentException | StorageException e) {
                lastError = e;
            }
        }
        throw new IllegalStateException("Firebase Storage bucket is not available. Configure FIREBASE_STORAGE_BUCKET.", lastError);
    }

    private List<String> bucketCandidates() {
        return new LinkedHashSet<>(List.of(
                storageBucket == null ? "" : storageBucket,
                projectId + ".appspot.com",
                projectId + ".firebasestorage.app"
        )).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private String firebaseDownloadUrl(String bucketName, String path, String token) {
        return "https://firebasestorage.googleapis.com/v0/b/"
                + bucketName
                + "/o/"
                + encode(path)
                + "?alt=media&token="
                + encode(token);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
