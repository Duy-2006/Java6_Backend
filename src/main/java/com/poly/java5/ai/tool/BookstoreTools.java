package com.poly.java5.ai.tool;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.UserLibrary;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.OrderRepository;
import com.poly.java5.Repository.UserLibraryRepository;
import com.poly.java5.Repository.BookFormatRepository;
import com.poly.java5.Repository.VoucherRepository;
import com.poly.java5.Service.PromotionService;
import com.poly.java5.Entity.Voucher;
import com.poly.java5.Entity.Promotion;
import java.time.LocalDate;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BookstoreTools {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final BookFormatRepository bookFormatRepository;
    private final PromotionService promotionService;
    private final VoucherRepository voucherRepository;
    private final ThreadLocal<Integer> currentUserId = new ThreadLocal<>();

    public BookstoreTools(BookRepository bookRepository, 
                          OrderRepository orderRepository,
                          UserLibraryRepository userLibraryRepository,
                          BookFormatRepository bookFormatRepository,
                          PromotionService promotionService,
                          VoucherRepository voucherRepository) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.userLibraryRepository = userLibraryRepository;
        this.bookFormatRepository = bookFormatRepository;
        this.promotionService = promotionService;
        this.voucherRepository = voucherRepository;
    }

    public void setCurrentUserId(Integer userId) {
        currentUserId.set(userId);
    }

    public void clearCurrentUserId() {
        currentUserId.remove();
    }

    @Tool("Lấy giá hiện tại, giá giảm (nếu có) và trạng thái còn hàng (inventory) của một quyển sách bằng ID")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
        
        String formatPrices = String.format("Physical Book Price: %s VND", 
                new java.text.DecimalFormat("#,###").format(book.getPrice()).replace(",", "."));
        
        Optional<com.poly.java5.Entity.BookFormat> audioFormat = bookFormatRepository.findByBookIdAndFormatType(bookId, "AUDIO");
        if (audioFormat.isPresent() && audioFormat.get().getPrice() != null) {
            formatPrices += String.format(", Audiobook Price: %s VND", 
                    new java.text.DecimalFormat("#,###").format(audioFormat.get().getPrice()).replace(",", "."));
            
            int totalChapters = 0;
            java.util.Set<String> languages = new java.util.HashSet<>();
            java.util.Set<String> voices = new java.util.HashSet<>();
            if (book.getChapters() != null) {
                totalChapters = book.getChapters().size();
                for (com.poly.java5.Entity.BookChapter ch : book.getChapters()) {
                    if (ch.getAudioBooks() != null) {
                        for (com.poly.java5.Entity.AudioBook ab : ch.getAudioBooks()) {
                            if (ab.getLanguage() != null && "SUCCESS".equalsIgnoreCase(ab.getTtsStatus())) {
                                voices.add(ab.getLanguage().getLanguageName());
                                if (ab.getLanguage().getSystemLanguage() != null) {
                                    languages.add(ab.getLanguage().getSystemLanguage().getName());
                                }
                            }
                        }
                    }
                }
            }
            if (totalChapters > 0) {
                formatPrices += String.format(" (Có %d chương sách nói. Ngôn ngữ: %s. Giọng đọc: %s)", 
                        totalChapters, 
                        languages.isEmpty() ? "Đang cập nhật" : String.join(", ", languages),
                        voices.isEmpty() ? "Đang cập nhật" : String.join(", ", voices));
            }
        }
        
        return String.format("Title: '%s', %s, Stock: %d, ID: %d",
                book.getTitle(),
                formatPrices,
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String getActivePromotions() {
        java.util.List<Promotion> activePromos = promotionService.getAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getStatus()) && "ACTIVE".equals(p.getComputedStatus()))
                .toList();

        if (activePromos.isEmpty()) {
            return "Hiện không có chương trình khuyến mãi nào đang diễn ra.";
        }

        StringBuilder sb = new StringBuilder("Các chương trình khuyến mãi đang diễn ra:\n");
        for (Promotion p : activePromos) {
            String applyTypeStr = p.getApplyType();
            if ("ALL".equals(applyTypeStr)) {
                applyTypeStr = "Toàn bộ cửa hàng";
            } else if ("BOOK".equals(applyTypeStr)) {
                java.util.List<String> bookTitles = new java.util.ArrayList<>();
                p.getDetails().forEach(d -> {
                    if (d.getBook() != null) bookTitles.add(d.getBook().getTitle());
                });
                applyTypeStr = "Các sách cụ thể: " + String.join(", ", bookTitles);
            } else if ("CATEGORY".equals(applyTypeStr)) {
                java.util.List<String> catNames = new java.util.ArrayList<>();
                p.getDetails().forEach(d -> {
                    if (d.getCategory() != null) catNames.add(d.getCategory().getName());
                });
                applyTypeStr = "Các thể loại cụ thể: " + String.join(", ", catNames);
            }

            sb.append(String.format("- %s: Giảm %s%% (Áp dụng: %s)\n", 
                    p.getName(), p.getDiscountValue(), applyTypeStr));
        }
        return sb.toString();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String getActiveVouchers() {
        System.out.println("======== TOOL CALLED: getActiveVouchers ========");
        java.util.List<Voucher> activeVouchers = voucherRepository.findActiveVouchers(LocalDate.now());
        System.out.println("Found active vouchers: " + activeVouchers.size());

        if (activeVouchers.isEmpty()) {
            return "Hiện không có mã giảm giá (voucher) nào đang có hiệu lực.";
        }

        StringBuilder sb = new StringBuilder("Các mã giảm giá (voucher) đang có hiệu lực:\n");
        for (Voucher v : activeVouchers) {
            String discountStr = "PERCENT".equals(v.getDiscountType()) 
                ? String.format("%s%%", v.getDiscountValue()) 
                : String.format("%s VND", new java.text.DecimalFormat("#,###").format(v.getDiscountValue()).replace(",", "."));
            
            sb.append(String.format("- Mã '%s': Giảm %s (Đơn tối thiểu %s VND, tối đa giảm %s VND, còn %d lượt)\n", 
                    v.getCode(), 
                    discountStr,
                    new java.text.DecimalFormat("#,###").format(v.getMinOrderValue()).replace(",", "."),
                    v.getMaxDiscount() != null ? new java.text.DecimalFormat("#,###").format(v.getMaxDiscount()).replace(",", ".") : "Không giới hạn",
                    v.getUsageLimit() - (v.getUsedCount() != null ? v.getUsedCount() : 0)));
        }
        return sb.toString();
    }
}
