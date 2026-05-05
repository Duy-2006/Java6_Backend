package com.poly.java5.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.poly.java5.Entity.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
	 // Lấy tất cả tác giả kèm số lượng sách (dùng LEFT JOIN để lấy cả tác giả chưa có sách)
    @Query("SELECT a.id, a.name, a.email, COUNT(b) FROM Author a LEFT JOIN a.books b GROUP BY a.id, a.name, a.email")
    List<Object[]> findAllWithBookCount();
}