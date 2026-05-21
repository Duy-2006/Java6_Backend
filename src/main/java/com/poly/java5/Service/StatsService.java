package com.poly.java5.Service;
import com.poly.java5.DTO.*;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Repository.OrderDetailRepository;
import com.poly.java5.Repository.OrderRepository;
import com.poly.java5.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {
	private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;

    // Cập nhật getSummary nhận range
public StatsSummaryDTO getSummary(String range) {
    LocalDateTime end = LocalDateTime.now();
    LocalDateTime start = getStartDateByRange(range);
    long daysBetween = ChronoUnit.DAYS.between(start, end);
    LocalDateTime previousStart = start.minusDays(daysBetween);
    LocalDateTime previousEnd = start.minusNanos(1);
    
    BigDecimal currentRevenue = orderRepository.getRevenueBetween(start, end);
    BigDecimal previousRevenue = orderRepository.getRevenueBetween(previousStart, previousEnd);
    long currentOrders = orderRepository.countByStatusAndOrderDateBetween("COMPLETED", start, end);
    long previousOrders = orderRepository.countByStatusAndOrderDateBetween("COMPLETED", previousStart, previousEnd);
    long currentCustomers = userRepository.countNewUsersBetween(start, end);
    long previousCustomers = userRepository.countNewUsersBetween(previousStart, previousEnd);
    
    BigDecimal avgOrderValue = currentOrders == 0 ? BigDecimal.ZERO :
            currentRevenue.divide(BigDecimal.valueOf(currentOrders), 0, RoundingMode.HALF_UP);
    BigDecimal previousAvgOrderValue = previousOrders == 0 ? BigDecimal.ZERO :
            previousRevenue.divide(BigDecimal.valueOf(previousOrders), 0, RoundingMode.HALF_UP);
    
    return new StatsSummaryDTO(
        currentRevenue,
        calcGrowth(previousRevenue, currentRevenue),
        currentOrders,
        calcGrowth((double) previousOrders, (double) currentOrders),
        avgOrderValue,
        calcGrowth(previousAvgOrderValue.doubleValue(), avgOrderValue.doubleValue()),
        currentCustomers,
        calcGrowth((double) previousCustomers, (double) currentCustomers)
    );
}

    // Cập nhật getMonthlyRevenue nhận range (lấy dữ liệu trong khoảng)
public List<MonthlyRevenueDTO> getMonthlyRevenue(String range) {
    LocalDateTime startDate = getStartDateByRange(range);
    List<Object[]> results = orderRepository.getMonthlyRevenueStats(startDate);
    List<MonthlyRevenueDTO> list = new ArrayList<>();
    for (Object[] row : results) {
        int year = ((Number) row[0]).intValue();
        int month = ((Number) row[1]).intValue();
        list.add(new MonthlyRevenueDTO("T" + month, (BigDecimal) row[2], ((Number) row[3]).longValue()));
    }
    list.sort(Comparator.comparingInt(dto -> Integer.parseInt(dto.getMonth().substring(1))));
    return list;
}


   // Cập nhật getRevenueByCategory nhận range
public List<CategoryStatsDTO> getRevenueByCategory(String range) {
    LocalDateTime start = getStartDateByRange(range);
    LocalDateTime end = LocalDateTime.now();
    List<Object[]> results = orderRepository.getRevenueByCategoryBetween(start, end);
    BigDecimal total = results.stream().map(r -> (BigDecimal) r[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
    List<CategoryStatsDTO> list = new ArrayList<>();
    String[] colors = {"hsl(var(--chart-1))", "hsl(var(--chart-2))", "hsl(var(--chart-3))",
                       "hsl(var(--chart-4))", "hsl(var(--chart-5))"};
    int idx = 0;
    for (Object[] row : results) {
        long percent = total.compareTo(BigDecimal.ZERO) == 0 ? 0 :
            ((BigDecimal) row[1]).multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).longValue();
        list.add(new CategoryStatsDTO((String) row[0], percent, colors[idx++ % colors.length]));
    }
    return list;
}

// Cập nhật getTopSellingBooks nhận range
public List<TopBookDTO> getTopSellingBooks(int limit, String range) {
    LocalDateTime start = getStartDateByRange(range);
    LocalDateTime end = LocalDateTime.now();
    return orderDetailRepository.findBestSellerBooksBetween(start, end).stream()
        .limit(limit)
        .map(row -> new TopBookDTO(((Number) row[0]).intValue(), (String) row[1], ((Number) row[2]).intValue()))
        .collect(Collectors.toList());
}


    // Cập nhật getRecentTransactions nhận range
public List<RecentTransactionDTO> getRecentTransactions(int limit, String range) {
    LocalDateTime start = getStartDateByRange(range);
    LocalDateTime end = LocalDateTime.now();
    return orderRepository.findOrdersBetweenOrderByOrderDateDesc(start, end).stream()
        .limit(limit)
        .map(order -> new RecentTransactionDTO(
            order.getOrderCode(),
            order.getCustomerName(),
            order.getTotalAmount(),
            mapStatus(order.getStatus()),
            order.getOrderDate().toLocalDate()
        )).collect(Collectors.toList());
}

    private double calcGrowth(BigDecimal oldVal, BigDecimal newVal) {
        return calcGrowth(oldVal.doubleValue(), newVal.doubleValue());
    }

    private double calcGrowth(Double oldVal, Double newVal) {
        if (oldVal == null || oldVal == 0) return 0.0;
        return (newVal - oldVal) / oldVal * 100;
    }

    private String mapStatus(String status) {
        switch (status) {
            case "COMPLETED": return "Hoàn thành";
            case "CANCELLED": return "Đã hủy";
            default: return "Đang xử lý";
        }
    }
    private LocalDateTime getStartDateByRange(String range) {
        LocalDateTime now = LocalDateTime.now();
        switch (range) {
            case "week": return now.minusWeeks(1).with(LocalTime.MIN);
            case "month": return now.minusMonths(1).with(LocalTime.MIN);
            case "quarter": return now.minusMonths(3).with(LocalTime.MIN);
            case "year": return now.minusYears(1).with(LocalTime.MIN);
            default: return now.minusYears(1).with(LocalTime.MIN);
        }
    }
}
