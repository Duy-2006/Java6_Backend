package com.poly.java5.Schedule;

import com.poly.java5.Entity.Order;
import com.poly.java5.Service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCompleteOrderJob {

    private final OrderService orderService;

    // Chạy định kỳ mỗi tiếng 1 lần (3.600.000 ms), sau khi khởi động app 1 phút
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    public void scanAndAutoCompleteDeliveredOrders() {
        log.info("⏰ [Scheduler] Bắt đầu quét đơn hàng DELIVERED quá hạn 3 ngày...");

        LocalDateTime threshold = LocalDateTime.now().minusDays(3);
        List<Order> expiredOrders = orderService.findExpiredDeliveredOrders(threshold);

        if (expiredOrders != null && !expiredOrders.isEmpty()) {
            log.info("👉 Tìm thấy {} đơn hàng 'Giao hàng thành công' quá 3 ngày. Tiến hành tự động hoàn thành...", expiredOrders.size());

            int count = 0;
            for (Order order : expiredOrders) {
                try {
                    orderService.autoCompleteOrder(order);
                    count++;
                    log.info("✅ Đã tự động hoàn thành đơn hàng ID: {}, Mã: {}", order.getId(), order.getOrderCode());
                } catch (Exception e) {
                    log.error("❌ Lỗi khi tự động hoàn thành đơn hàng ID {}: {}", order.getId(), e.getMessage());
                }
            }
            log.info("🏁 Hoàn tất job. Đã chuyển trạng thái COMPLETED cho {}/{} đơn hàng.", count, expiredOrders.size());
        } else {
            log.info("🏁 Không có đơn hàng DELIVERED nào quá hạn 3 ngày.");
        }
    }
}