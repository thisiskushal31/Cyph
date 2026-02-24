package com.cyph.service;

/**
 * Thrown when decryption fails (e.g. invalid key/nonce or tampered ciphertext).
 */
public class DecryptionException extends RuntimeException {

    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
