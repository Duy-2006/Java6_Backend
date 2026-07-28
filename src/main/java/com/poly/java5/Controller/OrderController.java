package com.poly.java5.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
	private final OrderService orderService;

    private OrderDTO toDTO(Order o) {
        OrderDTO dto = new OrderDTO();

        dto.setId(o.getId());
        dto.setOrderCode(o.getOrderCode());
        dto.setStatus(o.getStatus());
        dto.setTotalAmount(o.getTotalAmount());

        dto.setDiscountAmount(o.getDiscountAmount() != null ? o.getDiscountAmount() : java.math.BigDecimal.ZERO);
        dto.setShippingFee(o.getShippingFee() != null ? o.getShippingFee() : java.math.BigDecimal.ZERO);
        dto.setOrderDate(o.getOrderDate());

        if (o.getOrderDetails() != null) {
            List<OrderDetailDTO> details = o.getOrderDetails().stream().map(d -> {
                OrderDetailDTO od = new OrderDetailDTO();
                od.setId(d.getId());
                od.setBookId(d.getBook().getId());
                od.setBookTitle(d.getBook().getTitle());
                od.setQuantity(d.getQuantity());
                od.setPrice(d.getPrice());
                od.setBookImageUrl(d.getBook().getImageUrl());
                return od;
            }).toList();
            dto.setOrderDetails(details);
        }

        return dto;
    }

    @GetMapping
    public List<OrderDTO> getOrders(@RequestParam Integer userId,
                                   @RequestParam(required = false) String status) {
        return orderService.findOrdersByUser(userId, status)
                .stream().map(this::toDTO).toList();  
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderFullDTO> getOrderById(@PathVariable Integer id,
                                                      @RequestParam Integer userId) {
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

            dto.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : java.math.BigDecimal.ZERO);
            dto.setShippingFee(order.getShippingFee() != null ? order.getShippingFee() : java.math.BigDecimal.ZERO);

            List<OrderDetailDTO> details = order.getOrderDetails().stream().map(d -> {
                OrderDetailDTO od = new OrderDetailDTO();
                od.setId(d.getId());
                od.setBookId(d.getBook().getId());
                od.setBookTitle(d.getBook().getTitle());
                od.setQuantity(d.getQuantity());
                od.setPrice(d.getPrice());
                od.setBookImageUrl(d.getBook().getImageUrl());
                return od;
            }).toList();

            dto.setDetails(details);
            dto.setOrderDetails(details);

            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

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
            
            dto.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : java.math.BigDecimal.ZERO);
            dto.setShippingFee(order.getShippingFee() != null ? order.getShippingFee() : java.math.BigDecimal.ZERO);

            List<OrderDetailDTO> details = order.getOrderDetails().stream().map(d -> {
                OrderDetailDTO od = new OrderDetailDTO();
                od.setId(d.getId());
                od.setBookId(d.getBook().getId());
                od.setBookTitle(d.getBook().getTitle());
                od.setQuantity(d.getQuantity());
                od.setPrice(d.getPrice());
                od.setBookImageUrl(d.getBook().getImageUrl());
                return od;
            }).toList();

            dto.setDetails(details);
            dto.setOrderDetails(details);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

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