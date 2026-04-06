package com.poly.java5.Utils;
import java.security.MessageDigest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class Utils {
	 // ===== SESSION KEY =====
    public static final String SESSION_USER_ID = "USER_ID";

    // ===== SESSION =====

    // Lưu userId vào session (khi login thành công)
    public static void setUserSession(Integer userId, HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_USER_ID, userId);
    }

    // Lấy userId từ session
    public static Integer getUserIdFromSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;

        Object value = session.getAttribute(SESSION_USER_ID);
        return value instanceof Integer ? (Integer) value : null;
    }

    // Xóa session đăng nhập (logout)
    public static void clearUserSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    // ===== PASSWORD =====

    // Hash password SHA-256 (đủ cho đồ án)
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) return null;
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
