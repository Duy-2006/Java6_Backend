package com.poly.java5.Config;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;


import com.poly.java5.Entity.User;
public class JwtUtil {
	private static final SecretKey KEY = 
		    Keys.hmacShaKeyFor("mysecretkeymysecretkey1234567890123456".getBytes());

	    private static final long EXPIRATION = 86400000;
	    // tạo token 
	    public static String generateToken(User user) {
	        return Jwts.builder()
	                .setSubject(user.getId().toString())
	                .claim("role", user.getRole().name())
	                .setIssuedAt(new Date())
	                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
	                .signWith(KEY)
	                .compact();
	    }
	    // giải mã token
	    public static Claims parseToken(String token) {
	        try {
	            return Jwts.parserBuilder()
	                    .setSigningKey(KEY)
	                    .build()
	                    .parseClaimsJws(token)
	                    .getBody();
	        } catch (JwtException e) {
	            return null;
	        }
	    }

	    public static Integer getUserId(String token) {
	        Claims claims = parseToken(token);
	        return claims != null ? Integer.parseInt(claims.getSubject()) : null;
	    }

	    public static String getRole(String token) {
	        Claims claims = parseToken(token);
	        return claims != null ? claims.get("role", String.class) : null;
	    }

	    public static boolean isExpired(String token) {
	        Claims claims = parseToken(token);
	        return claims == null || claims.getExpiration().before(new Date());
	    }
}
