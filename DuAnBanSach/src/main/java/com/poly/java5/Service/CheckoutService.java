package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Cart;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.OrderDetail;
import com.poly.java5.Entity.OrderStatus;
import com.poly.java5.Entity.CartDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckoutService {
	@PersistenceContext
	private EntityManager em;
	// tạo mã đơn hàng 
	private String generateOrderCode() {
		return "ORD" + System.currentTimeMillis();
	}

	
	//	User bấm Thanh toán
	//    ↓
	//CheckoutService.checkout()
	//    ↓
	//Lấy Cart ACTIVE
	//    ↓
	//Lấy CartDetail đã selected
	//    ↓
	//Tạo Order
	//    ↓
	//For mỗi sản phẩm:
	//Lock Book
	//Kiểm tra tồn kho
	//Trừ kho
	//Tạo OrderDetail
	//Xóa CartDetail
	//    ↓
	//Cập nhật tổng tiền
	//    ↓
	//Commit transaction
	// tìm giỏ hàng, lấy các sản phẩm đã được chọn, 
	//tạo oder lưu vào db trước khi tao oderdetail, 
	@Transactional
	public Order checkout(Integer userId, String customerName, String phone, String address, String paymentMethod) {

		Cart cart = em.createQuery("SELECT c FROM Cart c WHERE c.user.id = :uid AND c.status = 'ACTIVE'", Cart.class)
				.setParameter("uid", userId).getResultStream().findFirst()
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

		List<CartDetail> cartDetails = em
				.createQuery("SELECT cd FROM CartDetail cd " + "WHERE cd.cart.id = :cid AND cd.selected = true",
						CartDetail.class)
				.setParameter("cid", cart.getId()).getResultList();

		if (cartDetails.isEmpty()) {
			throw new RuntimeException("Giỏ hàng trống");
		}

		Order order = Order.builder().orderCode(generateOrderCode()).user(cart.getUser()).customerName(customerName)
				.customerPhone(phone).customerAddress(address).paymentMethod(paymentMethod).status("PENDING")
				.paymentStatus("PENDING").totalAmount(BigDecimal.ZERO).orderDate(LocalDateTime.now()).build();

		em.persist(order);

		BigDecimal total = BigDecimal.ZERO;
		// duyệt từng CartDetail
		for (CartDetail cd : cartDetails) {
			// không cho người khác mua cùng lúc 
			Book book = em.find(Book.class, cd.getBook().getId(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

			int qty = cd.getQuantity();
			// kiểm tra tông kho 
			if (book.getQuantity() < qty) {
				throw new RuntimeException("Không đủ hàng: " + book.getTitle());
			}

			// ✅ Trừ kho tại đây
			book.setQuantity(book.getQuantity() - qty);
			// tạo oderDetail
			OrderDetail od = OrderDetail.builder().order(order).book(book).quantity(qty).price(cd.getPrice()).build();

			em.persist(od);
			// tính tổng tiền 
			total = total.add(od.calculateSubtotal());

			// ✅ Xóa item đã mua khỏi giỏ
			em.remove(cd);
		}

		order.setTotalAmount(total);

		// ✅ Cập nhật lại thời gian giỏ
		cart.setUpdatedDate(LocalDateTime.now());

		return order;
	}

}
