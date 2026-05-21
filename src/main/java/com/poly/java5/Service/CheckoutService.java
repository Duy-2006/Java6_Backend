package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Cart;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.OrderDetail;
import com.poly.java5.Entity.CartDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.LockModeType;
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
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMHHmm"));
		int random = (int) (Math.random() * 90) + 10;
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
				        cd.price as price,
				        cd.price * cd.quantity as itemTotal
				    FROM cart_details cd
				    JOIN books b ON b.id = cd.book_id
				    JOIN carts c ON c.id = cd.cart_id
				    WHERE c.user_id = :userId
				        AND c.status = 'ACTIVE'
				        AND cd.selected = 1
				""";

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> results = em.createNativeQuery(sql).setParameter("userId", userId)
				.unwrap(org.hibernate.query.NativeQuery.class)
				.setResultTransformer(org.hibernate.transform.Transformers.ALIAS_TO_ENTITY_MAP).getResultList();

		log.info("Found {} selected items for user: {}", results.size(), userId);
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

		BigDecimal total = (BigDecimal) em.createNativeQuery(sql).setParameter("userId", userId).getSingleResult();

		log.info("Total amount for user {}: {}", userId, total);
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

		Long count = ((Number) em.createNativeQuery(sql).setParameter("userId", userId).getSingleResult()).longValue();
		return count > 0;
	}

	// ================= 5. CHECKOUT MỚI – HỖ TRỢ GIÁ KHUYẾN MÃI =================
	@Transactional
	public Order checkout(Integer userId, String customerName, String phone, String address, 
	                      String paymentMethod, List<Map<String, Object>> requestItems, BigDecimal discountAmount) {
		log.info("========== START CHECKOUT (with discounted prices) ==========");
		log.info("User ID: {}", userId);
		log.info("Customer: {} - {} - {}", customerName, phone, address);
		log.info("Payment Method: {}", paymentMethod);
		log.info("Items from request: {}", requestItems);

		// 1. Tìm giỏ hàng ACTIVE
		Cart cart = em.createQuery("SELECT c FROM Cart c WHERE c.user.id = :uid AND c.status = 'ACTIVE'", Cart.class)
				.setParameter("uid", userId).getResultStream().findFirst()
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

		// 2. Lấy các sản phẩm đã chọn từ DB (chỉ để kiểm tra và xóa sau)
		List<CartDetail> cartDetails = em
				.createQuery("SELECT cd FROM CartDetail cd WHERE cd.cart.id = :cid AND cd.selected = true", CartDetail.class)
				.setParameter("cid", cart.getId()).getResultList();

		if (cartDetails.isEmpty()) {
			throw new RuntimeException("Chưa chọn sản phẩm nào để thanh toán");
		}

		// 3. Tạo đơn hàng
		String orderCode = generateOrderCode();
		Order order = Order.builder()
				.orderCode(orderCode)
				.user(cart.getUser())
				.customerName(customerName)
				.customerPhone(phone)
				.customerAddress(address)
				.paymentMethod(paymentMethod)
				.status("PENDING")
				.paymentStatus("PENDING")
				.totalAmount(BigDecimal.ZERO)
				.orderDate(LocalDateTime.now())
				.build();
		em.persist(order);
		log.info("Order created: ID={}, Code={}", order.getId(), order.getOrderCode());

		// 4. Xử lý từng sản phẩm theo request (đã có giá giảm)
		BigDecimal total = BigDecimal.ZERO;

		for (Map<String, Object> reqItem : requestItems) {
			Integer bookId = (Integer) reqItem.get("bookId");
			Integer quantity = (Integer) reqItem.get("quantity");
			BigDecimal price = new BigDecimal(reqItem.get("price").toString()); // giá đã giảm từ FE

			Book book = em.find(Book.class, bookId, LockModeType.PESSIMISTIC_WRITE);
			if (book == null) {
				throw new RuntimeException("Sách không tồn tại, ID: " + bookId);
			}
			if (book.getQuantity() < quantity) {
				throw new RuntimeException("Không đủ hàng: " + book.getTitle() + ". Còn " + book.getQuantity() + " cuốn");
			}

			// Trừ kho
			book.setQuantity(book.getQuantity() - quantity);
			log.info("Stock reduced for: {}, remaining: {}", book.getTitle(), book.getQuantity());

			// Tạo OrderDetail với giá đã giảm
			OrderDetail od = OrderDetail.builder()
					.order(order)
					.book(book)
					.quantity(quantity)
					.price(price)
					.build();
			em.persist(od);
			total = total.add(od.calculateSubtotal());

			// Xóa CartDetail tương ứng (dựa trên bookId)
			cartDetails.stream()
				.filter(cd -> cd.getBook().getId().equals(bookId))
				.findFirst()
				.ifPresent(em::remove);
		}

		// 5. Cập nhật tổng tiền đơn hàng (trừ đi discountAmount)
		if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
			total = total.subtract(discountAmount);
			if (total.compareTo(BigDecimal.ZERO) < 0) {
				total = BigDecimal.ZERO;
			}
		}
		order.setTotalAmount(total);
		cart.setUpdatedDate(LocalDateTime.now());

		log.info("Order total (with discount): {} VND", total);
		log.info("========== CHECKOUT COMPLETED ==========");
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
					.setParameter("code", orderCode).getSingleResult();
		} catch (Exception e) {
			log.warn("Order not found with code: {}", orderCode);
			return null;
		}
	}

	// ================= 8. LẤY ĐƠN HÀNG THEO ORDER CODE (KHÔNG EXCEPTION) =================
	public Order getOrderByOrderCode(String orderCode) {
		try {
			return em.createQuery("SELECT o FROM Order o WHERE o.orderCode = :orderCode", Order.class)
					.setParameter("orderCode", orderCode).getSingleResult();
		} catch (Exception e) {
			log.error("Không tìm thấy đơn hàng với code: {}", orderCode);
			return null;
		}
	}

	// ================= 9. CẬP NHẬT TRẠNG THÁI THANH TOÁN =================
	@Transactional
	public void updatePaymentStatus(Integer orderId, String paymentStatus, String transactionNo) {
		Order order = getOrderById(orderId);
		if (order == null) throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId);
		order.setPaymentStatus(paymentStatus);
		if ("PAID".equals(paymentStatus)) order.setStatus("CONFIRMED");
		else if ("FAILED".equals(paymentStatus)) order.setStatus("CANCELLED");
		if (transactionNo != null && !transactionNo.isEmpty()) order.setTransactionNo(transactionNo);
		log.info("Updated payment status for order {}: {}, transaction: {}", orderId, paymentStatus, transactionNo);
		em.merge(order);
	}

	// ================= 10. XỬ LÝ THANH TOÁN VNPAY THÀNH CÔNG =================
	@Transactional
	public void handleSuccessfulPayment(String orderCode, String transactionNo) {
		Order order = getOrderByOrderCode(orderCode);
		if (order == null) throw new RuntimeException("Không tìm thấy đơn hàng với mã: " + orderCode);
		order.setPaymentStatus("PAID");
		order.setStatus("CONFIRMED");
		order.setTransactionNo(transactionNo);
		log.info("Payment successful for order: {}, transaction: {}", orderCode, transactionNo);
		em.merge(order);
	}

	// ================= 11. XỬ LÝ THANH TOÁN VNPAY THẤT BẠI =================
	@Transactional
	public void handleFailedPayment(String orderCode, String transactionNo) {
		Order order = getOrderByOrderCode(orderCode);
		if (order == null) throw new RuntimeException("Không tìm thấy đơn hàng với mã: " + orderCode);
		order.setPaymentStatus("FAILED");
		order.setStatus("CANCELLED");
		order.setTransactionNo(transactionNo);
		// Hoàn lại số lượng sách vào kho
		List<OrderDetail> orderDetails = getOrderDetails(order.getId());
		for (OrderDetail od : orderDetails) {
			Book book = od.getBook();
			book.setQuantity(book.getQuantity() + od.getQuantity());
			em.merge(book);
			log.info("Restored stock for book: {}, quantity: {}", book.getTitle(), od.getQuantity());
		}
		log.info("Payment failed for order: {}, transaction: {}", orderCode, transactionNo);
		em.merge(order);
	}

	// ================= 12. CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG =================
	@Transactional
	public void updateOrderStatus(Integer orderId, String status) {
		Order order = getOrderById(orderId);
		if (order == null) throw new RuntimeException("Không tìm thấy đơn hàng");
		order.setStatus(status);
		em.merge(order);
		log.info("Updated order status for {}: {}", orderId, status);
	}

	// ================= 13. LẤY DANH SÁCH ĐƠN HÀNG CỦA USER =================
	public List<Order> getOrdersByUser(Integer userId) {
		return em.createQuery("SELECT o FROM Order o WHERE o.user.id = :uid ORDER BY o.orderDate DESC", Order.class)
				.setParameter("uid", userId).getResultList();
	}

	// ================= 14. LẤY CHI TIẾT ĐƠN HÀNG =================
	public List<OrderDetail> getOrderDetails(Integer orderId) {
		return em.createQuery("SELECT od FROM OrderDetail od WHERE od.order.id = :oid", OrderDetail.class)
				.setParameter("oid", orderId).getResultList();
	}

	// ================= 15. HỦY ĐƠN HÀNG (HOÀN LẠI KHO) =================
	@Transactional
	public void cancelOrder(Integer orderId) {
		Order order = getOrderById(orderId);
		if (order == null) throw new RuntimeException("Không tìm thấy đơn hàng");
		String status = order.getStatus();
		if (!"PENDING".equals(status) && !"CONFIRMED".equals(status))
			throw new RuntimeException("Không thể hủy đơn hàng ở trạng thái: " + status);
		List<OrderDetail> orderDetails = getOrderDetails(orderId);
		for (OrderDetail od : orderDetails) {
			Book book = od.getBook();
			book.setQuantity(book.getQuantity() + od.getQuantity());
			em.merge(book);
		}
		order.setStatus("CANCELLED");
		em.merge(order);
		log.info("Order cancelled: {}", orderId);
	}

	// ================= 16. KIỂM TRA ĐƠN HÀNG CÓ THỂ HỦY KHÔNG =================
	public boolean isOrderCancellable(Integer orderId) {
		Order order = getOrderById(orderId);
		if (order == null) return false;
		String status = order.getStatus();
		return "PENDING".equals(status) || "CONFIRMED".equals(status);
	}
}