package com.poly.java5.Entity;

/**
 * Enum phân quyền người dùng trong hệ thống BookStore.
 * Đồng bộ với Database SQL Server (cột 'role' trong bảng Users).
 */
public enum UserRole {
    ADMIN,   // Quản trị viên cao cấp (Full quyền)
    SELLER,  // Nhân viên bán hàng (Quyền quản lý đơn, sách, kho...)
    USER     // Khách hàng (Chỉ xem và mua hàng)
}