package com.poly.java5.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.poly.java5.DTO.UserprofileDTO;
import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.AuthUtil;

import lombok.RequiredArgsConstructor;
import com.poly.java5.Repository.OrderRepository;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.OrderDetail;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
	 @Autowired private UserService userService;
	 @Autowired private OrderRepository orderRepository;

	 private static final String UPLOAD_DIR = "src/main/resources/static/uploads/avatars/";

	    // GET profile - trả về DTO
	    @GetMapping("")
	    public ResponseEntity<?> profile() {
	        try {
	            User user = AuthUtil.getAuthenticatedUser(userService);
	            if (user == null) return ResponseEntity.status(401).body("Unauthorized");

	            // Map entity -> DTO
	            UserprofileDTO dto = new UserprofileDTO();
	            dto.setName(user.getName());
	            dto.setEmail(user.getEmail());
	            dto.setPhone(user.getPhone());
	            dto.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
	            dto.setCustomerRank(user.getCustomerRank());
	            
	            // Calculate lifetimeValue dynamically
	            List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
	            double calculatedLifetime = 0.0;
	            List<Map<String, Object>> recentBooks = new ArrayList<>();
	            
	            for (Order o : orders) {
	                if ("COMPLETED".equals(o.getStatus())) {
	                    calculatedLifetime += (o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0) 
                                            + (o.getShippingFee() != null ? o.getShippingFee().doubleValue() : 0.0);
	                }
	                
	                // Extract books from orders for recent books
	                if (recentBooks.size() < 4 && o.getOrderDetails() != null) {
	                    for (OrderDetail od : o.getOrderDetails()) {
	                        if (recentBooks.size() >= 4) break;
	                        
	                        // Avoid duplicates
	                        boolean exists = recentBooks.stream().anyMatch(b -> b.get("id").equals(od.getBook().getId()));
	                        if (!exists) {
	                            Map<String, Object> bookMap = new HashMap<>();
	                            bookMap.put("id", od.getBook().getId());
	                            bookMap.put("title", od.getBook().getTitle());
	                            bookMap.put("imageUrl", od.getBook().getImageUrl());
	                            
	                            long daysAgo = ChronoUnit.DAYS.between(o.getOrderDate(), LocalDateTime.now());
	                            bookMap.put("purchasedDaysAgo", daysAgo);
	                            
	                            recentBooks.add(bookMap);
	                        }
	                    }
	                }
	            }
	            
	            dto.setLifetimeValue(calculatedLifetime);
	            dto.setDiscountPercent((double) user.getDiscountPercent());
	            dto.setRecentBooks(recentBooks);

	            return ResponseEntity.ok(dto);
	        } catch (Exception e) {
	            return ResponseEntity.status(401).body("Unauthorized");
	        }
	    }

	    // UPDATE profile - nhận DTO, không nhận entity
	    @PostMapping("/update")
	    public ResponseEntity<?> updateProfile(@RequestBody UserprofileDTO request) {
	        try {
	            User user = AuthUtil.getAuthenticatedUser(userService);
	            if (user == null) return ResponseEntity.status(401).body("Unauthorized");

	            // Cập nhật từ DTO
	            user.setName(request.getName());
	            user.setEmail(request.getEmail());
	            user.setPhone(request.getPhone());
	            userService.save(user);

	            return ResponseEntity.ok("Cập nhật thành công");
	        } catch (Exception e) {
	            return ResponseEntity.status(401).body("Unauthorized");
	        }
	    }

	    // UPDATE avatar - tải lên file ảnh đại diện
	    @PostMapping("/update-avatar")
	    public ResponseEntity<?> updateAvatar(@RequestParam("imageFile") MultipartFile imageFile) {
	        try {
	            User user = AuthUtil.getAuthenticatedUser(userService);
	            if (user == null) return ResponseEntity.status(401).body("Unauthorized");

	            if (imageFile != null && !imageFile.isEmpty()) {
	                String filename = saveImage(imageFile);
	                user.setAvatar("/uploads/avatars/" + filename);
	                userService.save(user);
	                return ResponseEntity.ok(Map.of(
	                    "avatar", user.getAvatar(),
	                    "message", "Cập nhật ảnh đại diện thành công"
	                ));
	            } else {
	                return ResponseEntity.badRequest().body("Không tìm thấy file tải lên");
	            }
	        } catch (Exception e) {
	            return ResponseEntity.status(500).body("Lỗi tải ảnh: " + e.getMessage());
	        }
	    }

	    private String saveImage(MultipartFile file) throws IOException {
	        Path uploadPath = Paths.get(UPLOAD_DIR);
	        if (!Files.exists(uploadPath)) {
	            Files.createDirectories(uploadPath);
	        }
	        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
	        Path filePath = uploadPath.resolve(filename);
	        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
	        return filename;
	    }
}
