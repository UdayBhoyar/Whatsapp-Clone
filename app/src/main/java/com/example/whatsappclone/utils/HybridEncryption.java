package com.example.whatsappclone.utils;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * Hybrid Encryption: RSA + AES
 * 
 * How it works:
 * 1. Generate a random AES session key
 * 2. Encrypt the message with the AES session key
 * 3. Encrypt the AES session key with recipient's RSA public key
 * 4. Send both encrypted message and encrypted session key
 * 
 * Decryption:
 * 1. Decrypt the AES session key using RSA private key
 * 2. Decrypt the message using the decrypted AES session key
 */
public class HybridEncryption {
    private static final String TAG = "HybridEncryption";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * Result class containing both encrypted message and encrypted session key
     */
    public static class EncryptedMessage {
        private String encryptedData;      // Base64 encoded AES-encrypted message
        private String encryptedSessionKey; // Base64 encoded RSA-encrypted AES key

        public EncryptedMessage(String encryptedData, String encryptedSessionKey) {
            this.encryptedData = encryptedData;
            this.encryptedSessionKey = encryptedSessionKey;
        }

        public String getEncryptedData() {
            return encryptedData;
        }

        public String getEncryptedSessionKey() {
            return encryptedSessionKey;
        }

        /**
         * Combine into a single string for storage
         * Format: encryptedSessionKey::encryptedData
         */
        public String toStorageFormat() {
            return encryptedSessionKey + "::" + encryptedData;
        }

        /**
         * Parse from storage format
         */
        public static EncryptedMessage fromStorageFormat(String combined) {
            String[] parts = combined.split("::", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted message format");
            }
            return new EncryptedMessage(parts[1], parts[0]);
        }
    }

    /**
     * Encrypt a message for a specific recipient
     * 
     * @param plainText Message to encrypt
     * @param recipientPublicKey Recipient's RSA public key
     * @return EncryptedMessage containing encrypted data and encrypted session key
     */
    public static EncryptedMessage encrypt(String plainText, PublicKey recipientPublicKey) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Plain text cannot be null or empty");
        }
        if (recipientPublicKey == null) {
            throw new IllegalArgumentException("Recipient public key cannot be null");
        }

        try {
            // Step 1: Generate a random AES session key
            SecretKey sessionKey = AESUtils.generateKey();
            Log.d(TAG, "Generated session AES key");

            // Step 2: Encrypt the message with AES
            String encryptedMessage = AESUtils.encrypt(plainText, sessionKey);
            Log.d(TAG, "Message encrypted with AES");

            // Step 3: Encrypt the AES session key with RSA public key
            String encryptedSessionKey = encryptAESKeyWithRSA(sessionKey, recipientPublicKey);
            Log.d(TAG, "Session key encrypted with RSA");

            return new EncryptedMessage(encryptedMessage, encryptedSessionKey);

        } catch (Exception e) {
            Log.e(TAG, "Hybrid encryption failed", e);
            throw new Exception("Hybrid encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt a message using the recipient's private key
     * 
     * @param encryptedMessage The encrypted message object
     * @param privateKey Recipient's RSA private key
     * @return Decrypted plain text message
     */
    public static String decrypt(EncryptedMessage encryptedMessage, PrivateKey privateKey) throws Exception {
        if (encryptedMessage == null) {
            throw new IllegalArgumentException("Encrypted message cannot be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }

        try {
            // Step 1: Decrypt the AES session key using RSA private key
            SecretKey sessionKey = decryptAESKeyWithRSA(
                encryptedMessage.getEncryptedSessionKey(), 
                privateKey
            );
            Log.d(TAG, "Session key decrypted with RSA");

            // Step 2: Decrypt the message using the session key
            String plainText = AESUtils.decrypt(encryptedMessage.getEncryptedData(), sessionKey);
            Log.d(TAG, "Message decrypted with AES");

            return plainText;

        } catch (Exception e) {
            Log.e(TAG, "Hybrid decryption failed", e);
            throw new Exception("Hybrid decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypt an AES key using RSA public key
     */
    private static String encryptAESKeyWithRSA(SecretKey aesKey, PublicKey publicKey) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            
            byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
            return Base64.encodeToString(encryptedKey, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt AES key with RSA", e);
            throw new Exception("Failed to encrypt AES key: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt an AES key using RSA private key
     */
    private static SecretKey decryptAESKeyWithRSA(String encryptedKeyStr, PrivateKey privateKey) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            
            byte[] encryptedKey = Base64.decode(encryptedKeyStr, Base64.NO_WRAP);
            byte[] decryptedKey = cipher.doFinal(encryptedKey);

            return AESUtils.stringToKey(Base64.encodeToString(decryptedKey, Base64.NO_WRAP));

        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt AES key with RSA", e);
            throw new Exception("Failed to decrypt AES key: " + e.getMessage(), e);
        }
    }

    /**
     * Test the hybrid encryption system
     */
    public static boolean testHybridEncryption() {
        try {
            // Generate test RSA key pair
            java.security.KeyPair keyPair = RSAKeyManager.generateKeyPair();
            
            // Test message
            String originalMessage = "Hello! This is a test message for hybrid encryption.";
            
            // Encrypt
            EncryptedMessage encrypted = encrypt(originalMessage, keyPair.getPublic());
            Log.d(TAG, "Test encrypted successfully");
            
            // Decrypt
            String decrypted = decrypt(encrypted, keyPair.getPrivate());
            Log.d(TAG, "Test decrypted: " + decrypted);
            
            // Verify
            boolean success = originalMessage.equals(decrypted);
            Log.d(TAG, "Hybrid encryption test " + (success ? "PASSED" : "FAILED"));
            
            return success;

        } catch (Exception e) {
            Log.e(TAG, "Hybrid encryption test failed", e);
            return false;
        }
    }
}
