package com.poly.java5.Controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.poly.java5.Entity.Book;
import com.poly.java5.Service.BannerService;
import com.poly.java5.Service.BookService;
import com.poly.java5.Service.CategoryService;
import com.poly.java5.Service.PromotionService;

import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

   
	private final BookService bookService;
	private final BannerService bannerService; // ✅ inject
	private final CategoryService categoryService;
	private final PromotionService promotionService;

    


	@GetMapping("")
    public String home(Model model) {
        model.addAttribute("banners", bannerService.getActiveBanners());
        
        List<Book> newBooks = bookService.getNewBooks();
        
        // THÊM PHẦN NÀY: Gắn % khuyến mãi cao nhất cho từng sách (nếu có)
        if (newBooks != null && !newBooks.isEmpty()) {
        	newBooks.forEach(book -> {
        	    BigDecimal maxPercent = promotionService.getMaxDiscountPercentage(book.getId());
        	    System.out.println("Book: " + book.getTitle() + " | Discount: " + maxPercent);
        	    book.setTempDiscountPercent(maxPercent);
        	});
        }

        model.addAttribute("newBooks", newBooks);
        model.addAttribute("categories", categoryService.findAll());

        // THÊM DÒNG DEBUG (xem console khi truy cập trang)
        System.out.println("Số sách mới truyền vào view: " + (newBooks != null ? newBooks.size() : 0));
        

        return "home";  // đảm bảo file là src/main/resources/templates/home.html
    }
}
