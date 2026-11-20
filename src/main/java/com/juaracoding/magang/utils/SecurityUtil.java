package com.juaracoding.magang.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utilitas encode/decode Base64 untuk menyembunyikan kredensial sensitif
 * agar repositori tetap aman saat dipublikasikan ke GitHub.
 */
public final class SecurityUtil {

    private SecurityUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Mendekode string Base64 menjadi plaintext di dalam memori runtime.
     *
     * @param base64String string ter-encode Base64
     * @return teks asli hasil decode, atau string kosong jika input null/kosong
     */
    public static String decode(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return "";
        }
        byte[] decodedBytes = Base64.getDecoder().decode(base64String.trim());
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Meng-encode string plaintext menjadi format Base64.
     *
     * @param plainText string plaintext
     * @return string ter-encode Base64
     */
    public static String encode(String plainText) {
        if (plainText == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
    }
}

