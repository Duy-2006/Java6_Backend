package com.poly.java5.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
	private final JavaMailSender mailSender;

    public void sendSimpleEmail(String toEmail, String username, boolean isActive) {
        String subject = isActive ? "Tài khoản đã được mở khóa" : "Tài khoản đã bị khóa";
        String body = String.format(
            "Xin chào %s,\n\nTài khoản của bạn trên hệ thống BookStore đã được %s.\n\n%s",
            username,
            isActive ? "mở khóa" : "khóa",
            isActive ? "Bạn có thể đăng nhập lại bình thường." : "Vui lòng liên hệ hỗ trợ để biết thêm chi tiết."
        );
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

	
}
