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

    public void sendOrderCancelledEmail(String toEmail, String customerName, String orderCode, String cancelReason, String paymentStatus, String paymentMethod, java.math.BigDecimal totalAmount) {
        String subject = "Thông báo hủy đơn hàng " + orderCode;
        
        StringBuilder body = new StringBuilder();
        
        boolean isPaidOnlineOrder = "PAID".equalsIgnoreCase(paymentStatus) && ("VNPAY".equalsIgnoreCase(paymentMethod) || "PAYOS".equalsIgnoreCase(paymentMethod));

        if (isPaidOnlineOrder) {
            body.append(String.format("Xin chào %s,\n\n", customerName));
            body.append(String.format("Đơn hàng %s của bạn đã được cửa hàng hủy.\n\n", orderCode));
            body.append(String.format("Lý do hủy: %s\n\n", cancelReason != null ? cancelReason : "Không có"));
            body.append(String.format("Đơn hàng này đã được thanh toán online bằng %s.\n\n", paymentMethod));
            body.append("Cửa hàng sẽ liên hệ với bạn qua số điện thoại hoặc email đã đăng ký để xác nhận thông tin và thực hiện hoàn tiền.\n\n");
            
            String formattedAmount = new java.text.DecimalFormat("#,###").format(totalAmount != null ? totalAmount : java.math.BigDecimal.ZERO) + " đ";
            body.append(String.format("Số tiền đã thanh toán: %s\n\n", formattedAmount));
            body.append("Vui lòng không cung cấp mật khẩu, mã OTP hoặc mã PIN ngân hàng cho bất kỳ ai.\n\n");
            body.append("Nếu cần hỗ trợ về việc hoàn tiền, quý khách vui lòng liên hệ:\n");
            body.append("Hotline: 0901 234 567\n");
            body.append("Email: support@bookstore.vn\n");
            body.append("Thời gian hỗ trợ: 08:00–17:00, từ thứ Hai đến thứ Bảy\n");
            body.append("Địa chỉ: 123 Đường Sách, Quận 1, TP.HCM\n\n");
            body.append("Trân trọng,\nBookStore Team");
        } else {
            body.append("Xin chào,\n\n");
            body.append("Đơn hàng của bạn (Mã: ").append(orderCode).append(") đã bị hủy.\n\n");
            body.append("- Lý do hủy: ").append(cancelReason != null ? cancelReason : "Không có").append("\n");
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            body.append("- Thời gian hủy: ").append(java.time.LocalDateTime.now().format(formatter)).append("\n\n");
            
            body.append("Thông tin liên hệ cửa hàng:\n");
            body.append("- Hotline: 1900 1234\n");
            body.append("- Email: support@bookstore.com\n");
            body.append("- Địa chỉ: 123 Đường Sách, Quận 1, TP.HCM\n\n");
            body.append("Trân trọng,\nBookStore Team");
        }
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body.toString());
        mailSender.send(message);
    }
}
