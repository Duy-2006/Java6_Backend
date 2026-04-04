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
@CrossOrigin("*")
@RequiredArgsConstructor
public class OrderController {
	 private final OrderService orderService;

	    // ================== LẤY DANH SÁCH ==================
	    @GetMapping
	    public List<OrderDTO> getOrders(@RequestParam Integer userId,
	                                   @RequestParam(required = false) String status) {

	        return orderService.findOrdersByUser(userId, status)
	                .stream().map(o -> {
	                    OrderDTO dto = new OrderDTO();
	                    dto.setId(o.getId());
	                    dto.setOrderCode(o.getOrderCode());
	                    dto.setStatus(o.getStatus());
	                    dto.setTotalAmount(o.getTotalAmount());
	                    dto.setOrderDate(o.getOrderDate());
	                    return dto;
	                }).toList();
	    }

	    // ================== CHI TIẾT ==================
	    @GetMapping("/{code}")
	    public ResponseEntity<?> getOrderDetail(@PathVariable String code,
	                                            @RequestParam Integer userId) {

	        Order order = orderService.findByCodeAndUser(code, userId);

	        if (order == null) {
	            return ResponseEntity.notFound().build();
	        }

	        OrderFullDTO dto = new OrderFullDTO();
	        dto.setOrderCode(order.getOrderCode());
	        dto.setStatus(order.getStatus());
	        dto.setCustomerName(order.getCustomerName());
	        dto.setCustomerPhone(order.getCustomerPhone());
	        dto.setCustomerAddress(order.getCustomerAddress());
	        dto.setTotalAmount(order.getTotalAmount());

	        // 👉 CHỖ BẠN HỎI (map DTO)
	        List<OrderDetailDTO> details = order.getOrderDetails().stream().map(d -> {
	            OrderDetailDTO od = new OrderDetailDTO();
	            od.setBookTitle(d.getBook().getTitle());
	            od.setQuantity(d.getQuantity());
	            od.setPrice(d.getPrice());
	            return od;
	        }).toList();

	        dto.setDetails(details);

	        return ResponseEntity.ok(dto);
	    }

	    // ================== HỦY ĐƠN ==================
	    @PostMapping("/cancel/{id}")
	    public ResponseEntity<?> cancelOrder(@PathVariable Integer id,
	                                         @RequestParam Integer userId) {
	        try {
	            orderService.cancelOrder(id, userId);
	            return ResponseEntity.ok("Đã hủy đơn");
	        } catch (Exception e) {
	            return ResponseEntity.badRequest().body("Không thể hủy đơn");
	        }
	    }

}
