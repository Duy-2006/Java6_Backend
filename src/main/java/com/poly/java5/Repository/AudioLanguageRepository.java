package com.poly.java5.Repository;
import com.poly.java5.Entity.AudioLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioLanguageRepository extends JpaRepository<AudioLanguage, Integer>{
	// Tìm ngôn ngữ/giọng đọc dựa vào mã code (ví dụ truyền "banmai" vào sẽ tìm được)
    AudioLanguage findByNarratorCode(String narratorCode);
}
