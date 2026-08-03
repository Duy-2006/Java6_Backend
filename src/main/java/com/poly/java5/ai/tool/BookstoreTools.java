package com.poly.java5.ai.tool;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.UserLibrary;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.OrderRepository;
import com.poly.java5.Repository.UserLibraryRepository;
import com.poly.java5.Repository.BookFormatRepository;
import com.poly.java5.Repository.VoucherRepository;
import com.poly.java5.Repository.UserRepository;
import com.poly.java5.Service.PromotionService;
import com.poly.java5.Entity.Voucher;
import com.poly.java5.Entity.Promotion;
import com.poly.java5.Entity.User;
import java.time.LocalDate;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

@Component
public class BookstoreTools {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final BookFormatRepository bookFormatRepository;
    private final PromotionService promotionService;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final ThreadLocal<Integer> currentUserId = new ThreadLocal<>();

    public BookstoreTools(BookRepository bookRepository, 
                          OrderRepository orderRepository,
                          UserLibraryRepository userLibraryRepository,
                          BookFormatRepository bookFormatRepository,
                          PromotionService promotionService,
                          VoucherRepository voucherRepository,
                          UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.userLibraryRepository = userLibraryRepository;
        this.bookFormatRepository = bookFormatRepository;
        this.promotionService = promotionService;
        this.voucherRepository = voucherRepository;
        this.userRepository = userRepository;
    }

    public void setCurrentUserId(Integer userId) {
        currentUserId.set(userId);
    }

    public void clearCurrentUserId() {
        currentUserId.remove();
    }

