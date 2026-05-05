// File: OrderRepository.java
package com.poly.java5.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.poly.java5.Entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    // ✅ Lọc theo userId
    List<Order> findByUserIdOrderByOrderDateDesc(Integer userId);

    // ✅ Lọc theo userId + status
    List<Order> findByUserIdAndStatusOrderByOrderDateDesc(
        Integer userId,
        String status
    );
    
    List<Order> findAllByOrderByOrderDateDesc();

    // ✅ Lấy đơn theo ID và userId (THÊM METHOD NÀY)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.book b " +
           "WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);

    // ✅ Lấy đơn theo code + load chi tiết
    @Query(
        "SELECT DISTINCT o FROM Order o " +
        "LEFT JOIN FETCH o.orderDetails d " +
        "LEFT JOIN FETCH d.book " +
        "WHERE o.orderCode = :code AND o.user.id = :userId"
    )
    Optional<Order> findByOrderCodeFull(
        @Param("code") String code,
        @Param("userId") Integer userId
    );

    @Query("""
           SELECT COALESCE(SUM(o.totalAmount),0)
           FROM Order o
           WHERE o.status = 'COMPLETED'
           """)
    BigDecimal getTotalRevenue();

    @Query("""
           SELECT COALESCE(SUM(o.totalAmount),0)
           FROM Order o
           WHERE o.status = 'COMPLETED'
           AND o.orderDate >= :startOfDay
           AND o.orderDate < :endOfDay	
           """)
    BigDecimal getTodayRevenue(LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("""
           SELECT COUNT(o)
           FROM Order o
           WHERE o.orderDate >= :startOfDay
           AND o.orderDate < :endOfDay
           """)
    long countTodayOrders(LocalDateTime startOfDay, LocalDateTime endOfDay);

    long countByStatus(String status);
    
    

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'CANCELLED'")
    long countCancelledOrders();

    // Tổng chi tiêu
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.username = :username AND o.status = 'COMPLETED'")
    Double sumSpendingByUsername(@Param("username") String username);
     
    // Đơn hàng theo username
    List<Order> findByUserUsernameOrderByOrderDateDesc(String username);
}