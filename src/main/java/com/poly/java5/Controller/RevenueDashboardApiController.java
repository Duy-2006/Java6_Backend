package com.poly.java5.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class RevenueDashboardApiController {
    @Autowired
    private OrderRepository orderRepo;
    @Autowired 
    private BookRepository bookRepo;

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
        
        // Logs (có thể giữ hoặc xóa)
        System.out.println("todayRevenue=" + todayRevenue + ", todayOrders=" + todayOrders);
        System.out.println("startVN = " + startVN);
        System.out.println("endVN = " + endVN);

        // Lấy top 10 sách bán chạy
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> topBooksPage = bookRepo.findTopSellingBooks(pageable);
        List<Object[]> topBooksResult = topBooksPage.getContent();

        List<TopBookDTO> topBooks = topBooksResult.stream()
            .map(row -> new TopBookDTO(
                (Integer) row[0],          // book.id
                (String) row[1],           // book.title (hoặc tên sách)
                ((Number) row[2]).intValue() // sold quantity
            ))
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