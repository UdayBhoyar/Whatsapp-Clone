package com.example.whatsappclone.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Manages RSA key pair generation, storage, and retrieval
 * Private keys are stored locally, public keys are shared via Firebase
 */
public class RSAKeyManager {
    private static final String TAG = "RSAKeyManager";
    private static final String PREFS_NAME = "RSAKeys";
    private static final String PRIVATE_KEY = "rsa_private_key";
    private static final String PUBLIC_KEY = "rsa_public_key";
    private static final int KEY_SIZE = 2048; // RSA key size in bits

    /**
     * Generate a new RSA key pair
     */
    public static KeyPair generateKeyPair() throws Exception {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();
            Log.d(TAG, "RSA key pair generated successfully");
            return keyPair;
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate RSA key pair", e);
            throw new Exception("Failed to generate RSA key pair: " + e.getMessage(), e);
        }
    }

    /**
     * Save RSA key pair to SharedPreferences (local device storage)
     * IMPORTANT: Only the private key should be stored locally
     */
    public static void saveKeyPair(Context context, KeyPair keyPair) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Save private key
        String privateKeyStr = Base64.encodeToString(
            keyPair.getPrivate().getEncoded(), 
            Base64.NO_WRAP
        );
        editor.putString(PRIVATE_KEY, privateKeyStr);

        // Save public key (for convenience, though it's also in Firebase)
        String publicKeyStr = Base64.encodeToString(
            keyPair.getPublic().getEncoded(), 
            Base64.NO_WRAP
        );
        editor.putString(PUBLIC_KEY, publicKeyStr);

        editor.apply();
        Log.d(TAG, "RSA key pair saved to SharedPreferences");
    }

    /**
     * Load private key from local storage
     */
    public static PrivateKey loadPrivateKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String keyStr = prefs.getString(PRIVATE_KEY, null);
        
        if (keyStr == null) {
            Log.w(TAG, "No private key found in storage");
            return null;
        }

        try {
            byte[] keyBytes = Base64.decode(keyStr, Base64.NO_WRAP);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load private key", e);
            return null;
        }
    }

    /**
     * Load public key from local storage
     */
    public static PublicKey loadPublicKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String keyStr = prefs.getString(PUBLIC_KEY, null);
        
        if (keyStr == null) {
            Log.w(TAG, "No public key found in storage");
            return null;
        }

        return stringToPublicKey(keyStr);
    }

    /**
     * Convert public key to Base64 string for storage in Firebase
     */
    public static String publicKeyToString(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        return Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);
    }

    /**
     * Convert Base64 string to PublicKey (from Firebase)
     */
    public static PublicKey stringToPublicKey(String keyStr) {
        if (keyStr == null || keyStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Key string cannot be null or empty");
        }

        try {
            byte[] keyBytes = Base64.decode(keyStr.trim(), Base64.NO_WRAP);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert string to public key", e);
            throw new IllegalArgumentException("Invalid public key string format", e);
        }
    }

    /**
     * Check if RSA keys exist locally
     */
    public static boolean hasKeys(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(PRIVATE_KEY) && prefs.contains(PUBLIC_KEY);
    }

    /**
     * Clear all stored keys (for logout or reset)
     */
    public static void clearKeys(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d(TAG, "All RSA keys cleared from storage");
    }
}
