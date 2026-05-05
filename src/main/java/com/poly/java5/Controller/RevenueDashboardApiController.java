package com.poly.java5.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.DTO.TopBookDTO;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.OrderRepository;


@RestController
@RequestMapping("/api/admin/revenue")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class RevenueDashboardApiController {
	 @Autowired
	 private OrderRepository orderRepo;
	    @Autowired 
	    private BookRepository bookRepo; // để lấy top sách

	    @GetMapping("/dashboard")
	    public ResponseEntity<?> getDashboard() {
	        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
	        LocalDate todayVN = LocalDate.now(vnZone);
	        
	        LocalDateTime startVN = todayVN.atStartOfDay();        // 00:00:00 VN
	        LocalDateTime endVN = todayVN.plusDays(1).atStartOfDay(); // 00:00:00 hôm sau
	        
	        BigDecimal totalRevenue = orderRepo.getTotalRevenue();
	        BigDecimal todayRevenue = orderRepo.getTodayRevenue(startVN, endVN);
	        long todayOrders = orderRepo.countTodayOrders(startVN, endVN);
	        long deliveredOrders = orderRepo.countByStatus("COMPLETED");
	        System.out.println("todayRevenue=" + todayRevenue + ", todayOrders=" + todayOrders);
	        System.out.println("startVN = " + startVN);
	        System.out.println("endVN = " + endVN);
	        System.out.println("todayOrders = " + orderRepo.countTodayOrders(startVN, endVN));
	        System.out.println("todayRevenue = " + orderRepo.getTodayRevenue(startVN, endVN));

	        // Top sách bán chạy query từ OrderDetail
	        List<Object[]> topBooksResult = bookRepo.findTopSellingBooks(); 

	        List<TopBookDTO> topBooks = topBooksResult.stream()
	            .map(row -> new TopBookDTO((Integer)row[0], (String)row[1], ((Number)row[2]).intValue()))
	            .collect(Collectors.toList());

	        return ResponseEntity.ok(Map.of(
	            "totalRevenue", totalRevenue,
	            "todayRevenue", todayRevenue,
	            "todayOrders", todayOrders,
	            "deliveredOrders", deliveredOrders,
	            "topBooks", topBooks
	        ));
	    }
}
