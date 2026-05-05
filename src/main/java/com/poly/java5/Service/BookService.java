package com.poly.java5.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.Entity.Author;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Category;
import com.poly.java5.Repository.AuthorRepository;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.CategoryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookService {

    @PersistenceContext
    private EntityManager entityManager;

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    // ========== CÁC PHƯƠNG THỨC CŨ (TRẢ VỀ ENTITY) ==========
    public List<Book> getAllBooks() {
        log.info("Lấy tất cả sách...");
        TypedQuery<Book> query = entityManager.createQuery(
                "SELECT b FROM Book b ORDER BY b.id DESC",
                Book.class
        );
        return query.getResultList();
    }

    public Book getBookById(Integer id) {
        log.info("Tìm sách ID: {}", id);
        Book book = entityManager.find(Book.class, id.intValue());
        if (book == null) {
            throw new RuntimeException("Không tìm thấy sách");
        }
        return book;
    }

    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.isBlank()) return new ArrayList<>();
        List<Book> allBooks = bookRepository.findByDeletedFalse();
        String normalizedKeyword = normalize(keyword);
        return allBooks.stream()
                .filter(b -> {
                    String title = normalize(b.getTitle());
                    String author = b.getAuthor() != null ? normalize(b.getAuthor().getName()) : "";
                    String category = b.getCategory() != null ? normalize(b.getCategory().getName()) : "";
                    String isbn = normalize(b.getIsbn());
                    return title.contains(normalizedKeyword)
                            || author.contains(normalizedKeyword)
                            || category.contains(normalizedKeyword)
                            || isbn.contains(normalizedKeyword);
                })
                .collect(Collectors.toList());
    }

    private String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .trim();
    }

    public List<Book> getNewBooks() {
        TypedQuery<Book> query = entityManager.createQuery(
                "SELECT b FROM Book b ORDER BY b.id DESC",
                Book.class
        );
        query.setMaxResults(10);
        return query.getResultList();
    }

    public Book save(Book book) {
        return bookRepository.save(book);
    }

    public void deleteById(Integer id) {
        bookRepository.deleteById(id.intValue());
    }

    public Book findById(Integer id) {
        return bookRepository.findById(id.intValue()).orElse(null);
    }

    public Author findAuthorById(Long authorId) {
        if (authorId == null) return null;
        return authorRepository.findById(authorId).orElse(null);
    }

    public Category findCategoryById(Integer categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId.intValue()).orElse(null);
    }

    public List<Book> findLowStock(int threshold) {
        return bookRepository.findByQuantityLessThan(threshold);
    }

    public long countActive() {
        return bookRepository.countByActiveTrue();
    }

    // ========== CÁC PHƯƠNG THỨC MỚI TRẢ VỀ DTO ==========

    public List<BookDTO> getAllBooksDTO() {
        log.info("Lấy tất cả sách (DTO)");
        return getAllBooks().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookDTO> findLowStockDTO(int threshold) {
        log.info("Lấy sách tồn kho thấp < {} (DTO)", threshold);
        return findLowStock(threshold).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BookDTO getBookByIdDTO(Integer id) {
        log.info("Tìm sách ID: {} (DTO)", id);
        Book book = getBookById(id);
        return convertToDTO(book);
    }

    public List<BookDTO> searchBooksDTO(String keyword) {
        log.info("Tìm kiếm sách với keyword: {} (DTO)", keyword);
        return searchBooks(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookDTO> getNewBooksDTO() {
        log.info("Lấy sách mới (DTO)");
        return getNewBooks().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Chuyển đổi từ Entity Book sang BookDTO
    private BookDTO convertToDTO(Book entity) {
        if (entity == null) return null;

        BookDTO dto = new BookDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setIsbn(entity.getIsbn());
        dto.setPublisher(entity.getPublisher());
        dto.setPrice(entity.getPrice());
        dto.setQuantity(entity.getQuantity());
        dto.setActive(entity.getActive());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        
        // Quan trọng: không bao giờ trả MultipartFile trong JSON response
        dto.setImageFile(null);
        
        // Author info
        if (entity.getAuthor() != null) {
            dto.setAuthorId(entity.getAuthor().getId());
            dto.setAuthorName(entity.getAuthor().getName());
        }
        
        // Category info
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }
        
        return dto;
    }
}