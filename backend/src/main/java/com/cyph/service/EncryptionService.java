package com.cyph.service;

/**
 * Abstraction for symmetric encryption/decryption of message payloads.
 * Implementations use AES-256-GCM; key and nonce are managed by the caller (per-message).
 */
public interface EncryptionService {

    /**
     * Encrypt plaintext and return ciphertext. The implementation will generate
     * a new key and nonce; these must be stored with the ciphertext for decryption.
     */
    EncryptionResult encrypt(byte[] plaintext);

    /**
     * Decrypt ciphertext with the given key and nonce.
     *
     * @throws com.cyph.service.DecryptionException if decryption fails (e.g. tampered data)
     */
    byte[] decrypt(byte[] key, byte[] nonce, byte[] ciphertext);

    /**
     * Result of encryption: ciphertext plus the key and nonce to store.
     */
    record EncryptionResult(byte[] key, byte[] nonce, byte[] ciphertext) {}
}
