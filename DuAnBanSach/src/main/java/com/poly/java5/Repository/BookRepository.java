package com.poly.java5.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.poly.java5.Entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    // Hàm tìm kiếm sách chưa bị xóa mềm
    List<Book> findByDeletedFalse();
    
    List<Book> findTop10ByOrderByCreatedDateDesc();
}