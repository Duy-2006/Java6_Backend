package com.poly.java5.Controller;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.DTO.OrderDTO;
import com.poly.java5.DTO.OrderDetailDTO;
import com.poly.java5.DTO.OrderFullDTO;
import com.poly.java5.Entity.*;
import com.poly.java5.Repository.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AdminApiController {
	 private final BookRepository bookRepo;
	    private final CategoryRepository catRepo;
	    private final AuthorRepository authorRepo;
	    private final OrderRepository orderRepo;
	    private final UserRepository userRepo;
	    private final InventoryLogRepository inventoryRepo;

	    // ================= DASHBOARD =================
	    @GetMapping("/dashboard")
	    public ResponseEntity<?> dashboard() {

	        long totalBooks = bookRepo.count();
	        long totalOrders = orderRepo.count();
	        long totalUsers = userRepo.count();

	        return ResponseEntity.ok(
	            java.util.Map.of(
	                "totalBooks", totalBooks,
	                "totalOrders", totalOrders,
	                "totalUsers", totalUsers
	            )
	        );
	    }

	    // ================= INVENTORY =================
	    @GetMapping("/inventory")
	    public ResponseEntity<?> inventory() {

	        List<Book> books = bookRepo.findAll();

	        List<Book> lowStock = books.stream()
	                .filter(b -> b.getQuantity() != null && b.getQuantity() < 10)
	                .toList();

	        return ResponseEntity.ok(
	            java.util.Map.of(
	                "books", books,
	                "lowStockBooks", lowStock,
	                "logs", inventoryRepo.findAllByOrderByLogDateDesc()
	            )
	        );
	    }

	    // ================= ORDER =================
	    @GetMapping("/orders")
	    public List<OrderDTO> getOrders() {

	        return orderRepo.findAllByOrderByOrderDateDesc()
	                .stream().map(o -> {
	                    OrderDTO dto = new OrderDTO();
	                    dto.setId(o.getId());
	                    dto.setOrderCode(o.getOrderCode());
	                    dto.setStatus(o.getStatus());
	                    dto.setTotalAmount(o.getTotalAmount());
	                    dto.setOrderDate(o.getOrderDate());
	                    return dto;
	                }).toList();
	    }

	    // chi tiết đơn
	    @GetMapping("/orders/{id}")
	    public ResponseEntity<?> getOrderDetail(@PathVariable Integer id) {

	        Order order = orderRepo.findById(id).orElse(null);

	        if (order == null) {
	            return ResponseEntity.notFound().build();
	        }

	        OrderFullDTO dto = new OrderFullDTO();
	        dto.setOrderCode(order.getOrderCode());
	        dto.setStatus(order.getStatus());
	        dto.setCustomerName(order.getCustomerName());
	        dto.setCustomerPhone(order.getCustomerPhone());
	        dto.setCustomerAddress(order.getCustomerAddress());
	        dto.setTotalAmount(order.getTotalAmount());

	        List<OrderDetailDTO> details = order.getOrderDetails().stream().map(d -> {
	            OrderDetailDTO od = new OrderDetailDTO();
	            od.setBookTitle(d.getBook().getTitle());
	            od.setQuantity(d.getQuantity());
	            od.setPrice(d.getPrice());
	            return od;
	        }).toList();

	        dto.setDetails(details);

	        return ResponseEntity.ok(dto);
	    }

	    // update trạng thái
	    @PostMapping("/orders/update/{id}")
	    public ResponseEntity<?> updateOrderStatus(@PathVariable Integer id,
	                                               @RequestParam String newStatus) {

	        Order order = orderRepo.findById(id).orElse(null);

	        if (order == null) {
	            return ResponseEntity.badRequest().body("Không tìm thấy đơn hàng");
	        }

	        String currentStatus = order.getStatus();
	        boolean isValidFlow = false;

	        if ("PENDING".equals(currentStatus)) {
	            if ("CONFIRMED".equals(newStatus) || "CANCELLED".equals(newStatus))
	                isValidFlow = true;

	        } else if ("CONFIRMED".equals(currentStatus)) {
	            if ("SHIPPING".equals(newStatus) || "CANCELLED".equals(newStatus))
	                isValidFlow = true;

	        } else if ("SHIPPING".equals(currentStatus)) {
	            if ("COMPLETED".equals(newStatus))
	                isValidFlow = true;
	        }

	        if (currentStatus != null && currentStatus.equals(newStatus)) {
	            return ResponseEntity.badRequest().body("Trạng thái không đổi");
	        }

	        if (isValidFlow) {
	            order.setStatus(newStatus);
	            orderRepo.save(order);
	            return ResponseEntity.ok("Cập nhật thành công");
	        } else {
	            return ResponseEntity.badRequest().body("Sai luồng trạng thái");
	        }
	    }

	    // ================= CUSTOMER =================
	    @GetMapping("/customers")
	    public List<User> getCustomers(@RequestParam(required = false) String keyword) {

	        List<User> customers = userRepo.findAll().stream()
	                .filter(u -> u.getRole() == UserRole.USER)
	                .toList();

	        if (keyword != null && !keyword.trim().isEmpty()) {
	            String kw = keyword.toLowerCase();
	            customers = customers.stream()
	                    .filter(u ->
	                        (u.getName() != null && u.getName().toLowerCase().contains(kw)) ||
	                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)) ||
	                        (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
	                    ).toList();
	        }

	        return customers;
	    }

	    // khóa / mở user
	    @PostMapping("/customers/toggle/{username}")
	    public ResponseEntity<?> toggleCustomer(@PathVariable String username) {

	        User user = userRepo.findByUsername(username);

	        if (user == null) {
	            return ResponseEntity.badRequest().body("Không tìm thấy user");
	        }

	        user.setActive(!user.getActive());
	        userRepo.save(user);

	        return ResponseEntity.ok("Đã cập nhật trạng thái");
	    }
}
