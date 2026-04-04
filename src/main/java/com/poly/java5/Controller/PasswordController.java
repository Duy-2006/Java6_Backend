package com.poly.java5.Controller;


import com.poly.java5.DTO.ForgotPasswordRequestDTO;
import com.poly.java5.DTO.MessageResponseDTO;
import com.poly.java5.DTO.ResetPasswordRequestDTO;
import com.poly.java5.Service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class PasswordController {
	 @Autowired
	    private PasswordResetService passwordResetService;

	    // Gửi OTP qua email
	    @PostMapping("/forgot-password")
	    public MessageResponseDTO forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
	        return passwordResetService.forgotPassword(request.getEmail());
	    }

	    // Xác thực OTP và đặt lại mật khẩu
	    @PostMapping("/verify-otp")
	    public MessageResponseDTO verifyOtp(@RequestBody ResetPasswordRequestDTO request) {
	        return passwordResetService.verifyOtpAndResetPassword(
	            request.getEmail(), 
	            request.getOtp(), 
	            request.getNewPassword()
	        );
	    }
}
