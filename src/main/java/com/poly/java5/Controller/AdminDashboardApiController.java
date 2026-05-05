package com.poly.java5.Controller;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.OrderRepository;
import com.poly.java5.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminDashboardApiController {
	@Autowired private BookRepository bookRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private UserRepository userRepo;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        long totalBooks = bookRepo.count();
        long totalOrders = orderRepo.count();
        long totalUsers = userRepo.count();

        return ResponseEntity.ok(Map.of(
            "totalBooks", totalBooks,
            "totalOrders", totalOrders,
            "totalUsers", totalUsers
        ));
    }
}
