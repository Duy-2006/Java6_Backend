package com.poly.java5.ai.tool;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.UserLibrary;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.OrderRepository;
import com.poly.java5.Repository.UserLibraryRepository;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BookstoreTools {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final ThreadLocal<Integer> currentUserId = new ThreadLocal<>();

    public BookstoreTools(BookRepository bookRepository, 
                          OrderRepository orderRepository,
                          UserLibraryRepository userLibraryRepository) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.userLibraryRepository = userLibraryRepository;
    }

    public void setCurrentUserId(Integer userId) {
        currentUserId.set(userId);
    }

    public void clearCurrentUserId() {
        currentUserId.remove();
    }

    @Tool("Lấy giá hiện tại, giá giảm (nếu có) và trạng thái còn hàng (inventory) của một quyển sách bằng ID")
    public String getBookRealtimeInfo(Integer bookId) {
        if (bookId == null) return "Không có ID sách.";
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            return "Không tìm thấy sách với ID " + bookId;
        }
        Book book = bookOpt.get();
        if (!book.isAvailable()) {
            return String.format("Title: '%s', Status: OUT_OF_STOCK, ID: %d", book.getTitle(), book.getId());
        }
        return String.format("Title: '%s', Price: %s VND, Stock: %d, ID: %d",
                book.getTitle(),
                new java.text.DecimalFormat("#,###").format(book.getPrice()).replace(",", "."),
                book.getQuantity(),
                book.getId());
    }

    @Tool("Lấy trạng thái đơn hàng của người dùng đang đăng nhập bằng mã đơn hàng (orderId)")
    public String getOrderStatus(Integer orderId) {
        Integer userId = currentUserId.get();
        if (userId == null) {
            return "Vui lòng đăng nhập để kiểm tra trạng thái đơn hàng.";
        }
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return "Không tìm thấy đơn hàng với mã " + orderId;
        }
        Order order = orderOpt.get();
        // Giả sử Entity Order có phương thức getUser() hoặc getAccount(), tùy theo DB.
        // Cần kiểm tra xem có lấy ra đúng user không
        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            return "Bạn không có quyền truy cập đơn hàng này hoặc đơn hàng không phải của bạn.";
        }
        return String.format("Đơn hàng %d: Tổng tiền %s, Trạng thái: %s", 
                order.getId(), 
                order.getTotalAmount(), 
                order.getStatus() != null ? order.getStatus() : "Không rõ");
    }
}
