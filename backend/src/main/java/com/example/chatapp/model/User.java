package com.example.chatapp.model;

import com.example.chatapp.enums.Gender;
import com.example.chatapp.enums.UserRole;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String uid;
    private String passwordHash;
    private String name;
    private String gmail;
    private String phoneNumber;
    private String address;
    private String avatarImage;
    private String birthday;
    private Gender gender;
    private String note;
    private List<String> fcmTokens;
    private UserRole role;
    private String code;
    private Boolean isOnline;
    private Long lastSeenAt;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
