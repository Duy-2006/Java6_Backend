package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.OrderDetail;
import com.poly.java5.Entity.OrderStatus;
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
	// Cho phép user hủy đơn hàng
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
		// hoàng kho
		for (OrderDetail od : order.getOrderDetails()) {
			Book book = em.find(Book.class, od.getBook().getId(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
			// cộng lại sản phẩm vừa hủy vào kho
			book.setQuantity(book.getQuantity() + od.getQuantity());
		}

		order.setStatus("CANCELLED");
		// up lên data
		em.merge(order);
	}
	// Tìm chi tiết đơn hàng theo mã đơn Nhưng chỉ cho user sở hữu đơn hàng đó
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
	//Lấy danh sách đơn hàng của user Có thể lọc theo trạng thái
	@Transactional(readOnly = true)
	public List<Order> findOrdersByUser(Integer userId, String status) {

	    if (status == null || status.isBlank()) {
	        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
	    }

	    return orderRepository
	            .findByUserIdAndStatusOrderByOrderDateDesc(userId, status);
	}
	// cho admin xem toàn bộ đơn hàng 
	public List<Order> findAll() {
	    return orderRepository.findAll();
	}
	
	// Tìm đơn hàng theo ID
		public Order findById(Integer id) {        
		    return orderRepository.findById(id)
		            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + id));
		}
	 
	    
	 // Cập nhật trạng thái đơn hàng (dành cho Admin)
	    public void updateStatus(Integer id, String newStatus) {
	        Order order = orderRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + id));

	        // Kiểm tra trạng thái hợp lệ
	        try {
	            OrderStatus.valueOf(newStatus.toUpperCase().trim());  // chỉ để validate
	        } catch (IllegalArgumentException e) {
	            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus 
	                + ". Các trạng thái cho phép: PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED");
	        }

	        order.setStatus(newStatus.toUpperCase().trim());
	        orderRepository.save(order);
	    }
	 
	 // Tổng chi tiêu của 1 khách hàng (dùng cho trang customers)
	 // ✅ Note: cần thêm query vào OrderRepository (xem bên dưới)
	 public Double sumSpendingByUsername(String username) {
	     return orderRepository.sumSpendingByUsername(username);
	 }
	  
	 // Đơn hàng của 1 user — dùng cho lịch sử mua hàng admin
	 public java.util.List<Order> findByUsername(String username) {
	     return orderRepository.findByUserUsernameOrderByOrderDateDesc(username);
	 }
}
