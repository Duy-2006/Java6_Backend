package com.poly.java5.Config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
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
import jakarta.servlet.http.HttpSession;

@Configuration
public class SecurityConfig {
	@Autowired
	private AuthFilter authFilter;

	@Autowired
	private JWTService jwtService;

	@Autowired
	private UserService userService;

	private static final int TOKEN_EXPIRY_SECONDS = 86400; // 24 hours
	
//	@Bean
//	public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
//		return new NullAuthenticatedSessionStrategy(); // Không tạo session
//	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// ✅ PUBLIC ENDPOINTS - ĐẶT CỤ THỂ TRƯỚC
				.requestMatchers(
					"/api/auth/forgot-password",
					"/api/auth/verify-otp",
					"/api/auth/login",
					"/api/auth/register"
				).permitAll()
				// ✅ Các API public khác
				.requestMatchers("/api/categories/**", "/api/books/**", "/uploads/**")
				.permitAll()
				// ✅ OAuth2 endpoints
				.requestMatchers("/oauth2/**", "/login/oauth2/**")
				.permitAll()
				// ❌ Các request khác cần xác thực
				 .requestMatchers("/api/payment/**")
				 .permitAll()  // THÊM DÒNG NÀY
				 
				.anyRequest()
				.authenticated()
			)
			.oauth2Login(oauth -> oauth
				.successHandler(this::oauth2SuccessHandler)
				.failureHandler(this::oauth2FailureHandler)
			)
			.logout(logout -> logout.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler((request, response, authentication) -> {
					response.setStatus(200);
					response.setContentType("application/json");
					response.getWriter().write("{\"message\": \"Logout successful\"}");
				})
			)
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
		config.setAllowedOriginPatterns(List.of("http://localhost:3000"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		config.setExposedHeaders(List.of("Authorization"));
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	// OAuth2 Success Handler
	private void oauth2SuccessHandler(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
		OAuth2User oauth2User = oauthToken.getPrincipal();

		String email = oauth2User.getAttribute("email");
		String name = oauth2User.getAttribute("name");
		String googleId = oauth2User.getAttribute("sub");

		System.out.println("=== Google Login Success ===");
		System.out.println("Email: " + email);
		System.out.println("Name: " + name);
		System.out.println("Google ID: " + googleId);

		if (email == null) {
			response.sendRedirect("http://localhost:3000/user/callback?error=no_email");
			return;
		}

		User user = userService.findByEmail(email);

		if (user == null) {
			System.out.println("Creating new user from Google...");
			user = new User();
			user.setEmail(email);
			user.setName(name);
			user.setUsername(googleId);
			user.setRole(UserRole.USER);
			user.setActive(true);
			user.setCreatedDate(LocalDateTime.now());
			user.setPassword("");
			userService.save(user);
			System.out.println(" Created new user with ID: " + user.getId());
		} else {
			System.out.println(" Existing user found with ID: " + user.getId());
			if (user.getName() == null || !user.getName().equals(name)) {
				user.setName(name);
				userService.save(user);
				System.out.println(" Updated user name");
			}
		}

		String jwtToken = jwtService.create(user, TOKEN_EXPIRY_SECONDS);
		System.out.println("Generated JWT token: " + jwtToken);

		// ✅ Xóa session để tránh redirect lần sau
		request.getSession().invalidate();

		Map<String, Object> userData = new HashMap<>();
		userData.put("email", user.getEmail());
		userData.put("fullName", user.getName());
		userData.put("role", user.getRole().toString());
		userData.put("username", user.getUsername());

		String encodedUserData = java.net.URLEncoder.encode(new ObjectMapper().writeValueAsString(userData), "UTF-8");

		String redirectUrl = String.format("http://localhost:3000/user/callback?token=%s&user=%s", jwtToken,
				encodedUserData);

		System.out.println("Redirecting to: " + redirectUrl);
		response.sendRedirect(redirectUrl);
	}

	private void oauth2FailureHandler(HttpServletRequest request, HttpServletResponse response, Exception exception)
			throws IOException {
		System.err.println("Google login failed: " + exception.getMessage());
		response.sendRedirect("http://localhost:3000/user/callback?error=google_auth_failed");
	}

//	@Bean
//	public CorsConfigurationSource corsConfigurationSource() {
//	    CorsConfiguration config = new CorsConfiguration();
//	    // ❌ SAI: config.setAllowedOrigins(List.of("*"));
//	    // ✅ ĐÚNG: Dùng allowedOriginPatterns hoặc list cụ thể
//	    config.setAllowedOriginPatterns(List.of("http://localhost:3000"));  // Dùng Pattern
//	    // Hoặc:
//	    // config.setAllowedOrigins(List.of("http://localhost:3000"));
//	    
//	    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//	    config.setAllowedHeaders(List.of("*"));
//	    config.setAllowCredentials(true);
//	    config.setExposedHeaders(List.of("Authorization"));
//	    
//	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//	    source.registerCorsConfiguration("/**", config);
//	    return source;
//	}

}
