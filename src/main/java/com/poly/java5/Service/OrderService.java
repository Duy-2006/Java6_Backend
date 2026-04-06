package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Cart;
import com.poly.java5.Entity.CartDetail;
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
	private final PromotionService promotionService;

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
	
	    // =========================
    // TẠO ĐƠN HÀNG TỪ GIỎ HÀNG (CHECKOUT)
    // =========================
        // ====================== TẠO ĐƠN HÀNG TỪ GIỎ HÀNG + ÁP DỤNG KHUYẾN MÃI ======================
    @Transactional
    public Order createOrderFromCart(Integer userId, String paymentMethod, 
                                     String customerName, String customerPhone, 
                                     String customerAddress) {

        // Lấy giỏ hàng
        Cart cart = em.createQuery("""
                SELECT c FROM Cart c 
                LEFT JOIN FETCH c.cartDetails cd 
                LEFT JOIN FETCH cd.book b
                LEFT JOIN FETCH b.category
                WHERE c.user.id = :userId AND c.status = 'ACTIVE'
                """, Cart.class)
                .setParameter("userId", userId)
                .getSingleResult();

        if (cart == null || cart.getCartDetails().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setCustomerName(customerName != null ? customerName : cart.getUser().getFullName());
        order.setCustomerPhone(customerPhone != null ? customerPhone : cart.getUser().getPhone());
        order.setCustomerAddress(customerAddress);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setOrderCode(generateOrderCode());
        order.setOrderDate(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Xử lý từng item trong giỏ hàng
        for (CartDetail cd : cart.getCartDetails()) {
            if (Boolean.TRUE.equals(cd.getSelected())) {

                Book book = cd.getBook();
                BigDecimal originalPrice = cd.getPrice();

                // === ÁP DỤNG KHUYẾN MÃI TỐT NHẤT ===
                BigDecimal finalPrice = promotionService.calculateDiscountedPrice(book.getId(), originalPrice);

                OrderDetail od = new OrderDetail();
                od.setOrder(order);
                od.setBook(book);
                od.setQuantity(cd.getQuantity());
                od.setPrice(finalPrice);                    // Lưu giá sau khuyến mãi

                order.getOrderDetails().add(od);

                totalAmount = totalAmount.add(finalPrice.multiply(BigDecimal.valueOf(cd.getQuantity())));

                // Giảm tồn kho
                if (book.getQuantity() < cd.getQuantity()) {
                    throw new RuntimeException("Sách " + book.getTitle() + " không đủ số lượng!");
                }
                book.setQuantity(book.getQuantity() - cd.getQuantity());
                em.merge(book);
            }
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Không có sản phẩm hợp lệ để tạo đơn hàng!");
        }

        order.setTotalAmount(totalAmount);
        em.persist(order);

        // Đánh dấu giỏ hàng đã dùng
        cart.setStatus("COMPLETED");
        em.merge(cart);

        return order;
    }

    // Tạo mã đơn hàng ngẫu nhiên
    private String generateOrderCode() {
        return "ORD" + System.currentTimeMillis() % 10000000;
    }
}
