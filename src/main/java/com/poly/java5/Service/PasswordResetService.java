package com.poly.java5.Service;

import com.poly.java5.DTO.MessageResponseDTO;
import com.poly.java5.Entity.User;
import com.poly.java5.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
@Service
public class PasswordResetService {
	@Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final SecureRandom random = new SecureRandom();

    // Tạo mã OTP 6 số
    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // 6 số: 100000 - 999999
        return String.valueOf(otp);
    }

    // BƯỚC 1: Gửi OTP qua email
    public MessageResponseDTO forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            // Vì lý do bảo mật, vẫn trả về success nhưng không gửi mail
            return new MessageResponseDTO("bạn sẽ nhận được mã OTP!", true);
        }
        
        User user = userOpt.get();
        
        // Tạo OTP 6 số
        String otp = generateOtp();
        
        // Lưu OTP vào database (hết hạn sau 5 phút)
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        
     //  Thêm log ngay sau save
        User savedUser = userRepository.save(user);
        System.out.println("Saved OTP: " + savedUser.getResetOtp() + " for " + savedUser.getEmail());
        
        // Gửi OTP qua email
        sendOtpEmail(email, otp);
        
        return new MessageResponseDTO("Mã OTP đã được gửi đến email của bạn!", true);
    }
    
    // Gửi email chứa OTP
    private void sendOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP đặt lại mật khẩu - Book Store");
        
        String emailContent = String.format(
            "Xin chào,\n\n" +
            "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\n" +
            "Mã OTP của bạn là: %s\n\n" +
            "Mã này có hiệu lực trong 5 phút.\n\n" +
            "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
            "Trân trọng,\n" +
            "Book Store Team",
            otp
        );
        
        message.setText(emailContent);
        mailSender.send(message);
    }
    
    // BƯỚC 2: Xác thực OTP và đặt lại mật khẩu
    public MessageResponseDTO verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return new MessageResponseDTO("Email không tồn tại!", false);
        }
        
        User user = userOpt.get();
        
        // Kiểm tra OTP có đúng không
        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            return new MessageResponseDTO("Mã OTP không chính xác!", false);
        }
        
        // Kiểm tra OTP còn hạn không
        if (user.getResetOtpExpiry() == null || 
            user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            return new MessageResponseDTO("Mã OTP đã hết hạn! Vui lòng yêu cầu lại.", false);
        }
        
        // Đặt lại mật khẩu
        user.setPassword(com.poly.java5.Utils.Utils.hashPassword(newPassword));
        
        // Xóa OTP sau khi dùng
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);
        
        return new MessageResponseDTO("Đặt lại mật khẩu thành công!", true);
    }
}
