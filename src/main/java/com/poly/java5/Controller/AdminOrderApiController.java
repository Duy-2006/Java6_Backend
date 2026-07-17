package com.poly.java5.Controller;

import com.poly.java5.DTO.OrderDTO;
import com.poly.java5.DTO.OrderDetailDTO;
import com.poly.java5.Entity.Order;
import com.poly.java5.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderApiController {

	 @Autowired private OrderService orderService;

	 private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setCustomerAddress(order.getCustomerAddress());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingFee(order.getShippingFee() != null 
                ? order.getShippingFee() : java.math.BigDecimal.ZERO);
        dto.setDiscountAmount(order.getDiscountAmount() != null 
                ? order.getDiscountAmount() : java.math.BigDecimal.ZERO);
        dto.setStatus(order.getStatus());
        dto.setOrderDate(order.getOrderDate());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setCancelReason(order.getCancelReason());

        if (order.getOrderDetails() != null) {
            List<OrderDetailDTO> detailDTOs = order.getOrderDetails().stream()
                .map(detail -> {
                    OrderDetailDTO d = new OrderDetailDTO();
                    d.setId(detail.getId());
                    d.setBookId(detail.getBook().getId());
                    d.setBookTitle(detail.getBook().getTitle());
                    d.setQuantity(detail.getQuantity());
                    d.setPrice(detail.getPrice());
                    return d;
                }).collect(Collectors.toList());
            dto.setOrderDetails(detailDTOs);
        }
        return dto;
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        List<Order> orders = orderService.findAll();
        List<OrderDTO> dtos = orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Integer id) {
        Order order = orderService.findById(id);
        if (order == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(convertToDTO(order));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id,
                                          @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String cancelReason = body.get("cancelReason");

        try {
            orderService.updateStatus(id, status, cancelReason);
            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}