package com.example.custom_protect.utils;
import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtils {
    private PasswordUtils() {}

    public static String encode(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt(10));
    }

    public static boolean matches(String raw, String encoded) {
        return BCrypt.checkpw(raw, encoded);
    }
}
