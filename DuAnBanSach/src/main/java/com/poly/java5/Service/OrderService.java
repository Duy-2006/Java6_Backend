package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.OrderDetail;
import com.poly.java5.Entity.User;
import com.poly.java5.Repository.OrderRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

	private final OrderRepository orderRepository; // ✅ BẮT BUỘC

	@PersistenceContext
	private EntityManager em;

	public void cancelOrder(Integer orderId, Integer userId) {

		Order order = em.find(Order.class, orderId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

		if (order == null) {
			throw new RuntimeException("Không tìm thấy đơn hàng");
		}

		if (!order.getUser().getId().equals(userId)) {
			throw new RuntimeException("Không có quyền hủy đơn");
		}

		if (!order.isCancellable() || "PAID".equals(order.getPaymentStatus())) {
			throw new RuntimeException("Đơn hàng không thể hủy");
		}

		for (OrderDetail od : order.getOrderDetails()) {
			Book book = em.find(Book.class, od.getBook().getId(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
			book.setQuantity(book.getQuantity() + od.getQuantity());
		}

		order.setStatus("CANCELLED");

		em.merge(order);
	}

	@Transactional(readOnly = true)
	public Order findByCodeAndUser(String code, Integer userId) {

		return em
				.createQuery(
						"SELECT DISTINCT o FROM Order o " + "LEFT JOIN FETCH o.orderDetails od "
								+ "LEFT JOIN FETCH od.book " + "WHERE o.orderCode = :code AND o.user.id = :uid",
						Order.class)
				.setParameter("code", code).setParameter("uid", userId).getResultStream().findFirst()
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc không có quyền truy cập"));
	}

	@Transactional(readOnly = true)
	public List<Order> findOrdersByUser(Integer userId, String status) {

	    if (status == null || status.isBlank()) {
	        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
	    }

	    return orderRepository
	            .findByUserIdAndStatusOrderByOrderDateDesc(userId, status);
	}
	
	public List<Order> findAll() {
	    return orderRepository.findAll();
	}
	
}
