package org.delcom.ifs25042springbootcashflow.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility hashing password menggunakan SHA-256.
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        MessageDigest digest = messageDigest("SHA-256");
        byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }

    static MessageDigest messageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritma hash tidak tersedia: " + algorithm, ex);
        }
    }

    public static boolean matches(String rawPassword, String passwordHash) {
        return hash(rawPassword).equals(passwordHash);
    }
}
