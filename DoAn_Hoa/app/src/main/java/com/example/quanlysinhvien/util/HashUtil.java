package com.example.quanlysinhvien.util;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class HashUtil {
    private static final int SALT_LEN = 16;
    private static final int ITERATIONS = 100000; // adjust as needed
    private static final int KEY_LENGTH = 256; // bits

    public static String hashPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = generateSalt();
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);
        return ITERATIONS + ":" + saltB64 + ":" + hashB64;
    }

    public static boolean verifyPassword(String password, String stored) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (password == null || stored == null) return false;
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        int it = Integer.parseInt(parts[0]);
        byte[] salt = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] hash = Base64.decode(parts[2], Base64.NO_WRAP);
        byte[] candidate = pbkdf2(password.toCharArray(), salt, it, KEY_LENGTH);
        return slowEquals(hash, candidate);
    }

    private static byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LEN];
        sr.nextBytes(salt);
        return salt;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.min(a.length, b.length); i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }
}

