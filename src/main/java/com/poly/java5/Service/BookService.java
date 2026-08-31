package com.poly.java5.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.Entity.Author;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Category;
import com.poly.java5.Entity.Publisher;
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

    // ========== CÁC PHƯƠNG THỨC LẤY DỮ LIỆU ==========
    
    public List<Book> getAllBooks() {
        log.info("Lấy tất cả sách (không bao gồm đã xóa)...");
        TypedQuery<Book> query = entityManager.createQuery(
                "SELECT b FROM Book b WHERE b.deleted = false ORDER BY b.id DESC",
                Book.class
        );
        return query.getResultList();
    }

    // Phương thức gốc để Controller gọi (đã kiểm soát xóa mềm)
    public Book getBookById(Integer id) {
        return bookRepository.findById(id.intValue())
                .filter(b -> !Boolean.TRUE.equals(b.getDeleted()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách hoặc đã bị xóa"));
    }

    // ========== BỔ SUNG: PHƯƠNG THỨC FIND_BY_ID CHO CONTROLLER CŨ ==========
    public Book findById(Integer id) {
        // Trả về sách nếu tồn tại và chưa bị xóa, nếu không trả về null
        return bookRepository.findById(id.intValue())
                .filter(b -> !Boolean.TRUE.equals(b.getDeleted()))
                .orElse(null);
    }

    // ========== XỬ LÝ XÓA MỀM (SOFT DELETE) ==========

    public void deleteById(Integer id) {
        log.info("Thực hiện xóa mềm sách ID: {}", id);
        Book book = bookRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + id));
        
        book.setDeleted(true);
        book.setActive(false);
        
        bookRepository.save(book);
        log.info("Đã cập nhật trạng thái xóa mềm thành công cho sách ID: {}", id);
    }

    // ========== CÁC PHƯƠNG THỨC HỖ TRỢ & DTO ==========

    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.isBlank()) return new ArrayList<>();
        List<Book> allBooks = bookRepository.findByDeletedFalse(); 
        return allBooks.stream()
                .filter(b -> {
                    return fuzzyMatch(b.getTitle(), keyword)
                            || (b.getAuthor() != null && fuzzyMatch(b.getAuthor().getName(), keyword))
                            || (b.getCategory() != null && fuzzyMatch(b.getCategory().getName(), keyword))
                            || fuzzyMatch(b.getIsbn(), keyword);
                })
                .filter(Book::getActive)
                .collect(Collectors.toList());
    }

    private boolean fuzzyMatch(String target, String query) {
        if (target == null || query == null || query.isBlank()) return false;
        
        String normTarget = normalize(target);
        String normQuery = normalize(query);
        
        if (normTarget.contains(normQuery)) return true;
        
        String[] queryWords = normQuery.split("\\s+");
        String[] targetWords = normTarget.split("\\s+");
        
        int matchCount = 0;
        for (String qw : queryWords) {
            boolean foundMatch = false;
            for (String tw : targetWords) {
                if (isWordSimilar(qw, tw)) {
                    foundMatch = true;
                    break;
                }
            }
            if (foundMatch) {
                matchCount++;
            }
        }
        return (double) matchCount / queryWords.length >= 0.6;
    }
    
    private boolean isWordSimilar(String w1, String w2) {
        if (w1.equals(w2)) return true;
        if (w2.contains(w1)) return true;
        
        int len1 = w1.length();
        int len2 = w2.length();
        
        if (len1 <= 2 || len2 <= 2) {
            return false;
        }
        
        int maxDistance = (len1 > 4) ? 2 : 1;
        return getLevenshteinDistance(w1, w2) <= maxDistance;
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[] dp = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) {
            dp[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int temp = dp[j];
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[j] = prev;
                } else {
                    dp[j] = Math.min(Math.min(dp[j - 1], dp[j]), prev) + 1;
                }
                prev = temp;
            }
        }
        return dp[s2.length()];
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
                "SELECT b FROM Book b WHERE b.deleted = false ORDER BY b.id DESC",
                Book.class
        );
        query.setMaxResults(10);
        return query.getResultList();
    }

    public Book save(Book book) {
        return bookRepository.save(book);
    }

    public Author findAuthorById(Long authorId) {
        return (authorId == null) ? null : authorRepository.findById(authorId).orElse(null);
    }

    public Category findCategoryById(Integer categoryId) {
        return (categoryId == null) ? null : categoryRepository.findById(categoryId.intValue()).orElse(null);
    }

    // ========== CÁC PHƯƠNG THỨC DTO ==========

    public List<BookDTO> getAllBooksDTO() {
        return getAllBooks().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public BookDTO getBookByIdDTO(Integer id) {
        return convertToDTO(getBookById(id));
    }

    public List<BookDTO> searchBooksDTO(String keyword) {
        return searchBooks(keyword).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

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
        dto.setImageFile(null);
        
        if (entity.getAuthors() != null) {
            dto.setAuthorIds(entity.getAuthors().stream().map(Author::getId).collect(Collectors.toList()));
            dto.setAuthorNames(entity.getAuthors().stream().map(Author::getName).collect(Collectors.toList()));
            if (!entity.getAuthors().isEmpty()) {
                dto.setAuthorId(entity.getAuthors().get(0).getId());
                dto.setAuthorName(entity.getAuthors().get(0).getName());
            }
        } else {
            dto.setAuthorIds(new java.util.ArrayList<>());
            dto.setAuthorNames(new java.util.ArrayList<>());
        }
        if (entity.getPublishers() != null) {
            dto.setPublisherIds(entity.getPublishers().stream().map(Publisher::getId).collect(Collectors.toList()));
            dto.setPublisherNames(entity.getPublishers().stream().map(Publisher::getName).collect(Collectors.toList()));
            dto.setPublisher(entity.getPublishers().stream().map(Publisher::getName).collect(Collectors.joining(", ")));
        } else {
            dto.setPublisherIds(new java.util.ArrayList<>());
            dto.setPublisherNames(new java.util.ArrayList<>());
        }
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }
        return dto;
    }
}