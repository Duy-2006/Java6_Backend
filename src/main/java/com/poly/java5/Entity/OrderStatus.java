package com.poly.java5.Entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED, // Giao hàng thành công (Giới hạn tối đa của Admin)
    COMPLETED, // Hoàn thành (Do User bấm hoặc tự động sau 3 ngày)
    CANCELLED
}