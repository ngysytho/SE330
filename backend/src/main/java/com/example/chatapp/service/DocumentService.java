package com.example.chatapp.service;

import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.model.Document;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageException;
import com.google.firebase.cloud.StorageClient;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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

    @Value("${backend.url:http://localhost:8080}")
    private String backendUrl;

    @Value("${local.upload-dir:uploads}")
    private String localUploadDir;

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
            String contentType = resolveContentType(file, safeName);
            StorageResult storage = storeFile(file, id, userId, safeName, path, contentType);
            Document document = Document.builder()
                    .id(id)
                    .userId(userId)
                    .fileName(safeName)
                    .fileUrl(storage.url())
                    .storagePath(storage.path())
                    .documentType(contentType)
                    .size(file.getSize())
                    .note(note)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            firebase.save(FirebaseService.DOCUMENTS, id, document);
            return document;
        } catch (Exception e) {
            throw new IllegalStateException("Could not upload file: " + rootCauseMessage(e), e);
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

    public StoredFile downloadFile(String id) {
        Document document = firebase.get(FirebaseService.DOCUMENTS, id, Document.class);
        if (document.getDeletedAt() != null || document.getStoragePath() == null || !document.getStoragePath().startsWith("local:")) {
            throw new com.example.chatapp.exception.ResourceNotFoundException("Document not found");
        }
        try {
            Path file = localStorageRoot().resolve(document.getStoragePath().substring("local:".length())).normalize();
            if (!file.startsWith(localStorageRoot()) || !Files.exists(file)) {
                throw new com.example.chatapp.exception.ResourceNotFoundException("Document not found");
            }
            return new StoredFile(new UrlResource(file.toUri()), document.getDocumentType(), document.getFileName());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read file: " + rootCauseMessage(e), e);
        }
    }

    public void delete(String id, String userId) {
        Document document = get(id, userId);
        try {
            if (document.getStoragePath() != null && document.getStoragePath().startsWith("local:")) {
                Files.deleteIfExists(localStorageRoot().resolve(document.getStoragePath().substring("local:".length())).normalize());
            } else {
                Blob blob = resolveBucket().get(document.getStoragePath());
                if (blob != null) {
                    blob.delete();
                }
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

    private StorageResult storeFile(MultipartFile file, String id, String userId, String safeName, String storagePath, String contentType) throws IOException {
        try {
            String downloadToken = UUID.randomUUID().toString();
            Bucket bucket = resolveBucket();
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket.getName(), storagePath)
                    .setContentType(contentType)
                    .setMetadata(Map.of("firebaseStorageDownloadTokens", downloadToken))
                    .setContentDisposition("inline; filename=\"" + safeName.replace("\"", "") + "\"")
                    .build();
            Blob blob = bucket.getStorage().create(blobInfo, file.getBytes());
            try {
                blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
            } catch (Exception ignored) {
            }
            return new StorageResult(storagePath, firebaseDownloadUrl(bucket.getName(), storagePath, downloadToken));
        } catch (Exception firebaseError) {
            return storeLocal(file, id, userId, safeName);
        }
    }

    private StorageResult storeLocal(MultipartFile file, String id, String userId, String safeName) throws IOException {
        String localPath = "documents/" + safeSegment(userId) + "/" + id + "-" + safeName;
        Path destination = localStorageRoot().resolve(localPath).normalize();
        Files.createDirectories(destination.getParent());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return new StorageResult("local:" + localPath, localDownloadUrl(id));
    }

    private Path localStorageRoot() throws IOException {
        Path root = Path.of(localUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    private String localDownloadUrl(String id) {
        return backendUrl.replaceFirst("/+$", "") + "/api/documents/files/" + encode(id);
    }

    private String safeSegment(String value) {
        return value == null || value.isBlank() ? "anonymous" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File is too large");
        }
    }

    private String resolveContentType(MultipartFile file, String fileName) {
        String providedType = file.getContentType();
        if (providedType != null
                && !providedType.isBlank()
                && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(providedType)) {
            return providedType;
        }
        return MediaTypeFactory.getMediaType(fileName)
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private Bucket resolveBucket() {
        RuntimeException lastError = null;
        for (String bucketName : bucketCandidates()) {
            try {
                Bucket bucket = storageClient.bucket(bucketName);
                if (bucket != null) {
                    return bucket;
                }
            } catch (IllegalArgumentException | StorageException e) {
                lastError = e;
            }
        }
        throw new IllegalStateException("Firebase Storage bucket is not available. Configure FIREBASE_STORAGE_BUCKET.", lastError);
    }

    private List<String> bucketCandidates() {
        return new LinkedHashSet<>(List.of(
                storageBucket == null ? "" : normalizeBucketName(storageBucket),
                projectId + ".appspot.com",
                projectId + ".firebasestorage.app"
        )).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private String firebaseDownloadUrl(String bucketName, String path, String token) {
        return "https://firebasestorage.googleapis.com/v0/b/"
                + normalizeBucketName(bucketName)
                + "/o/"
                + encode(path)
                + "?alt=media&token="
                + encode(token);
    }

    private String normalizeBucketName(String bucketName) {
        return bucketName == null
                ? ""
                : bucketName.replaceFirst("^gs://", "").replaceFirst("/+$", "");
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null || current.getMessage().isBlank()
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private record StorageResult(String path, String url) {
    }

    public record StoredFile(Resource resource, String contentType, String fileName) {
    }
}