    private Optional<User> getCurrentUser() {
        Integer userId = currentUserId.get();
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId);
    }

    @Tool("""
Lấy giá hiện tại, giá giảm (nếu có), tồn kho và các định dạng (sách giấy, sách nói, giọng đọc) của một quyển sách bằng ID sách.
Sử dụng khi khách hỏi sách còn hàng không, giá bao nhiêu hoặc có bản audio không.
""")
    @Transactional(readOnly = true)
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
            Set<String> languages = new HashSet<>();
            Set<String> voices = new HashSet<>();
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

    @Tool("""
Tìm kiếm và gợi ý sách (sách giấy, sách nói) theo tên sách hoặc từ khóa.
Trả về danh sách tối đa 5 kết quả đang còn kinh doanh.
""")
    @Transactional(readOnly = true)
    public String searchBooks(String keyword) {
        if (keyword == null || keyword.isBlank()) return "Vui lòng cung cấp từ khóa để tìm sách.";
        List<Book> books = bookRepository.searchByKeyword(keyword);
        if (books.isEmpty()) {
            return "Không tìm thấy sách nào phù hợp với từ khóa: " + keyword;
        }
        List<Book> activeBooks = books.stream()
                .filter(Book::isAvailable)
                .limit(5)
                .collect(Collectors.toList());
        if (activeBooks.isEmpty()) {
            return "Sách có tồn tại nhưng hiện đã ngừng kinh doanh.";
        }
        StringBuilder sb = new StringBuilder("Kết quả tìm kiếm:\n");
        for (Book b : activeBooks) {
            sb.append(String.format("- %s (ID: %d), Giá: %s VND, Tồn kho: %d\n",
                    b.getTitle(), b.getId(),
                    new java.text.DecimalFormat("#,###").format(b.getPrice()).replace(",", "."), b.getQuantity()));
        }
        return sb.toString();
    }

    @Tool("""
Lấy trạng thái đơn hàng của người dùng đang đăng nhập bằng mã đơn hàng (orderId).
Không được sử dụng công cụ này nếu chưa có orderId. Không cho phép tra cứu đơn của người khác.
""")
    @Transactional(readOnly = true)
    public String getOrderStatus(Integer orderId) {
        Optional<User> userOpt = getCurrentUser();
        if (userOpt.isEmpty()) {
            return "AUTH_REQUIRED: Vui lòng đăng nhập để kiểm tra trạng thái đơn hàng.";
        }
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return "Không tìm thấy đơn hàng với mã " + orderId;
        }
        Order order = orderOpt.get();
        if (order.getUser() == null || !order.getUser().getId().equals(userOpt.get().getId())) {
            return "Bạn không có quyền truy cập đơn hàng này hoặc đơn hàng không phải của bạn.";
        }
        return String.format("Đơn hàng %s (ID: %d): Tổng tiền %s VND, Trạng thái: %s, Thanh toán: %s", 
                order.getOrderCode() != null ? order.getOrderCode() : "",
                order.getId(), 
                new java.text.DecimalFormat("#,###").format(order.getTotalAmount()).replace(",", "."), 
                order.getStatus() != null ? order.getStatus() : "Không rõ",
                order.getPaymentStatus() != null ? order.getPaymentStatus() : "Chưa thanh toán");
    }

    @Tool("""
Lấy danh sách tối đa 5 đơn hàng gần nhất của người dùng đang đăng nhập.
Sử dụng khi khách hỏi lịch sử mua hàng hoặc đơn hàng mới nhất.
""")
    @Transactional(readOnly = true)
    public String getMyRecentOrders() {
        Optional<User> userOpt = getCurrentUser();
        if (userOpt.isEmpty()) {
            return "AUTH_REQUIRED: Vui lòng đăng nhập để xem lịch sử mua hàng.";
        }
        List<Order> orders = userOpt.get().getOrders();
        if (orders == null || orders.isEmpty()) {
            return "Bạn chưa có đơn hàng nào.";
        }
        List<Order> recentOrders = orders.stream()
                .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                .limit(5)
                .toList();
        StringBuilder sb = new StringBuilder("Lịch sử đơn hàng gần đây của bạn:\n");
        for (Order o : recentOrders) {
            sb.append(String.format("- Đơn %s: Đặt ngày %s, Tổng tiền: %s VND, Trạng thái: %s\n",
                    o.getOrderCode() != null ? o.getOrderCode() : String.valueOf(o.getId()),
                    o.getOrderDate().toLocalDate().toString(),
                    new java.text.DecimalFormat("#,###").format(o.getTotalAmount()).replace(",", "."),
                    o.getStatus()));
        }
        return sb.toString();
    }

    @Tool("""
Lấy danh sách chương trình khuyến mãi đang diễn ra của cửa hàng.
Sử dụng khi khách hỏi sách nào đang giảm giá, chương trình nào còn hiệu lực, mức giảm giá, thời gian áp dụng hoặc sản phẩm được áp dụng.
""")
    public String getActivePromotions() {
        List<Promotion> allPromos = promotionService.getAll();

        List<Promotion> activePromos = allPromos.stream()
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
                List<String> bookTitles = new java.util.ArrayList<>();
                p.getDetails().forEach(d -> {
                    if (d.getBook() != null) bookTitles.add(d.getBook().getTitle());
                });
                applyTypeStr = "Các sách cụ thể: " + String.join(", ", bookTitles);
            } else if ("CATEGORY".equals(applyTypeStr)) {
                List<String> catNames = new java.util.ArrayList<>();
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

    @Tool("""
Lấy danh sách voucher đang hoạt động và có thể sử dụng.
Sử dụng khi khách hỏi mã giảm giá, điều kiện áp dụng, giá trị đơn tối thiểu, số lượt còn lại hoặc thời gian hết hạn.
""")
    public String getActiveVouchers() {
        List<Voucher> activeVouchers = voucherRepository.findActiveVouchers(LocalDate.now());

        if (activeVouchers.isEmpty()) {
            return "Hiện không có mã giảm giá (voucher) nào đang có hiệu lực.";
        }

        StringBuilder sb = new StringBuilder("Các mã giảm giá (voucher) đang có hiệu lực:\n");
        for (Voucher v : activeVouchers) {
            String discountStr = "PERCENT".equals(v.getDiscountType()) 
                ? String.format("%s%%", v.getDiscountValue()) 
                : String.format("%s VND", new java.text.DecimalFormat("#,###").format(v.getDiscountValue()).replace(",", "."));
            
            String remainingUses = (v.getUsageLimit() != null && v.getUsageLimit() > 0) 
                ? String.format("còn %d lượt", v.getUsageLimit() - (v.getUsedCount() != null ? v.getUsedCount() : 0))
                : "Không giới hạn lượt dùng";
                
            sb.append(String.format("- Mã '%s': Giảm %s (Đơn tối thiểu %s VND, tối đa giảm %s VND, %s)\n", 
                    v.getCode(), 
                    discountStr,
                    new java.text.DecimalFormat("#,###").format(v.getMinOrderValue()).replace(",", "."),
                    v.getMaxDiscount() != null ? new java.text.DecimalFormat("#,###").format(v.getMaxDiscount()).replace(",", ".") : "Không giới hạn",
                    remainingUses));
        }
        return sb.toString();
    }

    @Tool("""
Kiểm tra một mã voucher cụ thể có hợp lệ với người dùng và giá trị đơn hàng hay không.
Trả về nguyên nhân cụ thể nếu voucher không thể sử dụng.
""")
    @Transactional(readOnly = true)
    public String validateVoucherForUser(String code, Double orderAmount) {
        if (code == null || code.isBlank()) return "Vui lòng cung cấp mã voucher.";
        Optional<Voucher> voucherOpt = voucherRepository.findByCode(code.trim());
        if (voucherOpt.isEmpty()) {
            return "Mã voucher không tồn tại.";
        }
        Voucher voucher = voucherOpt.get();
        String status = voucher.getComputedStatus();
        if ("INACTIVE".equals(status)) return "Voucher đang bị vô hiệu hóa.";
        if ("EXHAUSTED".equals(status)) return "Voucher đã hết lượt sử dụng.";
        if ("UPCOMING".equals(status)) return "Voucher chưa tới thời gian sử dụng.";
        if ("EXPIRED".equals(status)) return "Voucher đã hết hạn.";
        
        if (orderAmount != null && orderAmount < voucher.getMinOrderValue()) {
            return String.format("Đơn hàng chưa đạt giá trị tối thiểu. Cần tối thiểu %s VND để dùng voucher này.", 
                new java.text.DecimalFormat("#,###").format(voucher.getMinOrderValue()).replace(",", "."));
        }

        return String.format("Voucher hợp lệ. Đơn hàng của bạn sẽ được giảm %s VND.", 
                new java.text.DecimalFormat("#,###").format(voucher.calculateDiscount(orderAmount != null ? orderAmount : voucher.getMinOrderValue())).replace(",", "."));
    }

    @Tool("""
Lấy thông tin tài khoản, hạng thành viên, tổng chi tiêu, số lượng đơn của người dùng đang đăng nhập.
Không sử dụng cho người dùng chưa đăng nhập.
""")
    @Transactional(readOnly = true)
    public String getUserProfileInfo() {
        Optional<User> userOpt = getCurrentUser();
        if (userOpt.isEmpty()) {
            return "AUTH_REQUIRED: Vui lòng đăng nhập để xem thông tin cá nhân và hạng thành viên.";
        }
        User user = userOpt.get();
        String rank = user.calculateRank();
        
        return String.format("Thông tin tài khoản:\n- Tên: %s\n- Hạng thành viên: %s (Chiết khấu %d%%)\n- Tổng chi tiêu tích lũy: %s VND\n- Tổng số đơn: %d đơn hàng.",
                user.getName(),
                rank,
                user.getDiscountPercent(),
                new java.text.DecimalFormat("#,###").format(user.getLifetimeValue()).replace(",", "."),
                user.getOrders() != null ? user.getOrders().size() : 0);
    }

    @Tool("""
Lấy danh sách sách nói mà người dùng đang đăng nhập đã sở hữu hoặc được mở khóa trong thư viện cá nhân.
""")
    @Transactional(readOnly = true)
    public String getMyAudiobookLibrary() {
        Optional<User> userOpt = getCurrentUser();
        if (userOpt.isEmpty()) {
            return "AUTH_REQUIRED: Vui lòng đăng nhập để xem thư viện sách nói của bạn.";
        }
        List<UserLibrary> library = userLibraryRepository.findByUser_IdAndVariant_FormatType(userOpt.get().getId(), "AUDIO");
        if (library == null || library.isEmpty()) {
            return "Thư viện của bạn hiện chưa có sách nói nào.";
        }
        
        StringBuilder sb = new StringBuilder("Sách nói bạn đã sở hữu:\n");
        for (UserLibrary ul : library) {
            sb.append(String.format("- Sách: '%s' (Trạng thái: %s, Mở khóa ngày: %s)\n",
                    ul.getBook().getTitle(), ul.getStatus(), 
                    ul.getPurchasedAt() != null ? ul.getPurchasedAt().toLocalDate() : "Không rõ"));
        }
        return sb.toString();
    }

    @Tool("""
Lấy thông tin liên hệ chính thức, giờ hoạt động và các kênh hỗ trợ của cửa hàng.
""")
    public String getStoreInformation() {
        return """
            Thông tin liên hệ cửa hàng sách:
            - Tên: Antigravity Bookstore
            - Giờ hoạt động: 8h00 - 22h00 (Tất cả các ngày trong tuần)
            - Hỗ trợ trực tuyến: Qua Chatbot hoặc Email hotro@bookstore.com
            - Số điện thoại hỗ trợ: 1900-xxxx
            """;
    }
}
