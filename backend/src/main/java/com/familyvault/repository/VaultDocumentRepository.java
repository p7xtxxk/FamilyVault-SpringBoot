package com.familyvault.repository;

import com.familyvault.model.VaultDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VaultDocumentRepository extends MongoRepository<VaultDocument, String> {
}
