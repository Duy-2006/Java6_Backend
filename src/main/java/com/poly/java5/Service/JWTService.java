package com.poly.java5.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.poly.java5.Entity.User;

@Service
public class JWTService {
    // ✅ SỬA: Key phải đủ mạnh (256 bits = 32 bytes)
    // Có thể dùng key này hoặc tạo mới
    private final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ✅ THÊM userId vào token
    public String create(User user, int expirySeconds) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().toString());
        
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirySeconds * 1000L))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
                
        System.out.println("=== TOKEN CREATED ===");
        System.out.println("Username: " + user.getUsername());
        System.out.println("User ID: " + user.getId());
        System.out.println("Token: " + token);
        System.out.println("===================");
        
        return token;
    }

    public Claims getBody(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validate(String token) {
        try {
            System.out.println("=== Validating token ===");
            System.out.println("Token: " + token.substring(0, Math.min(50, token.length())) + "...");
            Claims claims = getBody(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            System.out.println("Token expiration: " + expiration);
            System.out.println("Current time: " + now);
            boolean isValid = expiration.after(now);
            System.out.println("Is valid: " + isValid);
            
            // In ra userId trong token
            Integer userId = claims.get("userId", Integer.class);
            System.out.println("User ID in token: " + userId);
            
            return isValid;
        } catch (Exception e) {
            System.out.println("Validation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}