package com.poly.java5.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.poly.java5.Entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    // ========== PHƯƠNG THỨC DÙNG TRONG BookApiController ==========
    
    // 1. Lấy tất cả sách active (phân trang)
    Page<Book> findByActiveTrue(Pageable pageable);
    
    // 2. Tìm sách theo id và chỉ lấy khi active = true
    Optional<Book> findByIdAndActiveTrue(Integer id);
    
    // Lấy số lượng đã bán thực tế
    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od JOIN od.order o WHERE od.book.id = :bookId AND o.status = 'COMPLETED'")
    Long getSoldCountById(@Param("bookId") Integer bookId);
    
    // Lấy số lượng đã bán hàng loạt
    @Query("SELECT od.book.id, COALESCE(SUM(od.quantity), 0) FROM OrderDetail od JOIN od.order o WHERE od.book.id IN :bookIds AND o.status = 'COMPLETED' GROUP BY od.book.id")
    List<Object[]> getSoldCountByBookIds(@Param("bookIds") List<Integer> bookIds);
    
    // 3. Top sách bán chạy chỉ tính sách active = true, đơn hàng hoàn thành
    @Query("SELECT b, SUM(od.quantity) as sold " +
           "FROM OrderDetail od JOIN od.book b JOIN od.order o " +
           "WHERE b.active = true AND o.status = 'COMPLETED' " +
           "GROUP BY b ORDER BY sold DESC")
    Page<Object[]> findTopSellingBooksActiveOnly(Pageable pageable);
    
    // 4. Lấy danh sách sách nói active và sắp xếp theo lượt bán chạy nhất (phân trang)
    @Query("SELECT b " +
           "FROM BookFormat bf JOIN bf.book b " +
           "LEFT JOIN OrderDetail od ON od.book = b " +
           "LEFT JOIN od.order o ON o = od.order AND o.status = 'COMPLETED' " +
           "WHERE bf.formatType = 'AUDIO' AND bf.active = true AND b.active = true AND b.deleted = false " +
           "GROUP BY b " +
           "ORDER BY COALESCE(SUM(od.quantity), 0) DESC, b.id DESC")
    Page<Book> findAudiobooksActiveOnly(Pageable pageable);
    

    // ========== CÁC PHƯƠNG THỨC KHÁC (GIỮ LẠI ĐỂ TƯƠNG THÍCH VỚI CÁC CONTROLLER KHÁC) ==========
    
    // Dành cho admin (xóa mềm)
    Page<Book> findByDeletedFalse(Pageable pageable);
    List<Book> findByDeletedFalse();
    
    // Lấy tất cả sách active (không phân trang)
    List<Book> findAllByActiveTrue();
    
    // Lấy theo danh sách id và active = true
    List<Book> findAllByIdInAndActiveTrue(List<Integer> ids);
    
    // Top 10 sách mới nhất (dùng để lấy nhanh, không phân trang)
    List<Book> findTop10ByOrderByCreatedDateDesc();
    
    // Sách sắp hết hàng
    List<Book> findByQuantityLessThan(int threshold);
    
    // Đếm sách active
    long countByActiveTrue();
    
    // Tìm kiếm cho admin (deleted = false)
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND b.deleted = false")
    List<Book> searchByTitle(@Param("keyword") String keyword);
    
    // Lọc theo category, author (cho admin)
    List<Book> findByCategoryIdAndDeletedFalse(Integer categoryId);
    
    @Query("SELECT b FROM Book b JOIN b.authors a WHERE a.id = :authorId AND b.deleted = false")
    List<Book> findByAuthorIdAndDeletedFalse(@Param("authorId") Integer authorId);
    
    // Tìm kiếm đa trường cho admin
    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.authors a WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(a IS NOT NULL AND LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(b.category IS NOT NULL AND LOWER(b.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Book> searchByKeyword(@Param("keyword") String keyword);
    
    // Lấy theo danh sách category (không lọc active)
    List<Book> findByCategoryIdIn(List<Integer> categoryIds);
    
    // Top sách bán chạy cũ (dùng deleted = false) – giữ cho admin nếu cần
    @Query("SELECT b, SUM(od.quantity) as sold " +
           "FROM OrderDetail od JOIN od.book b JOIN od.order o " +
           "WHERE b.deleted = false AND o.status = 'COMPLETED' " +
           "GROUP BY b ORDER BY sold DESC")
    Page<Object[]> findTopSellingBooks(Pageable pageable);
}