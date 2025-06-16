package com.example.whatsappclone.utils;

import android.content.Context;
import android.content.SharedPreferences;

import javax.crypto.SecretKey;

public class MyKeyStorage {
    private static final String PREFS_NAME = "MyKeys";
    private static final String AES_KEY = "aes_key";

    public static void saveAESKey(Context context, SecretKey key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(AES_KEY, AESUtils.keyToString(key)).apply();
    }

    public static SecretKey loadAESKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String keyStr = prefs.getString(AES_KEY, null);
        if (keyStr == null) return null;
        return AESUtils.stringToKey(keyStr);
    }
}
