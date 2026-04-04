// File: OrderDetailRepository.java
package com.poly.java5.Repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.poly.java5.Entity.OrderDetail;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
	
	 @Query("""
	           SELECT b.id, b.title, SUM(od.quantity)
	           FROM OrderDetail od
	           JOIN od.order o
	           JOIN od.book b
	           WHERE o.status = 'COMPLETED'
	           GROUP BY b.id, b.title
	           ORDER BY SUM(od.quantity) DESC
	           """)
	    List<Object[]> findBestSellerBooks();

	


}