package com.poly.java5.Repository;
import com.poly.java5.Entity.AudioBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AudioBookRepository extends JpaRepository<AudioBook, Integer>{
	// Lấy tất cả các bản audio (các giọng đọc khác nhau) của 1 chương sách
    List<AudioBook> findByChapterId(Integer chapterId);

    // Lấy bản audio của 1 chương sách nhưng với 1 giọng đọc cụ thể
    AudioBook findByChapterIdAndLanguageId(Integer chapterId, Integer languageId);
    
    List<AudioBook> findByChapterIdOrderBySequenceOrderAsc(Integer chapterId);
    int countByChapterId(Integer chapterId);

    @Transactional
    @Modifying
    void deleteByChapterId(Integer chapterId);
}
