package com.example.chatapp.dto;

import com.example.chatapp.enums.Gender;
import com.example.chatapp.enums.UserRole;
import com.example.chatapp.model.User;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(String gmail, String password, String name) {
    }

    public record LoginRequest(String gmail, String password) {
    }

    public record UserResponse(
            String id,
            String uid,
            String name,
            String gmail,
            String phoneNumber,
            String address,
            String avatarImage,
            String birthday,
            Gender gender,
            String note,
            UserRole role,
            String code,
            Boolean isOnline,
            Long lastSeenAt,
            Long createdAt,
            Long updatedAt,
            Long deletedAt
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getUid(),
                    user.getName(),
                    user.getGmail(),
                    user.getPhoneNumber(),
                    user.getAddress(),
                    user.getAvatarImage(),
                    user.getBirthday(),
                    user.getGender(),
                    user.getNote(),
                    user.getRole(),
                    user.getCode(),
                    user.getIsOnline(),
                    user.getLastSeenAt(),
                    user.getCreatedAt(),
                    user.getUpdatedAt(),
                    user.getDeletedAt()
            );
        }
    }

    public record AuthResponse(String accessToken, String tokenType, UserResponse user) {
        public AuthResponse(String accessToken, User user) {
            this(accessToken, "Bearer", UserResponse.from(user));
        }
    }
}
