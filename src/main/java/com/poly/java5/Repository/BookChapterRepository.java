package com.poly.java5.Repository;
import com.poly.java5.Entity.BookChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookChapterRepository extends JpaRepository<BookChapter, Integer>{
	// Tìm tất cả các chương của một cuốn sách (dựa vào book_id) và sắp xếp tăng dần
    List<BookChapter> findByBookIdOrderByChapterNumberAsc(Integer bookId);
}
