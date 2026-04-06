package com.poly.java5.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.poly.java5.Repository.OrderDetailRepository;
import com.poly.java5.Repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    public Map<String, Object> getDashboard() {

        Map<String, Object> data = new HashMap<>();

        // tạo khoảng thời gian hôm nay
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // doanh thu
        data.put("totalRevenue", orderRepository.getTotalRevenue());
        data.put("todayRevenue", orderRepository.getTodayRevenue(startOfDay, endOfDay));

        // đơn hàng
        data.put("todayOrders", orderRepository.countTodayOrders(startOfDay, endOfDay));
        data.put("deliveredOrders", orderRepository.countByStatus("COMPLETED"));
        data.put("cancelledOrders", orderRepository.countCancelledOrders());

        // top sách bán chạy
        List<Object[]> rawTopBooks =
                orderDetailRepository.findBestSellerBooks();

        List<Map<String,Object>> topBooks = rawTopBooks.stream().map(o -> {
            Map<String,Object> book = new HashMap<>();
            book.put("bookId", o[0]);
            book.put("title", o[1]);
            book.put("sold", o[2]);
            return book;
        }).toList();

        data.put("topBooks", topBooks);

        return data;
    }
}
