package com.familyvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-CBC encryption service.
 * Matches the Python encryption.py format exactly:
 *   encrypted = IV (16 bytes) + ciphertext (PKCS5 padded)
 */
@Service
public class EncryptionService {

    private final SecretKeySpec keySpec;
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public EncryptionService(@Value("${app.aes-key}") String aesKeyRaw) {
        // Match Python: key[:32].ljust(32, '0').encode('utf-8')
        String trimmed = aesKeyRaw.length() > 32 ? aesKeyRaw.substring(0, 32) : aesKeyRaw;
        StringBuilder padded = new StringBuilder(trimmed);
        while (padded.length() < 32) {
            padded.append('0');
        }
        this.keySpec = new SecretKeySpec(padded.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
    }

    public byte[] encrypt(byte[] data) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[16];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(data);

            // IV + ciphertext (same format as Python)
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] data) {
        try {
            byte[] iv = Arrays.copyOfRange(data, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(data, 16, data.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public void saveEncryptedFile(byte[] plaintext, Path filepath) throws IOException {
        Files.createDirectories(filepath.getParent());
        byte[] encrypted = encrypt(plaintext);
        Files.write(filepath, encrypted);
    }

    public byte[] loadDecryptedFile(Path filepath) throws IOException {
        byte[] encrypted = Files.readAllBytes(filepath);
        return decrypt(encrypted);
    }
}
