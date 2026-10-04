package com.example.wipeguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Iptal kodunu PBKDF2 ile hashleyip device-protected depoda saklar. */
public class CodeStore {
    private static final int ITER = 60000;

    public static boolean isSet(Context c) {
        return Prefs.get(c).contains("code_hash");
    }

    public static int length(Context c) {
        return Prefs.get(c).getInt("code_len", 0);
    }

    public static void set(Context c, String code) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] h = hash(code, salt);
        if (h == null) return;
        Prefs.get(c).edit()
                .putString("code_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("code_hash", Base64.encodeToString(h, Base64.NO_WRAP))
                .putInt("code_len", code.length())
                .commit();
    }

    public static boolean verify(Context c, String code) {
        SharedPreferences p = Prefs.get(c);
        String s = p.getString("code_salt", null);
        String h = p.getString("code_hash", null);
        if (s == null || h == null) return false;
        byte[] got = hash(code, Base64.decode(s, Base64.NO_WRAP));
        return got != null && MessageDigest.isEqual(Base64.decode(h, Base64.NO_WRAP), got);
    }

    private static byte[] hash(String code, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, ITER, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            return null;
        }
    }
}
