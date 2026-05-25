package com.example.chatapp.service;

import com.example.chatapp.dto.UserDtos.UpdateUserRequest;
import com.example.chatapp.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final FirebaseService firebase;

    public UserService(FirebaseService firebase) {
        this.firebase = firebase;
    }

    public User get(String id) {
        User user = firebase.get(FirebaseService.USERS, id, User.class);
        if (user.getDeletedAt() != null) {
            throw new com.example.chatapp.exception.ResourceNotFoundException("User not found");
        }
        return user;
    }

    public User update(String id, UpdateUserRequest request) {
        Map<String, Object> updates = new HashMap<>();
        put(updates, "name", request.name());
        put(updates, "phoneNumber", request.phoneNumber());
        put(updates, "address", request.address());
        put(updates, "avatarImage", request.avatarImage());
        put(updates, "birthday", request.birthday());
        put(updates, "gender", request.gender());
        put(updates, "note", request.note());
        updates.put("updatedAt", firebase.now());
        firebase.update(FirebaseService.USERS, id, updates);
        return get(id);
    }

    public List<User> search(String keyword) {
        String lower = keyword == null ? "" : keyword.toLowerCase();
        return firebase.all(FirebaseService.USERS, User.class).stream()
                .filter(user -> user.getDeletedAt() == null)
                .filter(user -> lower.isBlank()
                        || contains(user.getName(), lower)
                        || contains(user.getGmail(), lower)
                        || contains(user.getPhoneNumber(), lower))
                .limit(30)
                .toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void put(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
