package com.poly.java5.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.text.Normalizer;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookService {
	@PersistenceContext
    private EntityManager entityManager;
    
    // 1️⃣ LẤY TẤT CẢ SÁCH
    public List<Book> getAllBooks() {
        log.info(" Lấy tất cả sách từ database...");
        
        try {
            TypedQuery<Book> query = entityManager.createQuery(
                "SELECT b FROM Book b ORDER BY b.id DESC", 
                Book.class
            );
            
            List<Book> books = query.getResultList();
            log.info(" Lấy được {} cuốn sách", books.size());
            return books;
            
        } catch (Exception e) {
            log.error(" Lỗi khi lấy sách: ", e);
            throw new RuntimeException("Không thể lấy danh sách sách");
        }
    }
    
    // 2️⃣ LẤY SÁCH THEO ID
    public Book getBookById(Integer id) {
        log.info(" Tìm sách theo ID: {}", id);
        
        try {
            Book book = entityManager.find(Book.class, id);
            
            if (book == null) {
                log.warn("⚠ Không tìm thấy sách với ID: {}", id);
                throw new RuntimeException("Không tìm thấy sách");
            }
            
            log.info(" Tìm thấy sách: {}", book.getTitle());
            return book;
            
        } catch (Exception e) {
            log.error(" Lỗi khi lấy sách theo ID: ", e);
            throw new RuntimeException("Lỗi khi lấy thông tin sách");
        }
    }
    
    
 // 🔍 TÌM SÁCH (Tên | Tác giả | Thể loại | ISBN)
public List<Book> searchBooks(String keyword) {

    if (keyword == null || keyword.isBlank()) {
        return new ArrayList<>();
    }

    String k = normalize(keyword);

    List<Book> books = entityManager
            .createQuery("SELECT b FROM Book b", Book.class)
            .getResultList();

    return books.stream()
            .filter(b -> {

                String title = normalize(b.getTitle());
                String author = b.getAuthor() != null
                        ? normalize(b.getAuthor().getName())
                        : "";
                String category = b.getCategory() != null
                        ? normalize(b.getCategory().getName())
                        : "";
                String isbn = normalize(b.getIsbn());

                return title.contains(k)
                        || author.contains(k)
                        || category.contains(k)
                        || isbn.contains(k);
            })
            .collect(Collectors.toList());
}


    // hàm không cần dấu
    private String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .trim();
    }


    
    // 4️⃣ LẤY SÁCH MỚI NHẤT (10 cuốn)
    public List<Book> getNewBooks() {
        log.info(" Lấy 10 sách mới nhất...");
        
        try {
            TypedQuery<Book> query = entityManager.createQuery(
                "SELECT b FROM Book b ORDER BY b.id DESC", 
                Book.class
            );
            query.setMaxResults(10);
            
            return query.getResultList();
            
        } catch (Exception e) {
            log.error(" Lỗi khi lấy sách mới: ", e);
            throw new RuntimeException("Lỗi khi lấy sách mới");
        }
    }
}
