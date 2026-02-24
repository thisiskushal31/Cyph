package com.cyph.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;

/**
 * AES-256-GCM encryption. One key and nonce per message; 12-byte nonce.
 */
@Service
public class AesGcmEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE_BITS = 256;
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public EncryptionResult encrypt(byte[] plaintext) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE_BITS, secureRandom);
            SecretKey key = keyGen.generateKey();
            byte[] keyBytes = key.getEncoded();

            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            return new EncryptionResult(keyBytes, nonce, ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] key, byte[] nonce, byte[] ciphertext) {
        try {
            SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, nonce);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new DecryptionException("Decryption failed", e);
        }
    }
}
