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
    
    @Query("SELECT COALESCE(SUM(o.totalAmount + COALESCE(o.shippingFee, 0)), 0) FROM Order o WHERE o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end")
    BigDecimal getRevenueBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndOrderDateBetween(String status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT FUNCTION('YEAR', o.orderDate) as year, FUNCTION('MONTH', o.orderDate) as month, " +
           "SUM(o.totalAmount + COALESCE(o.shippingFee, 0)) as revenue, COUNT(o) as orders " +
           "FROM Order o WHERE o.status = 'COMPLETED' AND o.orderDate >= :startDate " +
           "GROUP BY FUNCTION('YEAR', o.orderDate), FUNCTION('MONTH', o.orderDate) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyRevenueStats(LocalDateTime startDate);

    @Query("SELECT c.name, SUM(od.price * od.quantity) " +
           "FROM OrderDetail od JOIN od.book b JOIN b.category c JOIN od.order o " +
           "WHERE o.status = 'COMPLETED' " +
           "GROUP BY c.name")
    List<Object[]> getRevenueByCategory();

    List<Order> findTop5ByOrderByOrderDateDesc();

    @Query("SELECT c.name, SUM(od.price * od.quantity) " +
           "FROM OrderDetail od JOIN od.book b JOIN b.category c JOIN od.order o " +
           "WHERE o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end " +
           "GROUP BY c.name")
    List<Object[]> getRevenueByCategoryBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :start AND :end ORDER BY o.orderDate DESC")
    List<Order> findOrdersBetweenOrderByOrderDateDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
           "FROM Order o JOIN o.orderDetails od " +
           "WHERE o.user.id = :userId AND o.status = 'COMPLETED' AND od.book.id = :bookId")
    boolean hasPurchasedBook(@Param("userId") Integer userId, @Param("bookId") Integer bookId);
}