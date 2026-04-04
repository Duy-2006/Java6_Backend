package com.poly.java5.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.poly.java5.Entity.ActivityLog;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Cart;
import com.poly.java5.Entity.CartDetail;
import com.poly.java5.Entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {
	@PersistenceContext
	private EntityManager em;

	// Lấy giỏ hàng đang ACTIVE của user.
	//Nếu chưa có → tạo mới
	// lưu sản phẩm vào db

	public Cart getOrCreateCart(Integer userId) {
		List<Cart> carts = em
				.createQuery("SELECT c FROM Cart c WHERE c.user.id = :uid AND c.status = 'ACTIVE'", Cart.class)
				.setParameter("uid", userId).getResultList();

		if (!carts.isEmpty())
			return carts.get(0);

		User user = em.find(User.class, userId);
		if (user == null)
			throw new RuntimeException("User không tồn tại");

		Cart cart = Cart.builder().user(user).status("ACTIVE").createdDate(LocalDateTime.now())
				.updatedDate(LocalDateTime.now()).build();
		// chỗ này lưu db 
		em.persist(cart);
		return cart;
	}

	// Thêm sách vào giỏ hàng.

	public Map<String, Object> addToCart(Integer userId, Integer bookId, Integer qty) {

		if (qty <= 0)
			throw new RuntimeException("Số lượng không hợp lệ");

		Book book = em.find(Book.class, bookId);
		if (book == null)
			throw new RuntimeException("Sách không tồn tại");

		Cart cart = getOrCreateCart(userId);
		// kiêm tra sách có trong giỏ hàng chưa 
		List<CartDetail> list = em
				.createQuery("SELECT cd FROM CartDetail cd WHERE cd.cart.id = :cid AND cd.book.id = :bid",
						CartDetail.class)
				.setParameter("cid", cart.getId()).setParameter("bid", bookId).getResultList();

		CartDetail cd;
		

		if (!list.isEmpty()) {
			cd = list.get(0);
			int newQty = cd.getQuantity() + qty;

			// ✅ chỉ kiểm tra tồn kho
			if (book.getQuantity() < newQty)
				throw new RuntimeException("Chỉ còn " + book.getQuantity() + " sản phẩm");

			cd.setQuantity(newQty);
		} else {

			if (book.getQuantity() < qty)
				throw new RuntimeException("Chỉ còn " + book.getQuantity() + " sản phẩm");

			cd = CartDetail.builder().cart(cart).book(book).quantity(qty).price(book.getPrice()).selected(false)
					.build();
			em.persist(cd);
		}
		// cập nhật thời gian tạo cart
		cart.setUpdatedDate(LocalDateTime.now());
		
		return getCartSummary(userId);
	}

	// Cập nhật số lượng sản phẩm trong giỏ.

	public Map<String, Object> updateCartItem(Integer userId, Integer cartDetailId, Integer newQty) {

		CartDetail cd = em.find(CartDetail.class, cartDetailId);
		if (cd == null)
			throw new RuntimeException("Không tìm thấy sản phẩm");

		if (!cd.getCart().getUser().getId().equals(userId))
			throw new RuntimeException("Không có quyền");

		if (newQty <= 0) {
			return removeFromCart(userId, cartDetailId);
		}

		Book book = cd.getBook();

		// ✅ chỉ kiểm tra tồn kkho 
		if (book.getQuantity() < newQty)
			throw new RuntimeException("Chỉ còn " + book.getQuantity() + " sản phẩm");

		cd.setQuantity(newQty);
		cd.getCart().setUpdatedDate(LocalDateTime.now());

		return getCartSummary(userId);
	}

	// Xóa sản phẩm khỏi giỏ hàng	
//	1. Tìm CartDetail theo id
//	2. Kiểm tra quyền user
//	3. Lưu reference Cart
//	4. Xóa CartDetail
//	5. Cập nhật updatedDate của Cart
//	6. Tính lại summary
//	7. Trả kết quả
	public Map<String, Object> removeFromCart(Integer userId, Integer cartDetailId) {

    CartDetail cd = em.find(CartDetail.class, cartDetailId);
    if (cd == null)
        throw new RuntimeException("Không tìm thấy sản phẩm");

    if (!cd.getCart().getUser().getId().equals(userId))
        throw new RuntimeException("Không có quyền");

    Cart cart = cd.getCart();   // LƯU TRƯỚC

    em.remove(cd);

    cart.setUpdatedDate(LocalDateTime.now()); // dùng cart đã lưu

    return getCartSummary(userId);
}

	// Chọn / bỏ chọn sản phẩm để thanh toán.

	public void updateSelected(Integer userId, Integer cartDetailId, Boolean selected) {

		CartDetail cd = em.find(CartDetail.class, cartDetailId);
		if (cd == null)
			throw new RuntimeException("CartDetail không tồn tại");

		if (!cd.getCart().getUser().getId().equals(userId))
			throw new RuntimeException("Không có quyền");

		cd.setSelected(selected);
	}

	// Trả về toàn bộ thông tin giỏ hàng.

	public Map<String, Object> getCartSummary(Integer userId) {

		Cart cart = getOrCreateCart(userId);

		List<CartDetail> details = em
				.createQuery("SELECT cd FROM CartDetail cd WHERE cd.cart.id = :cid", CartDetail.class)
				.setParameter("cid", cart.getId()).getResultList();

		BigDecimal total = BigDecimal.ZERO;
		int totalItems = 0;
		List<Map<String, Object>> items = new ArrayList<>();

		for (CartDetail cd : details) {
			Map<String, Object> m = new HashMap<>();
			m.put("cartDetailId", cd.getId());
			m.put("title", cd.getBook().getTitle());
			m.put("author", cd.getBook().getAuthor());
			m.put("imageUrl", cd.getBook().getImageUrl());
			m.put("quantity", cd.getQuantity());
			m.put("price", cd.getPrice());
			m.put("selected", cd.getSelected());

			BigDecimal itemTotal = cd.calculateTotal();
			m.put("itemTotal", itemTotal);

			total = total.add(itemTotal);
			totalItems += cd.getQuantity();

			items.add(m);
		}

		Map<String, Object> result = new HashMap<>();
		result.put("cartItems", items);
		result.put("totalItems", totalItems);
		result.put("totalAmount", total);

		return result;
	}

	// ================= SELECTED =================

	public List<CartDetail> getSelectedCartDetails(Integer userId) {
		Cart cart = getOrCreateCart(userId);
		return em.createQuery("SELECT cd FROM CartDetail cd WHERE cd.cart.id = :cid AND cd.selected = true",
				CartDetail.class).setParameter("cid", cart.getId()).getResultList();
	}

	public BigDecimal getSelectedTotalAmount(Integer userId) {
		Cart cart = getOrCreateCart(userId);
		return em.createQuery(
				"SELECT COALESCE(SUM(cd.price * cd.quantity),0) FROM CartDetail cd WHERE cd.cart.id = :cid AND cd.selected = true",
				BigDecimal.class).setParameter("cid", cart.getId()).getSingleResult();
	}

	public int getCartItemCount(Integer userId) {
		Cart cart = getOrCreateCart(userId);
		Long count = em.createQuery("SELECT COALESCE(SUM(cd.quantity),0) FROM CartDetail cd WHERE cd.cart.id = :cid",
				Long.class).setParameter("cid", cart.getId()).getSingleResult();
		return count.intValue();
	}

	@Transactional
	public void clearCart(Integer userId) {

		Cart cart = getOrCreateCart(userId);

		List<CartDetail> details = em
				.createQuery("SELECT cd FROM CartDetail cd WHERE cd.cart.id = :cid", CartDetail.class)
				.setParameter("cid", cart.getId()).getResultList();

		for (CartDetail cd : details) {
			em.remove(cd);
		}

		cart.setUpdatedDate(LocalDateTime.now());
	}

	public List<Map<String, Object>> getSelectedCartItems(Integer userId) {

		Cart cart = getOrCreateCart(userId);

		List<CartDetail> details = em
				.createQuery("SELECT cd FROM CartDetail cd " + "JOIN FETCH cd.book "
						+ "WHERE cd.cart.id = :cid AND cd.selected = true", CartDetail.class)
				.setParameter("cid", cart.getId()).getResultList();

		List<Map<String, Object>> items = new ArrayList<>();

		for (CartDetail cd : details) {
			Map<String, Object> m = new HashMap<>();
			m.put("title", cd.getBook().getTitle());
			m.put("imageUrl", cd.getBook().getImageUrl());
			m.put("price", cd.getPrice());
			m.put("quantity", cd.getQuantity());
			m.put("itemTotal", cd.calculateTotal());
			items.add(m);
		}

		return items;
	}

}
