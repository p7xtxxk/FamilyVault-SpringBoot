package com.familyvault.controller;

import com.familyvault.model.User;
import com.familyvault.model.VaultDocument;
import com.familyvault.service.AuthService;
import com.familyvault.service.DocumentService;
import com.familyvault.service.DocumentService.DocumentBytes;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;
    private final AuthService authService;

    public DocumentController(DocumentService documentService, AuthService authService) {
        this.documentService = documentService;
        this.authService = authService;
    }

    private User requireUser(String token) {
        User user = authService.getCurrentUser(token);
        if (user == null) throw new UnauthorizedException("NOT REGISTERED");
        return user;
    }

    private User requireAdmin(String token) {
        User user = requireUser(token);
        if (!authService.isAdmin(user.getEmail())) {
            throw new ForbiddenException("YOUR NOT ISHAN");
        }
        return user;
    }

    @GetMapping("/documents")
    public ResponseEntity<?> listDocuments(@CookieValue(name = "access_token", required = false) String token) {
        requireUser(token);
        var docs = documentService.getAllDocuments();
        return ResponseEntity.ok(Map.of(
                "documents", docs,
                "count", docs.size(),
                "limit", DocumentService.MAX_DOCUMENTS
        ));
    }

    @GetMapping("/get_document/{documentId}")
    public ResponseEntity<byte[]> getDocument(
            @PathVariable String documentId,
            @CookieValue(name = "access_token", required = false) String token) {
        requireUser(token);
        try {
            DocumentBytes result = documentService.getDocumentBytes(documentId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set("Content-Disposition", "inline; filename=\"" + result.filename() + "\"");
            headers.setCacheControl("no-store, no-cache, must-revalidate");
            headers.set("X-Content-Type-Options", "nosniff");
            return new ResponseEntity<>(result.data(), headers, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("{\"detail\":\"Documents not found\"}").getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("{\"detail\":\"Error retrieving doc:" + e.getMessage() + "\"}").getBytes());
        }
    }

    @PostMapping("/upload-document")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("title") String title,
            @RequestParam("document_type") String documentType,
            @RequestParam("file") MultipartFile file,
            @CookieValue(name = "access_token", required = false) String token) {
        requireAdmin(token);

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("detail", "ONLY PDF ALLOWED!!"));
        }
        if (documentService.getDocumentCount() >= DocumentService.MAX_DOCUMENTS) {
            return ResponseEntity.badRequest()
                    .body(Map.of("detail", "Document limit of " + DocumentService.MAX_DOCUMENTS + " reached"));
        }

        try {
            byte[] contents = file.getBytes();
            if (contents.length == 0) {
                return ResponseEntity.badRequest().body(Map.of("detail", "Upload empty"));
            }
            if (contents.length > 20 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("detail", "File Too large"));
            }

            VaultDocument doc = documentService.uploadDocument(title, documentType, contents, filename);
            return ResponseEntity.ok(Map.of("message", "Document Uploaded", "id", doc.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("detail", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("detail", "Upload failed " + e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable String documentId,
            @CookieValue(name = "access_token", required = false) String token) {
        requireAdmin(token);
        boolean success = documentService.deleteDocument(documentId);
        if (!success) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "Document not found."));
        }
        return ResponseEntity.ok(Map.of("message", "Document deleted."));
    }

    // Exception classes for clean error handling
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String msg) { super(msg); }
    }
}
