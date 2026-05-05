package com.poly.java5.Service;


import com.poly.java5.DTO.AuthorDTO;
import com.poly.java5.Entity.Author;
import com.poly.java5.Repository.AuthorRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;
    
    @PersistenceContext
    private EntityManager entityManager; // Thêm EntityManager

    // Lấy tất cả tác giả
    public List<Author> findAll() {
        return authorRepository.findAll();
    }

    // Tìm theo ID
    public Author findById(Long id) {
        return authorRepository.findById(id).orElse(null);
    }

    // Lưu (thêm mới hoặc cập nhật)
    public Author save(Author author) {
        return authorRepository.save(author);
    }

    // Xóa theo ID
    public void deleteById(Long id) {
        authorRepository.deleteById(id);
    }
    public List<AuthorDTO> findAllWithBookCount() {
        List<Object[]> results = authorRepository.findAllWithBookCount();
        return results.stream().map(row -> {
            Long id = (Long) row[0];
            String name = (String) row[1];
            String email = (String) row[2];
            Long bookCount = (Long) row[3];
            return new AuthorDTO(id, name, email, bookCount);
        }).collect(Collectors.toList());
    }
    
 // Thêm method lấy số lượng sách theo author id
    public Long getBookCountByAuthorId(Long id) {
        String jpql = "SELECT COUNT(b) FROM Book b WHERE b.author.id = :id";
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    // Thêm method lấy AuthorDTO kèm bookCount theo id
    public AuthorDTO findByIdWithBookCount(Long id) {
        String jpql = "SELECT a.id, a.name, a.email, COUNT(b) FROM Author a LEFT JOIN a.books b WHERE a.id = :id GROUP BY a.id, a.name, a.email";
        Object[] result = entityManager.createQuery(jpql, Object[].class)
                .setParameter("id", id)
                .getSingleResult();
        if (result == null) return null;
        return new AuthorDTO(
            (Long) result[0],
            (String) result[1],
            (String) result[2],
            (Long) result[3]
        );
    }

	
}