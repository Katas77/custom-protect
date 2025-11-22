package com.example.custom_protect.utils;

import java.util.Base64;

public class Password {

    /**
     * Кодирует строку пароля в Base64.
     *
     * @param password Исходная строка пароля.
     * @return Закодированная строка в формате Base64.
     */
    public static String encodePassword(String password) {
        if (password == null) {
            return null;
        }
        // Преобразуем строку в байты, используя стандартную кодировку UTF-8
        byte[] passwordBytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // Кодируем байты в Base64
        return Base64.getEncoder().encodeToString(passwordBytes);
    }

    public static String decodePassword(String encodedPassword) {
        if (encodedPassword == null) {
            return null;
        }
        try {
            // Декодируем строку Base64 в байты
            byte[] decodedBytes = Base64.getDecoder().decode(encodedPassword);
            // Преобразуем байты обратно в строку, используя UTF-8
            return new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Бросаем исключение, если закодированная строка недействительна
            throw new IllegalArgumentException("Неверный формат Base64: " + e.getMessage(), e);
        }
    }

}