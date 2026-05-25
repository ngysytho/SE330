package com.example.chatapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private String id;
    private String userId;
    private String uid;
    private Long loggedInAt;
    private Long loggedOutAt;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
