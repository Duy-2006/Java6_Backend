package com.poly.java5.Utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;

/**
 * AuthUtil — Tiện ích lấy thông tin user đã xác thực từ SecurityContext.
 * 
 * AuthFilter đã validate JWT Cookie và đặt username vào SecurityContextHolder.
 * Tất cả Controller chỉ cần gọi AuthUtil để lấy userId, không cần tự parse token.
 * 
 * Đây là cách chuẩn Spring Security, đảm bảo Cookie-only bảo mật tuyệt đối.
 */
public class AuthUtil {

    /**
     * Lấy username của user đang đăng nhập từ SecurityContext.
     * @return username hoặc null nếu chưa xác thực
     */
    public static String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    /**
     * Lấy userId của user đang đăng nhập.
     * @param userService service để tra cứu user từ username
     * @return userId hoặc null nếu chưa xác thực
     */
    public static Integer getAuthenticatedUserId(UserService userService) {
        String username = getAuthenticatedUsername();
        if (username == null) return null;

        User user = userService.findByUsername(username);
        return user != null ? user.getId() : null;
    }

    /**
     * Lấy User entity đang đăng nhập.
     * @param userService service để tra cứu user từ username
     * @return User entity hoặc null nếu chưa xác thực
     */
    public static User getAuthenticatedUser(UserService userService) {
        String username = getAuthenticatedUsername();
        if (username == null) return null;
        return userService.findByUsername(username);
    }
}
