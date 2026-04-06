package com.poly.java5.Controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.poly.java5.Entity.*;
import com.poly.java5.Repository.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private BookRepository bookRepo;
	@Autowired
	private CategoryRepository catRepo;
	@Autowired
	private AuthorRepository authorRepo;
	@Autowired
	private OrderRepository orderRepo;
	@Autowired
	private UserRepository userRepo;
	@Autowired
	private InventoryLogRepository inventoryRepo;
	@Autowired
	private HttpSession session;

	// --- HELPER: KIỂM TRA QUYỀN ADMIN ---
	private boolean isAdmin() {

	    Integer userId = (Integer) session.getAttribute("USER_ID");

	    if (userId == null) {
	        return false;
	    }

	    User user = userRepo.findById(userId).orElse(null);

	    return user != null && user.getRole() == UserRole.ADMIN;
	}

	// ================= DASHBOARD (THỐNG KÊ TỔNG QUAN) =================
	@GetMapping
	public String dashboard(Model model) {
		// 1. Đếm tổng số sách đang có trong hệ thống
		long totalBooks = bookRepo.count();

		// 2. Đếm tổng số đơn hàng đã bán
		long totalOrders = orderRepo.count();

		// 3. Đếm tổng số người dùng (Bao gồm cả Admin và Khách)
		long totalUsers = userRepo.count();

		// --- NÂNG CAO: Nếu bạn chỉ muốn đếm Khách hàng (UserRole.USER) ---
		// long totalCustomers = userRepo.findAll().stream()
		// .filter(u -> u.getRole() == UserRole.USER)
		// .count();

		// 4. Gửi số liệu sang giao diện (dashboard.html)
		model.addAttribute("totalBooks", totalBooks);
		model.addAttribute("totalOrders", totalOrders);
		model.addAttribute("totalUsers", totalUsers);

		return "admin/dashboard";
	}

	// ================== QUẢN LÝ THỂ LOẠI ==================
	@GetMapping("/categories")
	public String listCategories(Model model) {
		model.addAttribute("categories", catRepo.findAll());
		return "admin/categories";
	}

	@GetMapping("/categories/create")
	public String formCategory(Model model) {
		model.addAttribute("category", new Category());
		return "admin/category-form";
	}

	// [FIX] Đã đổi về Long để khớp với Repository
	@GetMapping("/categories/edit/{id}")
	public String editCategory(@PathVariable("id") Integer id, Model model) {
		Category cat = catRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
		model.addAttribute("category", cat);
		return "admin/category-form";
	}

	@PostMapping("/categories/save")
	public String saveCategory(@Valid @ModelAttribute("category") Category category, BindingResult result) {
		if (result.hasErrors())
			return "admin/category-form";
		catRepo.save(category);
		return "redirect:/admin/categories";
	}

	// [FIX] Đã đổi về Long
	@GetMapping("/categories/delete/{id}")
	public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes params) {
		if (!isAdmin()) {
			params.addFlashAttribute("error", "Quyền hạn không đủ!");
			return "redirect:/admin/categories";
		}
		try {
			catRepo.deleteById(id);
			params.addFlashAttribute("success", "Xóa thành công!");
		} catch (Exception e) {
			params.addFlashAttribute("error", "Không thể xóa thể loại đang có sách!");
		}
		return "redirect:/admin/categories";
	}

	// ================== QUẢN LÝ TÁC GIẢ ==================
	@GetMapping("/authors")
	public String listAuthors(Model model) {
		model.addAttribute("authors", authorRepo.findAll());
		return "admin/authors";
	}

	@GetMapping("/authors/create")
	public String formAuthor(Model model) {
		model.addAttribute("author", new Author());
		return "admin/author-form";
	}

	// [FIX] Đã đổi về Long
	@GetMapping("/authors/edit/{id}")
	public String editAuthor(@PathVariable("id") Long id, Model model) {
		Author auth = authorRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
		model.addAttribute("author", auth);
		return "admin/author-form";
	}

	@PostMapping("/authors/save")
	public String saveAuthor(@Valid @ModelAttribute("author") Author author, BindingResult result) {
		if (result.hasErrors())
			return "admin/author-form";
		authorRepo.save(author);
		return "redirect:/admin/authors";
	}

	// [FIX] Đã đổi về Long
	@GetMapping("/authors/delete/{id}")
	public String deleteAuthor(@PathVariable("id") Long id, RedirectAttributes params) {
		if (!isAdmin()) {
			params.addFlashAttribute("error", "Quyền hạn không đủ!");
			return "redirect:/admin/authors";
		}
		try {
			authorRepo.deleteById(id);
			params.addFlashAttribute("success", "Xóa thành công!");
		} catch (Exception e) {
			params.addFlashAttribute("error", "Không thể xóa tác giả đang có sách!");
		}
		return "redirect:/admin/authors";
	}

	// ================= QUẢN LÝ SÁCH (BOOK) =================
	@GetMapping("/books")
	public String listBooks(Model model) {
		model.addAttribute("books", bookRepo.findByDeletedFalse());
		return "admin/books";
	}

	@GetMapping("/books/create")
	public String createBook(Model model) {
		model.addAttribute("book", new Book());
		model.addAttribute("categories", catRepo.findAll());
		model.addAttribute("authors", authorRepo.findAll());
		return "admin/book-form";
	}

	// [FIX] Đã đổi về Long
	@GetMapping("/books/edit/{id}")
	public String editBook(@PathVariable("id") Integer id, Model model) {
		Book book = bookRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
		model.addAttribute("book", book);
		model.addAttribute("categories", catRepo.findAll());
		model.addAttribute("authors", authorRepo.findAll());
		return "admin/book-form";
	}

	@PostMapping("/books/save")
	public String saveBook(@Valid @ModelAttribute("book") Book book, BindingResult result,
			@RequestParam("imageFile") MultipartFile file, Model model) {

		if (result.hasErrors()) {
			model.addAttribute("categories", catRepo.findAll());
			model.addAttribute("authors", authorRepo.findAll());
			return "admin/book-form";
		}

		try {

			Book existing;

			// ================= EDIT =================
			if (book.getId() != null) {

				existing = bookRepo.findById(book.getId()).orElseThrow(() -> new RuntimeException("Book not found"));

				// Cập nhật từng field (KHÔNG đụng tới reviews, orderDetails...)
				existing.setTitle(book.getTitle());
				existing.setIsbn(book.getIsbn());
				existing.setPrice(book.getPrice());
				existing.setPublisher(book.getPublisher());
				existing.setDescription(book.getDescription());
				existing.setActive(book.getActive());
				existing.setCategory(book.getCategory());
				existing.setAuthor(book.getAuthor());
				existing.setQuantity(book.getQuantity());

			} else {
				// ================= CREATE =================
				existing = book;
			}

			// ================= UPLOAD ẢNH =================
			if (!file.isEmpty()) {

				String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
				String uploadDir = "C:/uploads/books/";

				File dir = new File(uploadDir);
				if (!dir.exists()) {
					dir.mkdirs();
				}

				Path path = Paths.get(uploadDir + filename);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				existing.setImageUrl("books/" + filename);
			}

			bookRepo.save(existing);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/admin/books";
	}

	// [FIX] Đã đổi về Long
	@GetMapping("/books/delete/{id}")
	public String deleteBook(@PathVariable("id") Integer id, RedirectAttributes params) {
		if (!isAdmin()) {
			params.addFlashAttribute("error", "Quyền hạn không đủ!");
			return "redirect:/admin/books";
		}
		Book book = bookRepo.findById(id).orElse(null);
		if (book != null) {
			book.setDeleted(true);
			bookRepo.save(book);
			params.addFlashAttribute("success", "Đã ẩn sách thành công!");
		}
		return "redirect:/admin/books";
	}

	// ================= QUẢN LÝ KHO (INVENTORY) =================
	@GetMapping("/inventory")
	public String inventory(Model model) {
		List<Book> lowStockBooks = bookRepo.findAll().stream()
				.filter(b -> b.getQuantity() != null && b.getQuantity() < 10).collect(Collectors.toList());
		model.addAttribute("lowStockBooks", lowStockBooks);
		model.addAttribute("books", bookRepo.findAll());
		model.addAttribute("logs", inventoryRepo.findAllByOrderByLogDateDesc());
		return "admin/inventory";
	}

	// [NÂNG CẤP] Xử lý đa luồng giao dịch: Nhập/Xuất/Hủy/Trả
	@PostMapping("/inventory/transaction")
	public String processTransaction(@RequestParam("bookId") Integer bookId, // Giữ kiểu Long theo chuẩn của bạn
			@RequestParam("quantity") Integer quantity, @RequestParam("type") String type, // Nhận loại giao dịch
																							// (IMPORT, EXPORT, RETURN,
																							// DEFECT)
			@RequestParam(value = "supplier", required = false) String supplier, // Nhận tên Nhà cung cấp
			@RequestParam("note") String note, RedirectAttributes params) {
		if (quantity <= 0) {
			params.addFlashAttribute("error", "Số lượng giao dịch phải lớn hơn 0");
			return "redirect:/admin/inventory";
		}

		Book book = bookRepo.findById(bookId).orElse(null);
		if (book != null) {
			// 1. Nối tên Nhà cung cấp/Nguồn gốc vào Ghi chú (Nếu có)
			String finalNote = (supplier != null && !supplier.trim().isEmpty()) ? "[Nguồn: " + supplier + "] " + note
					: note;

			// 2. Xử lý logic Cộng / Trừ tồn kho tùy theo loại giao dịch
			if ("IMPORT".equals(type) || "RETURN".equals(type)) {
				// Nhập hàng từ NCC hoặc Khách trả hàng -> Cộng kho
				book.increaseStock(quantity);
				// (Nếu hàm increaseStock báo lỗi do chưa có trong Entity, bạn dùng:
				// book.setQuantity(book.getQuantity() + quantity); )
			} else if ("EXPORT".equals(type) || "DEFECT".equals(type)) {
				// Xuất kho hoặc Báo hỏng/Hủy -> Trừ kho
				try {
					book.decreaseStock(quantity);
				} catch (IllegalArgumentException e) {
					params.addFlashAttribute("error", "Lỗi: Số lượng tồn kho không đủ để xuất/hủy!");
					return "redirect:/admin/inventory";
				}
			}

			bookRepo.save(book);

			// 3. Ghi nhận Lịch sử (Log)
			InventoryLog log = InventoryLog.builder().book(book).changeAmount(quantity).type(type).note(finalNote)
					.build();

			inventoryRepo.save(log);
			params.addFlashAttribute("success", "Đã ghi nhận giao dịch thành công cho sách: " + book.getTitle());
		} else {
			params.addFlashAttribute("error", "Không tìm thấy sách trong hệ thống!");
		}

		return "redirect:/admin/inventory";
	}

	// ================= QUẢN LÝ ĐƠN HÀNG (ORDER) =================
	@GetMapping("/orders")
	public String listOrders(Model model) {
		model.addAttribute("orders", orderRepo.findAllByOrderByOrderDateDesc());
		return "admin/orders";
	}

	// Đã trả lại kiểu Integer để khớp với OrderRepository
	@GetMapping("/orders/detail/{id}")
	public String detailOrder(@PathVariable("id") Integer id, Model model) {
		Order order = orderRepo.findById(id).orElse(null);
		model.addAttribute("order", order);
		return "admin/order-detail";
	}

	// Đã trả lại kiểu Integer và giữ nguyên logic 1 chiều
	@PostMapping("/orders/update/{id}")
	public String updateOrderStatus(@PathVariable("id") Integer id, @RequestParam("status") String newStatus,
			RedirectAttributes ra) {
		Order order = orderRepo.findById(id).orElse(null);
		if (order == null) {
			ra.addFlashAttribute("error", "Không tìm thấy đơn hàng");
			return "redirect:/admin/orders";
		}

		String currentStatus = order.getStatus();
		boolean isValidFlow = false;

		// 1. NGHIỆP VỤ LUỒNG 1 CHIỀU: PENDING -> CONFIRMED -> SHIPPING -> COMPLETED
		if ("PENDING".equals(currentStatus)) {
			// Đang chờ xác nhận -> Lên: Đã xác nhận HOẶC Hủy
			if ("CONFIRMED".equals(newStatus) || "CANCELLED".equals(newStatus))
				isValidFlow = true;
		} else if ("CONFIRMED".equals(currentStatus)) {
			// Đã xác nhận -> Lên: Đang giao HOẶC Hủy
			if ("SHIPPING".equals(newStatus) || "CANCELLED".equals(newStatus))
				isValidFlow = true;
		} else if ("SHIPPING".equals(currentStatus)) {
			// Đang giao -> Lên: Hoàn thành (Không cho phép hủy khi đang giao)
			if ("COMPLETED".equals(newStatus))
				isValidFlow = true;
		}

		// 2. Kiểm tra nếu chọn lại trạng thái cũ
		if (currentStatus != null && currentStatus.equals(newStatus)) {
			ra.addFlashAttribute("error", "Trạng thái đơn hàng không thay đổi.");
			return "redirect:/admin/orders/detail/" + id;
		}

		// 3. Thực thi cập nhật hoặc báo lỗi
		if (isValidFlow) {
			order.setStatus(newStatus);
			orderRepo.save(order);
			ra.addFlashAttribute("success", "Cập nhật trạng thái thành công!");
		} else {
			ra.addFlashAttribute("error",
					"Lỗi: Thao tác không hợp lệ! Trạng thái đơn hàng chỉ được phép tiến lên theo quy trình.");
		}

		return "redirect:/admin/orders/detail/" + id;
	}

	// ================= QUẢN LÝ KHÁCH HÀNG (CUSTOMERS - CÓ TÌM KIẾM)
	// =================
	@GetMapping("/customers")
	public String listCustomers(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
		// 1. Lấy tất cả khách hàng (Role = USER)
		List<User> customers = userRepo.findAll().stream().filter(u -> u.getRole() == UserRole.USER)
				.collect(Collectors.toList());

		// 2. Nếu có từ khóa tìm kiếm -> Thực hiện lọc (Không phân biệt chữ hoa/thường)
		if (keyword != null && !keyword.trim().isEmpty()) {
			String kw = keyword.toLowerCase().trim();
			customers = customers.stream()
					.filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw))
							|| (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw))
							|| (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw)))
					.collect(Collectors.toList());

			// Trả lại từ khóa lên giao diện để giữ chữ trong ô input
			model.addAttribute("keyword", keyword);
		}

		model.addAttribute("customers", customers);
		return "admin/customers";
	}

	@PostMapping("/customers/toggle/{username}")
