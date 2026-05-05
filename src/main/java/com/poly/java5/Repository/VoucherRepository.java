package com.poly.java5.Repository;
import com.poly.java5.Entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
	 // Tìm theo mã (dùng khi user nhập mã)
    Optional<Voucher> findByCode(String code);

    // Kiểm tra mã đã tồn tại chưa
    boolean existsByCode(String code);

    // Lấy tất cả, mới nhất trước
    List<Voucher> findAllByOrderByIdDesc();

    // Lấy các voucher đang hoạt động và còn hạn (dùng khi user áp dụng)
    @Query("""
        SELECT v FROM Voucher v
        WHERE v.active = true
          AND v.startDate <= :today
          AND v.endDate   >= :today
          AND v.usedCount  < v.usageLimit
        """)
    List<Voucher> findActiveVouchers(@Param("today") LocalDate today);

    // Tìm voucher hợp lệ theo mã + tổng đơn hàng
    @Query("""
        SELECT v FROM Voucher v
        WHERE v.code      = :code
          AND v.active    = true
          AND v.startDate <= :today
          AND v.endDate   >= :today
          AND v.usedCount  < v.usageLimit
          AND v.minOrderValue <= :orderAmount
        """)
    Optional<Voucher> findValidVoucher(
        @Param("code")        String    code,
        @Param("today")       LocalDate today,
        @Param("orderAmount") Double    orderAmount
    );
}
