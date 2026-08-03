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
@RequiredArgsConstructor
public class StatsController {
	 private final StatsService statsService;

	 @GetMapping
	    public ResponseEntity<Map<String, Object>> getAllStats(
	            @RequestParam(defaultValue = "year") String range,
	            @RequestParam(required = false) String startDate,
	            @RequestParam(required = false) String endDate) {
	        Map<String, Object> response = new HashMap<>();
	        response.put("summary", statsService.getSummary(range, startDate, endDate));
	        response.put("monthlyRevenue", statsService.getMonthlyRevenue(range, startDate, endDate));
	        response.put("categoryStats", statsService.getRevenueByCategory(range, startDate, endDate));
	        response.put("topBooks", statsService.getTopSellingBooks(5, range, startDate, endDate));
	        response.put("recentTransactions", statsService.getRecentTransactions(5, range, startDate, endDate));
	        return ResponseEntity.ok(response);
	    }

	    @GetMapping("/summary")
	    public ResponseEntity<StatsSummaryDTO> getSummary(
	            @RequestParam(defaultValue = "year") String range,
	            @RequestParam(required = false) String startDate,
	            @RequestParam(required = false) String endDate) {
	        return ResponseEntity.ok(statsService.getSummary(range, startDate, endDate));
	    }

	    @GetMapping("/revenue/monthly")
	    public ResponseEntity<List<MonthlyRevenueDTO>> getMonthlyRevenue(
	            @RequestParam(defaultValue = "year") String range,
	            @RequestParam(required = false) String startDate,
	            @RequestParam(required = false) String endDate) {
	        return ResponseEntity.ok(statsService.getMonthlyRevenue(range, startDate, endDate));
	    }

	    @GetMapping("/revenue/category")
	    public ResponseEntity<List<CategoryStatsDTO>> getCategoryRevenue(
	            @RequestParam(defaultValue = "year") String range,
	            @RequestParam(required = false) String startDate,
	            @RequestParam(required = false) String endDate) {
	        return ResponseEntity.ok(statsService.getRevenueByCategory(range, startDate, endDate));
	    }

	    @GetMapping("/books/top")
	    public ResponseEntity<List<TopBookDTO>> getTopBooks(
	            @RequestParam(defaultValue = "5") int limit,
	            @RequestParam(defaultValue = "year") String range,
	            @RequestParam(required = false) String startDate,
	            @RequestParam(required = false) String endDate) {
	        return ResponseEntity.ok(statsService.getTopSellingBooks(limit, range, startDate, endDate));
	    }

	    @GetMapping("/transactions/recent")
	    public ResponseEntity<List<RecentTransactionDTO>> getRecentTransactions(
	            @RequestParam(defaultValue = "5") int limit,
	            @RequestParam(defaultValue = "year") String range,
	            @RequestParam(required = false) String startDate,
	            @RequestParam(required = false) String endDate) {
	        return ResponseEntity.ok(statsService.getRecentTransactions(limit, range, startDate, endDate));
	    }
}
