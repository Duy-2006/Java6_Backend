package com.poly.java5.Controller;
import com.poly.java5.DTO.*;
import com.poly.java5.Service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class StatsController {
	 private final StatsService statsService;

	 @GetMapping
	    public ResponseEntity<Map<String, Object>> getAllStats(
	            @RequestParam(defaultValue = "year") String range) {
	        Map<String, Object> response = new HashMap<>();
	        response.put("summary", statsService.getSummary(range));
	        response.put("monthlyRevenue", statsService.getMonthlyRevenue(range));
	        response.put("categoryStats", statsService.getRevenueByCategory(range));
	        response.put("topBooks", statsService.getTopSellingBooks(5, range));
	        response.put("recentTransactions", statsService.getRecentTransactions(5, range));
	        return ResponseEntity.ok(response);
	    }

	    // Có thể giữ các endpoint riêng lẻ nếu cần (thêm tham số range)
	    @GetMapping("/summary")
	    public ResponseEntity<StatsSummaryDTO> getSummary(@RequestParam(defaultValue = "year") String range) {
	        return ResponseEntity.ok(statsService.getSummary(range));
	    }

	    @GetMapping("/revenue/monthly")
	    public ResponseEntity<List<MonthlyRevenueDTO>> getMonthlyRevenue(
	            @RequestParam(defaultValue = "year") String range) {
	        return ResponseEntity.ok(statsService.getMonthlyRevenue(range));
	    }

	    @GetMapping("/revenue/category")
	    public ResponseEntity<List<CategoryStatsDTO>> getCategoryRevenue(
	            @RequestParam(defaultValue = "year") String range) {
	        return ResponseEntity.ok(statsService.getRevenueByCategory(range));
	    }

	    @GetMapping("/books/top")
	    public ResponseEntity<List<TopBookDTO>> getTopBooks(
	            @RequestParam(defaultValue = "5") int limit,
	            @RequestParam(defaultValue = "year") String range) {
	        return ResponseEntity.ok(statsService.getTopSellingBooks(limit, range));
	    }

	    @GetMapping("/transactions/recent")
	    public ResponseEntity<List<RecentTransactionDTO>> getRecentTransactions(
	            @RequestParam(defaultValue = "5") int limit,
	            @RequestParam(defaultValue = "year") String range) {
	        return ResponseEntity.ok(statsService.getRecentTransactions(limit, range));
	    }
}
