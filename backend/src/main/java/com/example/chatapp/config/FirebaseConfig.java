package com.example.chatapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseConfig {
    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.service-account-path:firebase-service-account.json}")
    private String serviceAccountPath;

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${firebase.service-account-base64:}")
    private String serviceAccountBase64;

    @Value("${firebase.storage-bucket:}")
    private String storageBucket;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        String bucket = storageBucket == null || storageBucket.isBlank()
                ? projectId + ".appspot.com"
                : storageBucket;
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(firebaseCredentials())
                .setProjectId(projectId)
                .setStorageBucket(bucket)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public StorageClient storageClient(FirebaseApp firebaseApp) {
        return StorageClient.getInstance(firebaseApp);
    }

    private GoogleCredentials firebaseCredentials() throws IOException {
        List<String> attempted = new ArrayList<>();

        InputStream serviceAccount = openConfiguredServiceAccount(attempted);
        if (serviceAccount != null) {
            try (serviceAccount) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        try {
            return GoogleCredentials.getApplicationDefault();
        } catch (IOException error) {
            attempted.add("Application Default Credentials");
            throw new IOException("Firebase credentials not found. Set FIREBASE_SERVICE_ACCOUNT_PATH, "
                    + "FIREBASE_SERVICE_ACCOUNT_JSON, FIREBASE_SERVICE_ACCOUNT_BASE64, or GOOGLE_APPLICATION_CREDENTIALS. "
                    + "Attempted: " + String.join(", ", attempted), error);
        }
    }

    private InputStream openConfiguredServiceAccount(List<String> attempted) throws IOException {
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            attempted.add(serviceAccountPath);
            ClassPathResource resource = new ClassPathResource(serviceAccountPath);
            if (resource.exists()) {
                return resource.getInputStream();
            }
            Path path = Path.of(serviceAccountPath);
            if (Files.exists(path)) {
                return Files.newInputStream(path);
            }
        }

        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            attempted.add("FIREBASE_SERVICE_ACCOUNT_JSON");
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }

        if (serviceAccountBase64 != null && !serviceAccountBase64.isBlank()) {
            attempted.add("FIREBASE_SERVICE_ACCOUNT_BASE64");
            byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64);
            return new ByteArrayInputStream(decoded);
        }

        return null;
    }
}
