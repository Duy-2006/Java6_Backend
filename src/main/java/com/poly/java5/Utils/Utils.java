package com.poly.java5.Utils;

import java.security.MessageDigest;

public class Utils {

    // ===== PASSWORD =====

    // Hash password SHA-256 (đủ cho đồ án)
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty())
            return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }

}
