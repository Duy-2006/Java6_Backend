package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.DTO.OrderDTO;
import com.poly.java5.DTO.OrderDetailDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.BookFormat;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.OrderDetail;
import com.poly.java5.Entity.OrderStatus;
import com.poly.java5.Entity.UserLibrary;
import com.poly.java5.Repository.BookFormatRepository;
import com.poly.java5.Repository.OrderRepository;
import com.poly.java5.Repository.UserLibraryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookFormatRepository bookFormatRepository;
    private final UserLibraryRepository userLibraryRepo;

    @PersistenceContext
    private EntityManager em;

    // ─────────────────────────────────────────────
    //  LẤY CHI TIẾT ĐƠN HÀNG THEO ID + USER
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Order findByIdAndUser(Integer id, Integer userId) {
        return em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                "LEFT JOIN FETCH o.orderDetails od " +
                "LEFT JOIN FETCH od.book " +
                "WHERE o.id = :id AND o.user.id = :uid", Order.class)
                .setParameter("id", id)
                .setParameter("uid", userId)
                .getResultStream().findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy đơn hàng ID: " + id + " cho user: " + userId));
    }

    // ─────────────────────────────────────────────
    //  LẤY CHI TIẾT THEO MÃ ĐƠN + USER
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Order findByCodeAndUser(String code, Integer userId) {
        return em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                "LEFT JOIN FETCH o.orderDetails od " +
                "LEFT JOIN FETCH od.book " +
                "WHERE o.orderCode = :code AND o.user.id = :uid", Order.class)
                .setParameter("code", code)
                .setParameter("uid", userId)
                .getResultStream().findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy đơn hàng hoặc không có quyền truy cập"));
    }

    // ─────────────────────────────────────────────
    //  DANH SÁCH ĐƠN HÀNG CỦA USER
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Order> findOrdersByUser(Integer userId, String status) {
        if (status == null || status.isBlank()) {
            return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        }
        return orderRepository.findByUserIdAndStatusOrderByOrderDateDesc(userId, status);
    }

    // ─────────────────────────────────────────────
    //  TÌM THEO ID ĐƠN GIẢN (dành cho admin)
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Order findById(Integer id) {
        return em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                "LEFT JOIN FETCH o.orderDetails od " +
                "LEFT JOIN FETCH od.book " +
                "WHERE o.id = :id", Order.class)
                .setParameter("id", id)
                .getResultStream().findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + id));
    }

    // ─────────────────────────────────────────────
    //  TẤT CẢ ĐƠN HÀNG (dành cho admin)
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                "LEFT JOIN FETCH o.orderDetails od " +
                "LEFT JOIN FETCH od.book " +
                "ORDER BY o.orderDate DESC", Order.class)
                .getResultList();
    }

    // ─────────────────────────────────────────────
    //  HỦY ĐƠN HÀNG (user tự hủy)
    // ─────────────────────────────────────────────
    @Transactional
    public void cancelOrder(Integer orderId, Integer userId, String cancelReason) {
        Order order = em.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) {
            throw new RuntimeException("Không tìm thấy đơn hàng");
        }
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền hủy đơn");
        }
        if (!order.isCancellable() || "PAID".equals(order.getPaymentStatus())) {
            throw new RuntimeException("Đơn hàng không thể hủy");
        }
        for (OrderDetail od : order.getOrderDetails()) {
            Book book = em.find(Book.class, od.getBook().getId(), LockModeType.PESSIMISTIC_WRITE);
            book.setQuantity(book.getQuantity() + od.getQuantity());
        }
        order.setStatus("CANCELLED");
        order.setCancelReason(cancelReason);
        em.merge(order);
    }

    // ─────────────────────────────────────────────
    //  CẬP NHẬT TRẠNG THÁI (dành cho admin - MỤC 4)
    // ─────────────────────────────────────────────
    @Transactional
    public void updateStatus(Integer id, String newStatus, String cancelReason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + id));

        String current = order.getStatus();
        String target = newStatus.toUpperCase().trim();

        // 🛑 MỤC 4: CHẶN ADMIN KHÔNG ĐƯỢC ĐẶT TRẠNG THÁI COMPLETED
        if ("COMPLETED".equals(target)) {
            throw new RuntimeException("Admin không thể chuyển trực tiếp sang trạng thái 'Hoàn thành'. " +
                    "Trạng thái này do Khách hàng bấm xác nhận hoặc hệ thống tự động chuyển sau 3 ngày giao hàng thành công.");
        }

        try {
            OrderStatus.valueOf(target);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }

        List<String> forwardFlow = Arrays.asList("PENDING", "CONFIRMED", "SHIPPING", "DELIVERED");
        boolean isForwardStep = forwardFlow.indexOf(current) + 1 == forwardFlow.indexOf(target);
        boolean isCancel = target.equals("CANCELLED") &&
                (current.equals("PENDING") || current.equals("CONFIRMED"));

        if (!isForwardStep && !isCancel) {
            throw new RuntimeException(String.format(
                    "Không thể chuyển từ %s sang %s. Chu trình hợp lệ: PENDING -> CONFIRMED -> SHIPPING -> DELIVERED.",
                    current, target));
        }

        if (target.equals("CANCELLED")) {
            if (cancelReason == null || cancelReason.trim().isEmpty()) {
                throw new RuntimeException("Vui lòng nhập lý do hủy đơn hàng");
            }
            order.setCancelReason(cancelReason);
        }

        // 🛑 MỤC 4: Ghi nhận thời điểm giao hàng thành công
        if ("DELIVERED".equals(target)) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        order.setStatus(target);
        orderRepository.save(order);
    }

    // ─────────────────────────────────────────────
    //  USER XÁC NHẬN ĐÃ NHẬN HÀNG (MỤC 4)
    // ─────────────────────────────────────────────
    @Transactional
    public void confirmReceived(Integer orderId, Integer userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này");
        }
        
        // 🛑 MỤC 4: Chỉ xác nhận khi Admin đã chuyển sang DELIVERED
        if (!"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể bấm xác nhận khi đơn hàng đã được giao hàng thành công (DELIVERED).");
        }

        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        order.setPaymentStatus("PAID");
        orderRepository.save(order);

        try {
            unlockAudiobooksForOrder(order);
        } catch (Exception e) {
            log.error("Error unlocking audiobooks for order {}: {}", order.getId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  TỰ ĐỘNG HOÀN THÀNH TỪ JOB SCHEDULER (MỤC 4)
    // ─────────────────────────────────────────────
    @Transactional
    public void autoCompleteOrder(Order order) {
        if (!"DELIVERED".equals(order.getStatus())) return;

        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        order.setPaymentStatus("PAID");
        orderRepository.save(order);

        try {
            unlockAudiobooksForOrder(order);
        } catch (Exception e) {
            log.error("Error unlocking audiobooks for order {}: {}", order.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Order> findExpiredDeliveredOrders(LocalDateTime threshold) {
        return orderRepository.findByStatusAndDeliveredAtBefore("DELIVERED", threshold);
    }

    public void unlockAudiobooksForOrder(Order order) {
        if (order == null || order.getUser() == null) return;
        java.util.Collection<OrderDetail> orderDetails = order.getOrderDetails();
        if (orderDetails == null || orderDetails.isEmpty()) {
            orderDetails = em.createQuery("SELECT od FROM OrderDetail od WHERE od.order.id = :oid", OrderDetail.class)
                    .setParameter("oid", order.getId()).getResultList();
        }
        for (OrderDetail od : orderDetails) {
            Book book = od.getBook();
            BookFormat audioVariant = bookFormatRepository.findByBookIdAndFormatType(book.getId(), "AUDIO").orElse(null);
            if (audioVariant != null) {
                if (!userLibraryRepo.existsByUser_IdAndBook_IdAndVariant_FormatType(order.getUser().getId(), book.getId(), "AUDIO")) {
                    UserLibrary lib = UserLibrary.builder()
                            .user(order.getUser())
                            .book(book)
                            .variant(audioVariant)
                            .status("ACTIVE")
                            .purchasedAt(java.time.LocalDateTime.now())
                            .build();
                    userLibraryRepo.save(lib);
                    log.info("Unlocked bonus audiobook {} for user {} upon order completion", book.getId(), order.getUser().getId());
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findByUsername(String username) {
        return orderRepository.findByUserUsernameOrderByOrderDateDesc(username)
                .stream().map(this::convertToOrderDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double sumSpendingByUsername(String username) {
        return orderRepository.sumSpendingByUsername(username);
    }

    private OrderDTO convertToOrderDTO(Order order) {
        List<OrderDetailDTO> detailDTOs = null;
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            detailDTOs = order.getOrderDetails().stream()
                    .map(this::convertToOrderDetailDTO)
                    .collect(Collectors.toList());
        }

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setCustomerAddress(order.getCustomerAddress());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingFee(order.getShippingFee() != null
                ? order.getShippingFee() : BigDecimal.ZERO);
        dto.setDiscountAmount(order.getDiscountAmount() != null
                ? order.getDiscountAmount() : BigDecimal.ZERO);
        dto.setStatus(order.getStatus());
        dto.setOrderDate(order.getOrderDate());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setOrderDetails(detailDTOs);
        return dto;
    }

    private OrderDetailDTO convertToOrderDetailDTO(OrderDetail detail) {
        Book book = detail.getBook();
        return new OrderDetailDTO(
                detail.getId(),
                book != null ? book.getId() : null,
                book != null ? book.getTitle() : null,
                detail.getQuantity(),
                detail.getPrice(),
                book != null ? book.getImageUrl() : null
        );
    }
}