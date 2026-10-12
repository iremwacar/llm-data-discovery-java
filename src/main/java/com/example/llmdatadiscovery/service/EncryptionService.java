package com.example.llmdatadiscovery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class EncryptionService {

    // Şifreleme anahtarını (secret.key) ileride application.properties veya .env'den alacağız
    @Value("${app.encryption.key:jPs-PuJnzomLZHpPUvVBilRbzM3wjI1gzRtlfJ1gP-E=}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";

    public String encryptData(String data) {
        try {
            SecretKeySpec secretKey = getSecretKeySpec();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Şifreleme hatası", e);
        }
    }

    public String decryptData(String encryptedData) {
        try {
            SecretKeySpec secretKey = getSecretKeySpec();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Şifre çözme hatası", e);
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        // Python'daki Fernet anahtarları base64 url-safe formatındadır.
        // Şimdilik en basit haliyle AES'e uygun 32 byte'lık bir anahtara dönüştürüyoruz.
        byte[] keyBytes = Base64.getUrlDecoder().decode(encryptionKey);
        return new SecretKeySpec(keyBytes, 0, 32, ALGORITHM);
    }
}