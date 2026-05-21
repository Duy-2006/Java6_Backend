package com.poly.java5.Repository;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;   // ✅ ĐÚNG
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.poly.java5.Entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
	
	// Thêm method phân trang
    Page<Book> findByDeletedFalse(Pageable pageable);
    
    List<Book> findByDeletedFalse();
    
 // Chỉ lấy sách active = true của khuyến mãi 
    List<Book> findAllByActiveTrue();
    
 // Hoặc lấy theo danh sách id và active = true
    List<Book> findAllByIdInAndActiveTrue(List<Integer> ids);
    // lấy sách có active = true của danh sách 
    List<Book> findTop10ByOrderByCreatedDateDesc();
    

    @Query("SELECT b FROM Book b WHERE b.active = true ORDER BY b.createdDate DESC")
    List<Book> findTop10ActiveNewBooks(Pageable pageable);
    
    List<Book> findByQuantityLessThan(int threshold);
    
    long countByActiveTrue();
    
    // ✅ THÊM METHOD NÀY
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND b.deleted = false")
    List<Book> searchByTitle(@Param("keyword") String keyword);
    
    List<Book> findByCategoryIdAndDeletedFalse(Integer categoryId);
    
    List<Book> findByAuthorIdAndDeletedFalse(Integer authorId);
    
    @Query("SELECT b FROM Book b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "(b.author IS NOT NULL AND LOWER(b.author.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "(b.category IS NOT NULL AND LOWER(b.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))")
     List<Book> searchByKeyword(@Param("keyword") String keyword);
    
 // Top sách bán chạy dựa trên order details, trả về Page
    @Query("SELECT b, SUM(od.quantity) as sold FROM OrderDetail od JOIN od.book b WHERE b.deleted = false GROUP BY b ORDER BY sold DESC")
    Page<Object[]> findTopSellingBooks(Pageable pageable);
    
    // lấy danh sách khuyến mãi 
    List<Book> findByCategoryIdIn(List<Integer> categoryIds);
}