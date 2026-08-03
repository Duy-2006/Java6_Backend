package com.poly.java5.Service;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Repository.UserRepository;
import com.poly.java5.Utils.Utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService implements UserDetailsService {
	@PersistenceContext
	private EntityManager manager;
	@Autowired
	private UserRepository userRepository; // ← thêm dòng này, bỏ dòng kia

	@Autowired
	private EmailService emailService; // Thêm EmailService

	@Transactional
	public Map<String, String> register(User user) {
		Map<String, String> errorsMap = new HashMap<>();

		String jpql = "SELECT u FROM User u WHERE u.username = :username OR u.email = :email OR u.phone = :phone";
		List<User> users = manager.createQuery(jpql, User.class).setParameter("username", user.getUsername().trim())
				.setParameter("email", user.getEmail().trim()).setParameter("phone", user.getPhone().trim())
				.getResultList();

		for (User u : users) {
			if (u.getUsername().equals(user.getUsername().trim()))
				errorsMap.put("username", "Tên đăng nhập đã tồn tại");
			if (u.getEmail().equalsIgnoreCase(user.getEmail().trim()))
				errorsMap.put("email", "Email đã tồn tại");
			if (u.getPhone().equals(user.getPhone().trim()))
				errorsMap.put("phone", "Số điện thoại đã tồn tại");
		}

		if (errorsMap.isEmpty()) {
			user.setUsername(user.getUsername().trim());
			user.setEmail(user.getEmail().trim());
			user.setPhone(user.getPhone().trim());
			String hashedPassword = Utils.hashPassword(user.getPassword());
			user.setPassword(hashedPassword);
			user.setCreatedDate(LocalDateTime.now());
			user.setActive(true);
			manager.persist(user);
		}

		return errorsMap;
	}

	@Transactional
	public User login(String usernameOrEmail, String password) {
		System.out.println("=== LOGIN DEBUG ===");
		System.out.println("Input: [" + usernameOrEmail + "]");

		// Native query để bypass mọi vấn đề JPQL/Entity mapping
		List<User> all = manager.createNativeQuery("SELECT * FROM Users", User.class).getResultList();

		System.out.println("Tổng user trong DB: " + all.size());
		for (User u : all) {
			System.out.println(
					"  username=[" + u.getUsername() + "] email=[" + u.getEmail() + "] active=" + u.getActive());
		}

		// Tìm thủ công
		User found = all.stream().filter(
				u -> usernameOrEmail.trim().equals(u.getUsername()) || usernameOrEmail.trim().equals(u.getEmail()))
				.findFirst().orElse(null);

		if (found == null) {
			System.out.println("Không tìm thấy user!");
			return null;
		}

		String hashedInputPassword = Utils.hashPassword(password);
		System.out.println("DB:    " + found.getPassword());
		System.out.println("Input: " + hashedInputPassword);
		System.out.println("Match: " + found.getPassword().equals(hashedInputPassword));

		return found.getPassword().equals(hashedInputPassword) ? found : null;
	}

	public User findById(Integer id) {
		return manager.find(User.class, id);
	}

	@Transactional
	public void save(User user) {
		System.out.println("=== SAVING USER ===");
		System.out.println("User ID: " + user.getId());
		System.out.println("Username: " + user.getUsername());
		System.out.println("Email: " + user.getEmail());

		if (user.getId() == null) {
			// Chưa có ID -> insert mới
			if (user.getCreatedDate() == null) {
				user.setCreatedDate(LocalDateTime.now());
			}
			if (user.getActive() == null) {
				user.setActive(true);
			}
			if (user.getRole() == null) {
				user.setRole(com.poly.java5.Entity.UserRole.USER);
			}
			manager.persist(user);
			System.out.println("✅ Insert new user, generated ID: " + user.getId());
		} else {
			// Đã có ID -> update
			manager.merge(user);
			System.out.println("✅ Update existing user, ID: " + user.getId());
		}
	}

	public User findByUsername(String username) {
		if (username == null || username.trim().isEmpty())
			return null;

		String jpql = "SELECT u FROM User u WHERE u.username = :username";
		List<User> users = manager.createQuery(jpql, User.class).setParameter("username", username.trim())
				.getResultList();

		return users.isEmpty() ? null : users.get(0);
	}

	public User findByEmail(String email) {
		if (email == null || email.trim().isEmpty())
			return null;

		String jpql = "SELECT u FROM User u WHERE u.email = :email";
		List<User> users = manager.createQuery(jpql, User.class).setParameter("email", email.trim()).getResultList();

		System.out.println("findByEmail: " + email + " - Found: " + (users.isEmpty() ? "No" : "Yes"));
		return users.isEmpty() ? null : users.get(0);
	}

	@Override
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
		// Tìm theo username, email, hoặc phone
		String jpql = "SELECT u FROM User u WHERE u.username = :ue OR u.email = :ue OR u.phone = :ue";
		List<User> users = manager.createQuery(jpql, User.class).setParameter("ue", usernameOrEmail.trim())
				.getResultList();

		if (users.isEmpty()) {
			throw new UsernameNotFoundException("User không tồn tại: " + usernameOrEmail);
		}

		User user = users.get(0);
		String role = user.getRole() != null ? "ROLE_" + user.getRole().name() : "ROLE_USER";

		return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
				.password(user.getPassword() == null ? "" : user.getPassword()).authorities(role).build();
	}

	@Transactional

	// Thêm vào class UserService
	public List<User> findByRole(UserRole role) {
		return userRepository.findByRole(role);
	}

	// Toggle active bằng query trực tiếp (tối ưu hơn) - ĐÃ SỬA ĐỂ GỬI EMAIL
	@Transactional
	public void toggleActive(String username) {
		User user = findByUsername(username);
		if (user == null) {
			throw new RuntimeException("Không tìm thấy user: " + username);
		}
		boolean newStatus = !Boolean.TRUE.equals(user.getActive());
		user.setActive(newStatus);
		userRepository.save(user);

		// Gửi email thông báo (tránh lỗi SMTP làm rollback transaction)
		if (user.getEmail() != null && !user.getEmail().isEmpty()) {
			try {
				emailService.sendSimpleEmail(user.getEmail(), user.getUsername(), newStatus);
			} catch (Exception e) {
				System.err.println("Loi gui email thong bao trang thai tai khoan: " + e.getMessage());
			}
		}
	}

	// Tự động tính toán và cập nhật hạng khách hàng
	@Transactional
	public void recalculateCustomerRank(Integer userId) {
		User user = findById(userId);
		if (user == null) return;

		String jpql = "SELECT COALESCE(SUM(o.totalAmount + COALESCE(o.shippingFee, 0)), 0) FROM Order o WHERE o.user.id = :userId AND o.status = 'COMPLETED'";
		java.math.BigDecimal totalSpent = manager.createQuery(jpql, java.math.BigDecimal.class)
				.setParameter("userId", userId)
				.getSingleResult();

		user.setLifetimeValue(totalSpent);
		user.setCustomerRank(user.calculateRank());
		manager.merge(user);
		System.out.println("✅ Đã cập nhật hạng khách hàng cho User ID " + userId + " -> " + user.getCustomerRank() + " (Tổng chi tiêu: " + totalSpent + ")");
	}
}
