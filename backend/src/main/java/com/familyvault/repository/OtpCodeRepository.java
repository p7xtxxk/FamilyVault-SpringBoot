package com.familyvault.repository;

import com.familyvault.model.OtpCode;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpCodeRepository extends MongoRepository<OtpCode, String> {
    void deleteAllByEmail(String email);
    long countByEmailAndCreatedAtAfter(String email, Instant after);
    Optional<OtpCode> findByEmailAndOtpAndExpiresAtAfter(String email, String otp, Instant now);
}
