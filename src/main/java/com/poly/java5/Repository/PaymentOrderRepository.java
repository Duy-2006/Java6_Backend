package com.poly.java5.Repository;

import com.poly.java5.Entity.PaymentOrder;
import com.poly.java5.Entity.PaymentOrder.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    // Tìm theo orderCode (PayOS trả về trong webhook)
    Optional<PaymentOrder> findByOrderCode(Long orderCode);

    // Lấy danh sách theo trạng thái
    List<PaymentOrder> findByStatus(PaymentStatus status);

    // Cập nhật trạng thái khi webhook về
    @Modifying
    @Transactional
    @Query("UPDATE PaymentOrder p SET p.status = :status, p.paidAt = CURRENT_TIMESTAMP " +
           "WHERE p.orderCode = :orderCode")
    int updateStatusByOrderCode(
        @Param("orderCode") Long orderCode,
        @Param("status") PaymentStatus status
    );

    // Kiểm tra đơn hàng đã tồn tại chưa
    boolean existsByOrderCode(Long orderCode);
}