package com.example.whatsappclone.utils;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {
    private static final String TAG = "AESUtils";
    private static final String AES = "AES";
    private static final String AES_MODE = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16; // 128 bits for CBC mode
    private static final int KEY_SIZE = 256; // 256-bit key for better security

    public static SecretKey generateKey() throws Exception {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES);
            keyGen.init(KEY_SIZE);
            SecretKey key = keyGen.generateKey();
            Log.d(TAG, "AES key generated successfully");
            return key;
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate AES key", e);
            throw new Exception("Failed to generate AES key: " + e.getMessage(), e);
        }
    }

    public static String keyToString(SecretKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
    }

    public static SecretKey stringToKey(String keyStr) {
        if (keyStr == null || keyStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Key string cannot be null or empty");
        }

        try {
            byte[] decodedKey = Base64.decode(keyStr.trim(), Base64.NO_WRAP);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, AES);
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert string to key", e);
            throw new IllegalArgumentException("Invalid key string format", e);
        }
    }

    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        if (plainText == null) {
            throw new IllegalArgumentException("Plain text cannot be null");
        }
        if (secretKey == null) {
            throw new IllegalArgumentException("Secret key cannot be null");
        }

        try {
            Cipher cipher = Cipher.getInstance(AES_MODE);
            byte[] iv = new byte[IV_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            String result = Base64.encodeToString(combined, Base64.NO_WRAP);
            Log.d(TAG, "Text encrypted successfully");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            throw new Exception("Encryption failed: " + e.getMessage(), e);
        }
    }

    public static String decrypt(String cipherText, SecretKey secretKey) throws Exception {
        if (cipherText == null || cipherText.trim().isEmpty()) {
            throw new IllegalArgumentException("Cipher text cannot be null or empty");
        }
        if (secretKey == null) {
            throw new IllegalArgumentException("Secret key cannot be null");
        }

        try {
            Log.d(TAG, "Starting decryption for: " + cipherText);
            byte[] combined = Base64.decode(cipherText.trim(), Base64.NO_WRAP);
            Log.d(TAG, "Decoded combined length: " + combined.length);

            if (combined.length < IV_SIZE + 1) {
                throw new Exception("Invalid ciphertext: too short");
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_SIZE);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_SIZE, combined.length);
            Log.d(TAG, "IV length: " + iv.length + ", Encrypted data length: " + encrypted.length);

            Cipher cipher = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            String result = new String(decrypted, StandardCharsets.UTF_8);
            Log.d(TAG, "Decryption successful: " + result);

            return result;

        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            throw new Exception("Decryption failed: " + e.getMessage(), e);
        }
    }

    public static boolean isValidEncryptedData(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return false;
        }

        try {
            byte[] data = Base64.decode(encryptedText.trim(), Base64.NO_WRAP);
            return data.length >= IV_SIZE + 16;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean testAES() {
        try {
            SecretKey testKey = generateKey();
            String originalText = "Hello, this is a test message for AES encryption!";
            String encrypted = encrypt(originalText, testKey);
            Log.d(TAG, "Test encrypted: " + encrypted);
            String decrypted = decrypt(encrypted, testKey);
            Log.d(TAG, "Test decrypted: " + decrypted);
            boolean success = originalText.equals(decrypted);
            Log.d(TAG, "AES test " + (success ? "PASSED" : "FAILED"));
            return success;

        } catch (Exception e) {
            Log.e(TAG, "AES test failed", e);
            return false;
        }
    }
}
