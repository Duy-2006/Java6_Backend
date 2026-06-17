	package com.poly.java5.Config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.JWTService;
import com.poly.java5.Service.UserService;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Autowired
    private AuthFilter authFilter;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    private static final int TOKEN_EXPIRY_SECONDS = 86400;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. Mở cửa cho giao diện Swagger và Error
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                
                // 2. Mở cửa cho các API Auth công khai
                .requestMatchers("/api/auth/forgot-password", "/api/auth/verify-otp", "/api/auth/login", "/api/auth/register").permitAll()
                
                // 3. MỔ CỬA THÊM API AUDIO VÀO ĐÂY ĐỂ TEST BẰNG SWAGGER (đã thêm /api/admin/audio/**)
                .requestMatchers("/api/categories/**", "/api/books/**", "/uploads/**", 
                                 "/api/admin/authors/**", "/api/admin/books/**", 
                                 "/api/admin/orders/**", "/api/admin/customers/**", 
                                 "/api/admin/audio/**", "/api/banners/**").permitAll()
                
                // 4. Các API công khai khác
                .requestMatchers("/api/search", "/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/vouchers/admin/**", "/api/payment/vnpay-return", "/api/payment/ipn").permitAll()
                
                // 5. Sách nói: cho phép guest xem danh sách chương (Chapter 1 miễn phí)
                .requestMatchers("/api/user/books/**").permitAll()
                
                // 5. Các API bắt buộc đăng nhập (Token)
                .requestMatchers(HttpMethod.POST, "/api/books/*/reviews").authenticated()
                .requestMatchers("/api/orders/**").authenticated()
                
                // Chặn tất cả những đường dẫn còn lại nếu chưa cấu hình ở trên
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth.successHandler(this::oauth2SuccessHandler)
                                     .failureHandler(this::oauth2FailureHandler))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> {
                String path = request.getRequestURI();
                if (path.startsWith("/api/")) {
                    response.setContentType("application/json");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Unauthorized");
                    error.put("message", "Token invalid or expired");
                    response.getWriter().write(new ObjectMapper().writeValueAsString(error));
                } else {
                    response.sendRedirect("/login");
                }
            }))
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000", 
            "http://192.168.38.99:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void oauth2SuccessHandler(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");

        if (email == null) {
            response.sendRedirect("http://localhost:3000/user/callback?error=no_email");
            return;
        }

        User user = userService.findByEmail(email);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setUsername(googleId);
            user.setRole(UserRole.USER);
            user.setActive(true);
            user.setCreatedDate(LocalDateTime.now());
            user.setPassword("");
            userService.save(user);
        } else {
            if (user.getName() == null || !user.getName().equals(name)) {
                user.setName(name);
                userService.save(user);
            }
        }

        String jwtToken = jwtService.create(user, TOKEN_EXPIRY_SECONDS);
        request.getSession().invalidate();

        // ✅ Gắn JWT vào HTTP-Only Cookie (an toàn hơn truyền qua URL)
        response.setHeader("Set-Cookie",
            String.format("jwt=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax", jwtToken, TOKEN_EXPIRY_SECONDS));

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("fullName", user.getName());
        userData.put("role", user.getRole().toString());
        userData.put("username", user.getUsername());

        String encodedUserData = java.net.URLEncoder.encode(new ObjectMapper().writeValueAsString(userData), "UTF-8");
        // Vẫn truyền token qua URL để backwards compatible, cookie sẽ được ưu tiên dần
        String redirectUrl = String.format("http://localhost:3000/user/callback?token=%s&user=%s", jwtToken, encodedUserData);

        response.sendRedirect(redirectUrl);
    }

    private void oauth2FailureHandler(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        response.sendRedirect("http://localhost:3000/user/callback?error=google_auth_failed");
    }
}