package com.example.chatapp.controller;

import com.example.chatapp.model.Document;
import com.example.chatapp.security.SecurityUtils;
import com.example.chatapp.service.DocumentService;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public Document upload(@RequestParam("file") MultipartFile file, @RequestParam(required = false) String note) {
        return documentService.upload(file, note, SecurityUtils.currentUserId());
    }

    @GetMapping("/my")
    public List<Document> my() {
        return documentService.my(SecurityUtils.currentUserId());
    }

    @GetMapping("/{id}")
    public Document get(@PathVariable String id) {
        return documentService.get(id, SecurityUtils.currentUserId());
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> file(@PathVariable String id) {
        DocumentService.StoredFile file = documentService.downloadFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.fileName()).build().toString())
                .body(file.resource());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        documentService.delete(id, SecurityUtils.currentUserId());
    }
}
