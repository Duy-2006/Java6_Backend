package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.DTO.OrderDTO;
import com.poly.java5.DTO.OrderDetailDTO;
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

	// Lấy danh sách đơn hàng của user Có thể lọc theo trạng thái
	@Transactional(readOnly = true)
	public List<Order> findOrdersByUser(Integer userId, String status) {

		if (status == null || status.isBlank()) {
			return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
		}

		return orderRepository.findByUserIdAndStatusOrderByOrderDateDesc(userId, status);
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
	@Transactional
public void updateStatus(Integer id, String newStatus) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + id));

    String current = order.getStatus();
    String target = newStatus.toUpperCase().trim();

    // Validate enum
    try {
        OrderStatus.valueOf(target);
    } catch (IllegalArgumentException e) {
        throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
    }

    // Định nghĩa luồng hợp lệ
	List<String> forwardFlow = Arrays.asList("PENDING", "CONFIRMED", "SHIPPING", "COMPLETED");
    boolean isForwardStep = forwardFlow.indexOf(current) + 1 == forwardFlow.indexOf(target);
    boolean isCancel = target.equals("CANCELLED") && (current.equals("PENDING") || current.equals("CONFIRMED"));

    if (!isForwardStep && !isCancel) {
        throw new RuntimeException(
            String.format("Không thể chuyển từ %s sang %s. Chỉ được: %s → %s, hoặc hủy từ PENDING/CONFIRMED.",
                current, target,
                current, getNextStatus(current))
        );
    }

    order.setStatus(target);
    orderRepository.save(order);
}

// Helper để lấy trạng thái tiếp theo (dùng trong thông báo lỗi)
private String getNextStatus(String current) {
    switch (current) {
        case "PENDING": return "CONFIRMED";
        case "CONFIRMED": return "SHIPPING";
        case "SHIPPING": return "COMPLETED";
        default: return "";
    }
}

	// 🔥 SỬA: trả về List<OrderDTO> thay vì List<Order>
	public List<OrderDTO> findByUsername(String username) {
		List<Order> orders = orderRepository.findByUserUsernameOrderByOrderDateDesc(username);
		return orders.stream().map(this::convertToOrderDTO).collect(Collectors.toList());
	}

	// Chuyển đổi Order → OrderDTO
	private OrderDTO convertToOrderDTO(Order order) {
		List<OrderDetailDTO> detailDTOs = null;
		if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
			detailDTOs = order.getOrderDetails().stream().map(this::convertToOrderDetailDTO)
					.collect(Collectors.toList());
		}

		OrderDTO dto = new OrderDTO();
		dto.setId(order.getId());
		dto.setOrderCode(order.getOrderCode());
		dto.setCustomerName(order.getCustomerName());
		dto.setCustomerPhone(order.getCustomerPhone());
		dto.setCustomerAddress(order.getCustomerAddress()); // lấy từ entity
		dto.setTotalAmount(order.getTotalAmount());
		dto.setStatus(order.getStatus());
		dto.setOrderDate(order.getOrderDate());
		dto.setPaymentMethod(order.getPaymentMethod()); // lấy từ entity
		dto.setPaymentStatus(order.getPaymentStatus()); // lấy từ entity
		dto.setOrderDetails(detailDTOs);
		return dto;
	}

	// Chuyển đổi OrderDetail → OrderDetailDTO (dùng book title, imageUrl)
	private OrderDetailDTO convertToOrderDetailDTO(OrderDetail detail) {
		Book book = detail.getBook();
		String bookImageUrl = (book != null) ? book.getImageUrl() : null;
		String bookTitle = (book != null) ? book.getTitle() : null;
		return new OrderDetailDTO(detail.getId(), (book != null) ? book.getId() : null, bookTitle, detail.getQuantity(),
				detail.getPrice(), bookImageUrl);
	}

	// Tổng chi tiêu của 1 khách hàng (dùng cho trang customers)
	public Double sumSpendingByUsername(String username) {
		return orderRepository.sumSpendingByUsername(username);
	}

	// Tìm order theo ID và UserId
	@Transactional(readOnly = true)
	public Order findByIdAndUser(Integer id, Integer userId) {
		return orderRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + id + " cho user: " + userId));
	}

}
