package com.example.chatapp.dto;

import com.example.chatapp.enums.Gender;

public final class UserDtos {
    private UserDtos() {
    }

    public record UpdateUserRequest(
            String name,
            String phoneNumber,
            String address,
            String avatarImage,
            String birthday,
            Gender gender,
            String note
    ) {
    }

    public record FcmTokenRequest(String token) {
    }
}
