package com.example.chatapp.service;

import com.example.chatapp.exception.ResourceNotFoundException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {
    public static final String USERS = "users";
    public static final String CHAT_ROOMS = "chatRooms";
    public static final String CHAT_ROOM_MEMBERS = "chatRoomMembers";
    public static final String MESSAGES = "messages";
    public static final String DOCUMENTS = "documents";
    public static final String FORUM_POSTS = "forumPosts";
    public static final String FORUM_COMMENTS = "forumComments";
    public static final String SESSIONS = "sessions";
    public static final String PRESENCE = "presence";

    private final Firestore firestore;

    public FirebaseService(Firestore firestore) {
        this.firestore = firestore;
    }

    public long now() {
        return System.currentTimeMillis();
    }

    public String newId(String collection) {
        return firestore.collection(collection).document().getId();
    }

    public CollectionReference collection(String collection) {
        return firestore.collection(collection);
    }

    public <T> T get(String collection, String id, Class<T> type) {
        try {
            DocumentSnapshot snapshot = firestore.collection(collection).document(id).get().get();
            if (!snapshot.exists()) {
                throw new ResourceNotFoundException(collection + " item not found: " + id);
            }
            return snapshot.toObject(type);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> List<T> all(String collection, Class<T> type) {
        return run(firestore.collection(collection).get()).stream()
                .map(document -> document.toObject(type))
                .toList();
    }

    public <T> void save(String collection, String id, T value) {
        waitFor(firestore.collection(collection).document(id).set(value));
    }

    public void update(String collection, String id, Map<String, Object> values) {
        waitFor(firestore.collection(collection).document(id).update(values));
    }

    public DocumentReference ref(String collection, String id) {
        return firestore.collection(collection).document(id);
    }

    public List<QueryDocumentSnapshot> run(ApiFuture<com.google.cloud.firestore.QuerySnapshot> query) {
        try {
            return query.get().getDocuments();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    public void waitFor(ApiFuture<?> future) {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}
