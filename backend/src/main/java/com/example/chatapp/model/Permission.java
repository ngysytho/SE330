package com.example.chatapp.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    private String id;
    private String userId;
    private String roomId;
    private List<String> permissions;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
