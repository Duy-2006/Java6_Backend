package com.poly.java5.Config;

import java.io.IOException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.Utils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
@Configuration
public class AuthFilter implements Filter {
	 @Autowired
	    private UserService userService;

	    @Override
	    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	            throws IOException, ServletException {

	        HttpServletRequest req = (HttpServletRequest) request;
	        HttpServletResponse resp = (HttpServletResponse) response;

	        String uri = req.getRequestURI();

	        // Chỉ kiểm tra admin
	        if (uri.startsWith("/admin")) {

	            HttpSession session = req.getSession(false);

	            if (session == null) {
	                resp.sendRedirect(req.getContextPath() + "/login");
	                return;
	            }

	            Integer userId = (Integer) session.getAttribute(Utils.SESSION_USER_ID);

	            if (userId == null) {
	                resp.sendRedirect(req.getContextPath() + "/login");
	                return;
	            }

	            User user = userService.findById(userId);

	            if (user == null || !user.getActive()) {
	                session.invalidate();
	                resp.sendRedirect(req.getContextPath() + "/login");
	                return;
	            }

	            if (user.getRole() != UserRole.ADMIN) {
	                resp.sendRedirect(req.getContextPath() + "/home");
	                return;
	            }
	        }

	        chain.doFilter(request, response);
	    }


	 


}
