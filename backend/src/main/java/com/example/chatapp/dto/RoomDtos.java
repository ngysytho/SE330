package com.example.chatapp.dto;

import java.util.List;

public final class RoomDtos {
    private RoomDtos() {
    }

    public record CreatePrivateRoomRequest(String otherUserId) {
    }

    public record CreateRoomRequest(
            String name,
            String description,
            String avatarUrl,
            Boolean isPublic,
            List<String> memberIds
    ) {
    }

    public record UpdateRoomRequest(
            String name,
            String description,
            String avatarUrl,
            Boolean isPublic
    ) {
    }
}
