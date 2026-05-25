package com.example.chatapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String id;
    private String userId;
    private String fileName;
    private String fileUrl;
    private String storagePath;
    private String documentType;
    private Long size;
    private String note;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
