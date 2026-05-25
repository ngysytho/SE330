package com.example.chatapp.dto;

public final class DocumentDtos {
    private DocumentDtos() {
    }

    public record DocumentResponse(
            String id,
            String fileName,
            String fileUrl,
            String storagePath,
            String documentType,
            Long size,
            String note
    ) {
    }
}
