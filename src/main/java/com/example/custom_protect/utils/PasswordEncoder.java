package com.example.custom_protect.utils;

import java.util.Base64;

public class PasswordEncoder {

    public static String encodePassword(String password) {
        if (password == null) return null;
        return Base64.getEncoder().encodeToString(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return encodePassword(rawPassword).equals(encodedPassword);
    }
}