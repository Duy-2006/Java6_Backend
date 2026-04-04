package com.poly.java5.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Service;
import com.poly.java5.Entity.User;

@Service
public class JWTService {
	 private final String SECRET_KEY = "mysecretkeymysecretkeymysecretkey12345";

	    private Key getSigningKey() {
	        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
	    }

	    public String create(User user, int expirySeconds) {
	        String token = Jwts.builder()
	                .setSubject(user.getUsername())
	                .setIssuedAt(new Date())
	                .setExpiration(new Date(System.currentTimeMillis() + expirySeconds * 1000L))
	                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
	                .compact();
	        System.out.println("Token created for user: " + user.getUsername());
	        System.out.println("Token: " + token);
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
	            System.out.println("Token: " + token);
	            Claims claims = getBody(token);
	            Date expiration = claims.getExpiration();
	            Date now = new Date();
	            System.out.println("Token expiration: " + expiration);
	            System.out.println("Current time: " + now);
	            boolean isValid = expiration.after(now);
	            System.out.println("Is valid: " + isValid);
	            return isValid;
	        } catch (Exception e) {
	            System.out.println("Validation error: " + e.getMessage());
	            e.printStackTrace();
	            return false;
	        }
	    }
}
