package com.poly.java5.Repository;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserVoucher;
import com.poly.java5.Entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Integer> {

    // Tìm tất cả voucher của một user (kèm thông tin voucher)
    List<UserVoucher> findByUser(User user);

    // Tìm theo user và voucher (để kiểm tra đã nhận chưa)
    Optional<UserVoucher> findByUserAndVoucher(User user, Voucher voucher);

    // Kiểm tra user đã dùng voucher này chưa (isUsed = true)
    boolean existsByUserAndVoucherAndIsUsedTrue(User user, Voucher voucher);

    // Đếm số lần user đã dùng một voucher (nếu cho phép dùng nhiều lần? Mặc định mỗi voucher 1 lần/user)
    long countByUserAndVoucherAndIsUsedTrue(User user, Voucher voucher);

    // Lấy các voucher user đã nhận nhưng chưa dùng
    @Query("SELECT uv FROM UserVoucher uv WHERE uv.user = :user AND uv.isUsed = false")
    List<UserVoucher> findUnusedByUser(@Param("user") User user);

    // Đánh dấu đã sử dụng
    @Modifying
    @Transactional
    @Query("UPDATE UserVoucher uv SET uv.isUsed = true, uv.usedDate = CURRENT_TIMESTAMP WHERE uv.user = :user AND uv.voucher = :voucher")
    int markAsUsed(@Param("user") User user, @Param("voucher") Voucher voucher);
}