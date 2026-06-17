package com.poly.java5.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    // Mã đơn hàng
    private Long orderCode;

    // Link thanh toán → FE redirect sang đây
    private String checkoutUrl;

    // QR code để hiển thị trực tiếp trên web
    private String qrCode;

    // Trạng thái: PENDING / PAID / CANCELLED
    private String status;

    // Thông báo kết quả
    private String message;

    // ── Static factory methods ──────────────────────────

    public static PaymentResponseDTO success(Long orderCode, String checkoutUrl, String qrCode) {
        return PaymentResponseDTO.builder()
                .orderCode(orderCode)
                .checkoutUrl(checkoutUrl)
                .qrCode(qrCode)
                .status("PENDING")
                .message("Tạo link thanh toán thành công")
                .build();
    }

    public static PaymentResponseDTO error(String message) {
        return PaymentResponseDTO.builder()
                .status("ERROR")
                .message(message)
                .build();
    }
}