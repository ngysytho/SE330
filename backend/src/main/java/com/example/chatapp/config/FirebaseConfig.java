package com.example.chatapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Value("${firebase.storage-bucket:}")
    private String storageBucket;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream serviceAccount = openServiceAccount()) {
            String bucket = storageBucket == null || storageBucket.isBlank()
                    ? projectId + ".appspot.com"
                    : storageBucket;
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .setStorageBucket(bucket)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public StorageClient storageClient(FirebaseApp firebaseApp) {
        return StorageClient.getInstance(firebaseApp);
    }

    private InputStream openServiceAccount() throws IOException {
        ClassPathResource resource = new ClassPathResource(serviceAccountPath);
        if (resource.exists()) {
            return resource.getInputStream();
        }
        Path path = Path.of(serviceAccountPath);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }
        throw new IOException("Firebase service account file not found: " + serviceAccountPath);
    }
}
