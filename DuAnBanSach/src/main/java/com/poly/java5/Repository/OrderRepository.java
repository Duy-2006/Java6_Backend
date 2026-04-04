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
    	
    	


}