package com.poly.java5.Controller;

import com.poly.java5.Entity.Order;
import com.poly.java5.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminOrderApiController {

    @Autowired private OrderService orderService;

    // GET /api/admin/orders
    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderService.findAll());
    }

    // GET /api/admin/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Integer id) {
        // ✅ dùng Integer vì OrderRepository extends JpaRepository<Order, Integer>
        Order order = orderService.findById(Integer.valueOf(id));
        if (order == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(order);
    }

    // PUT /api/admin/orders/{id}/status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id,
                                          @RequestBody Map<String, String> body) {
        String status = body.get("status");
        orderService.updateStatus(Integer.valueOf(id), status);
        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công"));
    }
}