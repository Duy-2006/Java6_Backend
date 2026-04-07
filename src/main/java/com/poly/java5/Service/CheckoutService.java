package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Cart;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.OrderDetail;
import com.poly.java5.Entity.CartDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CheckoutService {
	
	
	
	@PersistenceContext
	private EntityManager em;
	
	// ================= 1. TẠO MÃ ĐƠN HÀNG =================
	private String generateOrderCode() {
	    // Format: ORD + ddMMHHmm (8 số) + 2 số random = 13 ký tự
	    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMHHmm"));
	    int random = (int) (Math.random() * 90) + 10; // 2 số ngẫu nhiên
	    return "ORD" + timestamp + random;
	}
	
	// ================= 2. LẤY THÔNG TIN GIỎ HÀNG ĐÃ CHỌN =================
	public List<Map<String, Object>> getSelectedCartItems(Integer userId) {
		String sql = """
			SELECT 
				b.id as bookId,
				b.title as title,
				b.image_url as imageUrl,
				cd.quantity as quantity,
				cd.price * cd.quantity as itemTotal
			FROM cart_details cd
			JOIN books b ON b.id = cd.book_id
			JOIN carts c ON c.id = cd.cart_id
			WHERE c.user_id = :userId 
				AND c.status = 'ACTIVE'
				AND cd.selected = 1
		""";
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> results = em.createNativeQuery(sql)
			.setParameter("userId", userId)
			.unwrap(org.hibernate.query.NativeQuery.class)
			.setResultTransformer(org.hibernate.transform.Transformers.ALIAS_TO_ENTITY_MAP)
			.getResultList();
		
		return results;
	}
	
	// ================= 3. TÍNH TỔNG TIỀN CÁC SẢN PHẨM ĐÃ CHỌN =================
	public BigDecimal getSelectedTotalAmount(Integer userId) {
		String sql = """
			SELECT COALESCE(SUM(cd.price * cd.quantity), 0) as total
			FROM cart_details cd
			JOIN carts c ON c.id = cd.cart_id
			WHERE c.user_id = :userId 
				AND c.status = 'ACTIVE'
				AND cd.selected = 1
		""";
		
		BigDecimal total = (BigDecimal) em.createNativeQuery(sql)
			.setParameter("userId", userId)
			.getSingleResult();
		
		return total != null ? total : BigDecimal.ZERO;
	}
	
	// ================= 4. KIỂM TRA GIỎ HÀNG CÓ SẢN PHẨM ĐÃ CHỌN KHÔNG =================
	public boolean hasSelectedItems(Integer userId) {
		String sql = """
			SELECT COUNT(*) 
			FROM cart_details cd
			JOIN carts c ON c.id = cd.cart_id
			WHERE c.user_id = :userId 
				AND c.status = 'ACTIVE'
				AND cd.selected = 1
		""";
		
		Long count = ((Number) em.createNativeQuery(sql)
			.setParameter("userId", userId)
			.getSingleResult()).longValue();
		
		return count > 0;
	}
	
	// ================= 5. XỬ LÝ CHECKOUT CHÍNH =================
	@Transactional
	public Order checkout(Integer userId, String customerName, String phone, String address, String paymentMethod) {

		log.info("Starting checkout for user: {}", userId);
		
		// Tìm giỏ hàng ACTIVE
		Cart cart = em.createQuery("SELECT c FROM Cart c WHERE c.user.id = :uid AND c.status = 'ACTIVE'", Cart.class)
				.setParameter("uid", userId).getResultStream().findFirst()
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

		// Lấy các sản phẩm đã chọn
		List<CartDetail> cartDetails = em
				.createQuery("SELECT cd FROM CartDetail cd " + "WHERE cd.cart.id = :cid AND cd.selected = true",
						CartDetail.class)
				.setParameter("cid", cart.getId()).getResultList();

		if (cartDetails.isEmpty()) {
			throw new RuntimeException("Chưa chọn sản phẩm nào để thanh toán");
		}

		// Tạo đơn hàng - Dùng String cho status
		Order order = Order.builder()
				.orderCode(generateOrderCode())
				.user(cart.getUser())
				.customerName(customerName)
				.customerPhone(phone)
				.customerAddress(address)
				.paymentMethod(paymentMethod)
				.status("PENDING")           // String, không phải Enum
				.paymentStatus("PENDING")
				.totalAmount(BigDecimal.ZERO)
				.orderDate(LocalDateTime.now())
				.build();

		em.persist(order);
		log.info("Created order: {} with code: {}", order.getId(), order.getOrderCode());

		BigDecimal total = BigDecimal.ZERO;
		
		// Duyệt từng sản phẩm trong giỏ đã chọn
		for (CartDetail cd : cartDetails) {
			// Khóa bi quan để tránh trùng lặp
			Book book = em.find(Book.class, cd.getBook().getId(), 
					jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

			int qty = cd.getQuantity();
			
			// Kiểm tra tồn kho
			if (book.getQuantity() < qty) {
				throw new RuntimeException("Không đủ hàng: " + book.getTitle() + ". Còn " + book.getQuantity() + " cuốn");
			}

			// Trừ kho
			book.setQuantity(book.getQuantity() - qty);
			log.info("Reduced stock for book: {}, remaining: {}", book.getTitle(), book.getQuantity());
			
			// Tạo OrderDetail
			OrderDetail od = OrderDetail.builder()
					.order(order)
					.book(book)
					.quantity(qty)
					.price(cd.getPrice())
					.build();

			em.persist(od);
			
			// Tính tổng tiền
			total = total.add(od.calculateSubtotal());

			// Xóa item đã mua khỏi giỏ
			em.remove(cd);
		}

		order.setTotalAmount(total);
		cart.setUpdatedDate(LocalDateTime.now());
		
		log.info("Checkout completed. Order total: {}", total);
		
		return order;
	}
	
	// ================= 6. LẤY ĐƠN HÀNG THEO ID =================
	public Order getOrderById(Integer orderId) {
		return em.find(Order.class, orderId);
	}
	
	// ================= 7. LẤY ĐƠN HÀNG THEO ORDER CODE =================
	public Order getOrderByCode(String orderCode) {
		try {
			return em.createQuery("SELECT o FROM Order o WHERE o.orderCode = :code", Order.class)
					.setParameter("code", orderCode)
					.getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}
	
	// ================= 8. CẬP NHẬT TRẠNG THÁI THANH TOÁN =================
	@Transactional
	public void updatePaymentStatus(Integer orderId, String paymentStatus, String transactionNo) {
		Order order = getOrderById(orderId);
		if (order == null) {
			throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId);
		}
		
		order.setPaymentStatus(paymentStatus);
		
		// Nếu thanh toán thành công, cập nhật trạng thái đơn hàng
		if ("PAID".equals(paymentStatus)) {
			order.setStatus("CONFIRMED");   // String CONFIRMED
		} else if ("FAILED".equals(paymentStatus)) {
			order.setStatus("CANCELLED");   // String CANCELLED
		}
		
		log.info("Updated payment status for order {}: {}, transaction: {}", orderId, paymentStatus, transactionNo);
		
		em.merge(order);
	}
	
	// ================= 9. CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG =================
	@Transactional
	public void updateOrderStatus(Integer orderId, String status) {
		Order order = getOrderById(orderId);
		if (order == null) {
			throw new RuntimeException("Không tìm thấy đơn hàng");
		}
		
		order.setStatus(status);
		em.merge(order);
		
		log.info("Updated order status for {}: {}", orderId, status);
	}
	
	// ================= 10. LẤY DANH SÁCH ĐƠN HÀNG CỦA USER =================
	public List<Order> getOrdersByUser(Integer userId) {
		return em.createQuery("SELECT o FROM Order o WHERE o.user.id = :uid ORDER BY o.orderDate DESC", Order.class)
				.setParameter("uid", userId)
				.getResultList();
	}
	
	// ================= 11. LẤY CHI TIẾT ĐƠN HÀNG =================
	public List<OrderDetail> getOrderDetails(Integer orderId) {
		return em.createQuery("SELECT od FROM OrderDetail od WHERE od.order.id = :oid", OrderDetail.class)
				.setParameter("oid", orderId)
				.getResultList();
	}
	
	// ================= 12. HỦY ĐƠN HÀNG (HOÀN LẠI KHO) =================
	@Transactional
	public void cancelOrder(Integer orderId) {
		Order order = getOrderById(orderId);
		if (order == null) {
			throw new RuntimeException("Không tìm thấy đơn hàng");
		}
		
		// Chỉ hủy được đơn hàng đang PENDING hoặc CONFIRMED
		String status = order.getStatus();
		if (!"PENDING".equals(status) && !"CONFIRMED".equals(status)) {
			throw new RuntimeException("Không thể hủy đơn hàng ở trạng thái: " + status);
		}
		
		// Hoàn lại số lượng sách vào kho
		List<OrderDetail> orderDetails = getOrderDetails(orderId);
		for (OrderDetail od : orderDetails) {
			Book book = od.getBook();
			book.setQuantity(book.getQuantity() + od.getQuantity());
			em.merge(book);
			log.info("Restored stock for book: {}, quantity: {}", book.getTitle(), od.getQuantity());
		}
		
		// Cập nhật trạng thái đơn hàng
		order.setStatus("CANCELLED");
		em.merge(order);
		
		log.info("Order cancelled: {}", orderId);
	}
	
	// ================= 13. KIỂM TRA ĐƠN HÀNG CÓ THỂ HỦY KHÔNG =================
	public boolean isOrderCancellable(Integer orderId) {
		Order order = getOrderById(orderId);
		if (order == null) return false;
		String status = order.getStatus();
		return "PENDING".equals(status) || "CONFIRMED".equals(status);
	}
}