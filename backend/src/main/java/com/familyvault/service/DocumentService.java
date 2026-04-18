package com.familyvault.service;

import com.familyvault.model.VaultDocument;
import com.familyvault.repository.VaultDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentService {

    private final VaultDocumentRepository docRepo;
    private final EncryptionService encryptionService;
    private final Path secureDocsDir;

    public static final int MAX_DOCUMENTS = 60;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf");

    public DocumentService(VaultDocumentRepository docRepo,
                           EncryptionService encryptionService,
                           @Value("${app.secure-docs-dir}") String secureDocsDirStr) {
        this.docRepo = docRepo;
        this.encryptionService = encryptionService;
        this.secureDocsDir = Paths.get(secureDocsDirStr).toAbsolutePath();
    }

    public List<Map<String, Object>> getAllDocuments() {
        List<VaultDocument> docs = docRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (VaultDocument doc : docs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", doc.getId());
            map.put("_id", doc.getId());
            map.put("title", doc.getTitle());
            map.put("document_type", doc.getDocumentType());
            map.put("uploaded_at", doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : null);
            result.add(map);
        }
        return result;
    }

    public long getDocumentCount() {
        return docRepo.count();
    }

    public VaultDocument uploadDocument(String title, String documentType, byte[] fileBytes, String originalFilename) throws IOException {
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only PDF");
        }
        if (getDocumentCount() >= MAX_DOCUMENTS) {
            throw new IllegalArgumentException("LIMIT REACHED OF " + MAX_DOCUMENTS);
        }

        String uniqueName = UUID.randomUUID().toString().replace("-", "") + ".enc";
        Path filepath = secureDocsDir.resolve(uniqueName);

        encryptionService.saveEncryptedFile(fileBytes, filepath);

        VaultDocument doc = new VaultDocument();
        doc.setTitle(title);
        doc.setDocumentType(documentType);
        doc.setFilePath(filepath.toString());
        doc.setOriginalName(originalFilename);
        doc.setUploadedAt(Instant.now());

        return docRepo.save(doc);
    }

    public record DocumentBytes(byte[] data, String filename) {}

    public DocumentBytes getDocumentBytes(String documentId) throws IOException {
        VaultDocument doc = docRepo.findById(documentId)
                .orElseThrow(() -> new FileNotFoundException("NOT FOUND"));

        byte[] decrypted = encryptionService.loadDecryptedFile(Path.of(doc.getFilePath()));
        String filename = doc.getOriginalName() != null ? doc.getOriginalName() : "document.pdf";
        return new DocumentBytes(decrypted, filename);
    }

    public boolean deleteDocument(String documentId) {
        Optional<VaultDocument> opt = docRepo.findById(documentId);
        if (opt.isEmpty()) return false;

        VaultDocument doc = opt.get();
        try {
            java.nio.file.Files.deleteIfExists(Path.of(doc.getFilePath()));
        } catch (IOException ignored) {}

        docRepo.deleteById(documentId);
        return true;
    }
}
