package com.poly.java5.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.DTO.OrderDTO;
import com.poly.java5.DTO.OrderDetailDTO;
import com.poly.java5.DTO.OrderFullDTO;
import com.poly.java5.Entity.Order;
import com.poly.java5.Service.OrderService;
import com.poly.java5.Entity.User; 
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class OrderController {
	private final OrderService orderService;

    // Thêm method toDTO
    private OrderDTO toDTO(Order o) {
    OrderDTO dto = new OrderDTO();

    dto.setId(o.getId());
    dto.setOrderCode(o.getOrderCode());
    dto.setStatus(o.getStatus());
    dto.setTotalAmount(o.getTotalAmount());

    // Calculate discount amount
    java.math.BigDecimal calculatedTotal = o.calculateTotal();
    java.math.BigDecimal discount =
            calculatedTotal.subtract(o.getTotalAmount());

    if (discount.compareTo(java.math.BigDecimal.ZERO) < 0) {
        discount = java.math.BigDecimal.ZERO;
    }

    dto.setDiscountAmount(discount);
    dto.setOrderDate(o.getOrderDate());

    return dto;
}
    // LẤY DANH SÁCH 
    @GetMapping
    public List<OrderDTO> getOrders(@RequestParam Integer userId,
                                   @RequestParam(required = false) String status) {
        return orderService.findOrdersByUser(userId, status)
                .stream().map(this::toDTO).toList();  
    }

  

    //  CHI TIẾT THEO ID 
    @GetMapping("/{id}")
    public ResponseEntity<OrderFullDTO> getOrderById(@PathVariable Integer id,
                                                      @RequestParam Integer userId) {
        
        System.out.println("=== Get Order by ID ===");
        System.out.println("Order ID: " + id);
        System.out.println("User ID: " + userId);
        
        try {
            Order order = orderService.findByIdAndUser(id, userId);

            OrderFullDTO dto = new OrderFullDTO();
            dto.setId(order.getId());
            dto.setOrderCode(order.getOrderCode());
            dto.setStatus(order.getStatus());
            dto.setCustomerName(order.getCustomerName());
            dto.setCustomerPhone(order.getCustomerPhone());
            dto.setCustomerAddress(order.getCustomerAddress());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setPaymentMethod(order.getPaymentMethod());
            dto.setPaymentStatus(order.getPaymentStatus());
            dto.setOrderDate(order.getOrderDate());
            dto.setCancelReason(order.getCancelReason());

            java.math.BigDecimal calculatedTotal = order.calculateTotal();
            java.math.BigDecimal discount = calculatedTotal.subtract(order.getTotalAmount());
            if (discount.compareTo(java.math.BigDecimal.ZERO) < 0) {
                discount = java.math.BigDecimal.ZERO;
            }
            dto.setDiscountAmount(discount);

            List<OrderDetailDTO> details = order.getOrderDetails().stream().map(d -> {
                OrderDetailDTO od = new OrderDetailDTO();
                od.setId(d.getId());
                od.setBookId(d.getBook().getId());
                od.setBookTitle(d.getBook().getTitle());
                od.setQuantity(d.getQuantity());
                od.setPrice(d.getPrice());
                od.setBookImageUrl(d.getBook().getImageUrl()); // giả sử Book có field imageUrl
                return od;
            }).toList();

            dto.setDetails(details);
            dto.setOrderDetails(details); // Set cả hai tên cho frontend

            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // CHI TIẾT THEO CODE (GIỮ LẠI) 
    @GetMapping("/code/{code}")
    public ResponseEntity<OrderFullDTO> getOrderByCode(@PathVariable String code,
                                                        @RequestParam Integer userId) {
        try {
            Order order = orderService.findByCodeAndUser(code, userId);

            OrderFullDTO dto = new OrderFullDTO();
            dto.setId(order.getId());
            dto.setOrderCode(order.getOrderCode());
            dto.setStatus(order.getStatus());
            dto.setCustomerName(order.getCustomerName());
            dto.setCustomerPhone(order.getCustomerPhone());
            dto.setCustomerAddress(order.getCustomerAddress());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setPaymentMethod(order.getPaymentMethod());
            dto.setPaymentStatus(order.getPaymentStatus());
            dto.setOrderDate(order.getOrderDate());
            dto.setCancelReason(order.getCancelReason());
            
            java.math.BigDecimal calculatedTotal = order.calculateTotal();
            java.math.BigDecimal discount = calculatedTotal.subtract(order.getTotalAmount());
            if (discount.compareTo(java.math.BigDecimal.ZERO) < 0) {
                discount = java.math.BigDecimal.ZERO;
            }
            dto.setDiscountAmount(discount);

            List<OrderDetailDTO> details = order.getOrderDetails().stream().map(d -> {
                OrderDetailDTO od = new OrderDetailDTO();
                od.setId(d.getId());
                od.setBookId(d.getBook().getId());
                od.setBookTitle(d.getBook().getTitle());
                od.setQuantity(d.getQuantity());
                od.setPrice(d.getPrice());
                od.setBookImageUrl(d.getBook().getImageUrl()); // giả sử Book có field imageUrl
                return od;
            }).toList();

            dto.setDetails(details);
            dto.setOrderDetails(details);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    //  HỦY ĐƠN 
 // HỦY ĐƠN 
    @PostMapping("/cancel/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer id,
                                         @RequestParam Integer userId,
                                         @RequestParam String cancelReason) {
        try {
            orderService.cancelOrder(id, userId, cancelReason);
            return ResponseEntity.ok("Đã hủy đơn");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Không thể hủy đơn: " + e.getMessage());
        }
    }
    // user xác nhận giao hàng thành công 
    @PostMapping("/{id}/confirm-received")
    public ResponseEntity<?> confirmReceived(@PathVariable Integer id,
                                             @RequestParam Integer userId) {
        try {
            orderService.confirmReceived(id, userId);
            return ResponseEntity.ok("Đã xác nhận nhận hàng thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
 
}