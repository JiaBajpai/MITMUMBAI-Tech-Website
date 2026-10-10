package com.mittechkernel.backend.common.github;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class GitHubTokenCipher {

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String KEY_VERSION = "v1";

    private final GitHubProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public GitHubTokenCipher(GitHubProperties properties) {
        this.properties = properties;
    }

    public EncryptedToken encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("GitHub access token is required");
        }
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new EncryptedToken(ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array(), KEY_VERSION);
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    public void validateConfiguration() {
        secretKey();
    }

    public String decrypt(byte[] value, String keyVersion) {
        if (!KEY_VERSION.equals(keyVersion)) {
            throw new IllegalStateException("Unsupported GitHub credential encryption key version");
        }
        if (value == null || value.length <= NONCE_LENGTH) {
            throw new IllegalStateException("Stored GitHub credential is invalid");
        }
        try {
            byte[] nonce = Arrays.copyOfRange(value, 0, NONCE_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(value, NONCE_LENGTH, value.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decrypt GitHub credential", ex);
        }
    }

    private SecretKeySpec secretKey() {
        String configuredKey = properties.getTokenEncryptionKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("GITHUB_TOKEN_ENCRYPTION_KEY must be configured");
        }
        try {
            byte[] key = Base64.getDecoder().decode(configuredKey);
            if (key.length != 32) {
                throw new IllegalStateException("GITHUB_TOKEN_ENCRYPTION_KEY must be a Base64-encoded 256-bit key");
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("GITHUB_TOKEN_ENCRYPTION_KEY must be a Base64-encoded 256-bit key", ex);
        }
    }

    public record EncryptedToken(byte[] ciphertext, String keyVersion) {
    }
}
