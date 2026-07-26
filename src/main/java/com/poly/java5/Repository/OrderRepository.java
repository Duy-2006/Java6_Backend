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
	@Query("""
		    SELECT o
		    FROM Order o
		    WHERE o.user.id = :userId
		    ORDER BY
		        CASE
		            WHEN o.status = 'PENDING' THEN 0
		            ELSE 1
		        END,
		        CASE
		            WHEN o.status = 'PENDING' THEN o.orderDate
		        END ASC,
		        o.orderDate DESC
		""")
		List<Order> findByUserIdPriority(
		        @Param("userId") Integer userId
		);

    // ✅ Lọc theo userId + status
	@Query("""
		    SELECT o
		    FROM Order o
		    WHERE o.user.id = :userId
		      AND o.status = :status
		    ORDER BY
		        o.orderDate ASC
		""")
		List<Order> findByUserIdAndStatusPriority(
		        @Param("userId") Integer userId,
		        @Param("status") String status
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
    	
    	
    	@Query("""
    			SELECT DISTINCT o
    			FROM Order o
    			JOIN FETCH o.orderDetails od
    			JOIN FETCH od.book b
    			WHERE o.user.id=:userId
    			AND (:status IS NULL OR o.status=:status)
    			AND b.bookType=:bookType
    			ORDER BY o.orderDate DESC
    			""")
    			List<Order> findOrdersByType(
    			        Integer userId,
    			        String status,
    			        String bookType);

}