package com.poly.java5.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.JWTService;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.Utils;

import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Configuration
@Component
public class AuthFilter extends OncePerRequestFilter {
	@Autowired
	private JWTService jwtService;

	@Autowired
	private UserService userService;

	
	@Override
	    protected void doFilterInternal(HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    FilterChain filterChain)
	            throws ServletException, IOException {
	        
	        String path = request.getRequestURI();
	        
	        System.out.println("=== AUTH FILTER CALLED ===");
	        System.out.println("Path: " + path);
	        
	        //  Bỏ qua hoàn toàn cho VNPay callback và IPN
	        if (path.startsWith("/api/payment/vnpay-return") || path.startsWith("/api/payment/ipn")) {
	            System.out.println("Skip VNPay callback: " + path);
	            filterChain.doFilter(request, response);
	            return;
	        }
	        
	        //  Bỏ qua OAuth2 endpoints
	        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
	            System.out.println("Skip OAuth2 endpoint: " + path);
	            filterChain.doFilter(request, response);
	            return;
	        }
	        
	        //  Bỏ qua login/register endpoints
	        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
	            System.out.println("Skip auth endpoint: " + path);
	            filterChain.doFilter(request, response);
	            return;
	        }
	     //  Bỏ qua forgot-password và verify-otp endpoints
	        if (path.startsWith("/api/auth/forgot-password") || 
	        	    path.startsWith("/api/auth/verify-otp")) {
	        	    
	        	    System.out.println("Skip password reset endpoint: " + path);
	        	    filterChain.doFilter(request, response);
	        	    return;
	        	}
	        
	        // Lấy token từ header
	        String authHeader = request.getHeader("Authorization");
	        System.out.println("Authorization header: " + authHeader);
	        
	        if (authHeader != null && authHeader.startsWith("Bearer ")) {
	            String token = authHeader.substring(7);
	            System.out.println("Token received: " + token);
	            
	            boolean isValid = jwtService.validate(token);
	            System.out.println("Token valid: " + isValid);
	            
	            if (isValid) {
	                try {
	                    io.jsonwebtoken.Claims claims = jwtService.getBody(token);
	                    String username = claims.getSubject();
	                    System.out.println("Username from token: " + username);
	                    
	                    if (username != null) {
	                        UsernamePasswordAuthenticationToken authentication = 
	                            new UsernamePasswordAuthenticationToken(username, null, new java.util.ArrayList<>());
	                        SecurityContextHolder.getContext().setAuthentication(authentication);
	                        System.out.println(" Authentication set for: " + username);
	                    }
	                } catch (Exception e) {
	                    System.out.println("Error: " + e.getMessage());
	                    e.printStackTrace();
	                }
	            }
	        } else {
	            System.out.println("No Bearer token found");
	            // Nếu không có token và request cần xác thực -> trả về 401
	            if (!path.startsWith("/api/auth") && 
	            	!path.startsWith("/api/categories") &&
	                !path.startsWith("/api/books") && 
	                !path.startsWith("/api/search") &&
	                !path.startsWith("/uploads") &&
	                !path.startsWith("/api/checkout") && 
	                !path.startsWith("/api/payment")&&
	                !path.startsWith("/api/admin/authors")&& 
	                !path.startsWith("/api/admin/customers")&& 
	                !path.startsWith("/api/admin/books")&& 
	                !path.startsWith("/api/admin/orders")&& 
	            	!path.startsWith("/api/orders")){   
	                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	                response.getWriter().write("{\"message\":\"Unauthorized\"}");
	                response.setContentType("application/json");
	                return;
	            }
	            System.out.println("Request path: " + path);
	            
	            System.out.println("=== AuthFilter path: '" + path + "'");
	            System.out.println("  startsWith /api/categories: " + path.startsWith("/api/categories"));
	        }
	        
	        filterChain.doFilter(request, response);
	    }
}