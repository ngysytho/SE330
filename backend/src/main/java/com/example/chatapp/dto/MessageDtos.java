package com.example.chatapp.dto;

import com.example.chatapp.enums.MessageType;
import java.util.List;

public final class MessageDtos {
    private MessageDtos() {
    }

    public record CreateMessageRequest(
            String roomId,
            String content,
            MessageType messageType,
            List<String> attachmentIds
    ) {
    }

    public record UpdateMessageRequest(String messageId, String content) {
    }

    public record DeleteMessageRequest(String messageId, String roomId) {
    }

    public record TypingRequest(String roomId, Boolean typing) {
    }
}