@Transactional
public String toggleCustomer(@PathVariable("username") String username,
                             RedirectAttributes params) {

    System.out.println("TOGGLE CALLED");

    User user = userRepo.findByUsername(username);

    if (user != null) {
        user.setActive(!user.getActive());
        userRepo.save(user);
        System.out.println("NEW ACTIVE = " + user.getActive());
    }

    return "redirect:/admin/customers";
}

	@GetMapping("/customers/history/{username}")
	public String customerHistory(@PathVariable("username") String username, Model model) {
		User user = userRepo.findByUsername(username);
		model.addAttribute("customer", user);
		return "admin/customer-history";
	}

	// ================= QUẢN LÝ NHÂN SỰ (USERS) =================
	@GetMapping("/users")
	public String listUsers(Model model, RedirectAttributes params) {
		if (!isAdmin()) {
			params.addFlashAttribute("error", "Truy cập bị từ chối!");
			return "redirect:/admin";
		}
		model.addAttribute("users", userRepo.findAll());
		return "admin/users";
	}


	@GetMapping("/users/create")
	public String formUser(Model model) {
		if (!isAdmin())
			return "redirect:/admin";
		model.addAttribute("user", new User());
		return "admin/user-form";
	}

	@GetMapping("/users/edit/{username}")
	public String editUser(@PathVariable("username") String username, Model model) {
		if (!isAdmin())
			return "redirect:/admin";
		User user = userRepo.findByUsername(username);
		model.addAttribute("user", user);
		return "admin/user-form";
	}

	@PostMapping("/users/save")
	public String saveUser(@Valid @ModelAttribute("user") User user, BindingResult result) {
		if (!isAdmin())
			return "redirect:/admin";
		if (result.hasErrors())
			return "admin/user-form";
		userRepo.save(user);
		return "redirect:/admin/users";
	}

	@GetMapping("/users/delete/{username}")
	public String deleteUser(@PathVariable String username, RedirectAttributes params) {
		if (!isAdmin()) {
			params.addFlashAttribute("error", "Truy cập bị từ chối!");
			return "redirect:/admin";
		}
		User currentUser = (User) session.getAttribute("currentUser");
		if (currentUser != null && currentUser.getUsername().equals(username)) {
			params.addFlashAttribute("error", "Không thể tự xóa tài khoản đang đăng nhập!");
			return "redirect:/admin/users";
		}
		User user = userRepo.findByUsername(username);
		if (user != null) {
			userRepo.delete(user);
			params.addFlashAttribute("success", "Đã xóa nhân viên: " + username);
		}
		return "redirect:/admin/users";
	}
}