package com.poly.java5.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.poly.java5.Bean.LoginBean;
import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.Utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/login")
public class LoginController {
	@Autowired
    private UserService userService;

    // ===== LOGIN UI =====
    @GetMapping("")
    public String loginUI(Model model) {
        model.addAttribute("bean", new LoginBean());
        return "login";
    }

    // ===== LOGIN POST =====
    @PostMapping("")
    public String login(
            @Valid @ModelAttribute("bean") LoginBean loginBean,
            Errors errors,
            Model model,
            HttpServletRequest request) {

        if (errors.hasErrors()) {
            return "login";
        }

        // 🔐 CHECK LOGIN
        User user = userService.login(
                loginBean.getUsernameOrEmail(),
                loginBean.getPassword()
        );

        if (user == null) {
            errors.rejectValue(
                    "usernameOrEmail",
                    "login.failed",
                    "Tên đăng nhập/email hoặc mật khẩu không đúng"
            );
            return "login";
        }

        if (!user.getActive()) {
            errors.reject("login.locked", "Tài khoản đã bị khóa");
            return "login";
        }
        

        // ✅ LƯU SESSION (DUY NHẤT 1 KIỂU)
        Utils.setUserSession(user.getId(), request);

        // ===== LOG CHECK =====
        System.out.println(">>> LOGIN SUCCESS | USER_ID = " + user.getId());
        System.out.println(">>> ROLE = " + user.getRole());

        // ===== PHÂN QUYỀN =====
        if (user.isAdmin()) {
            return "redirect:/admin";
        }

        return "redirect:/home";
    }
    
    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {

        System.out.println(">>> LOGOUT USER");

        request.getSession().invalidate();

        return "redirect:/home";
    }
}

   
