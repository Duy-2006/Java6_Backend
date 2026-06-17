package com.poly.java5.Controller;
import com.poly.java5.Entity.AudioBook;
import com.poly.java5.Entity.TTS_Voice;
import com.poly.java5.Entity.BookChapter;
import com.poly.java5.Repository.AudioBookRepository;
import com.poly.java5.Repository.AudioLanguageRepository;
import com.poly.java5.Repository.BookChapterRepository;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.CategoryRepository;
import com.poly.java5.Service.PromotionService;
import com.poly.java5.Service.TtsService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audio")

@RequiredArgsConstructor
public class AudioController {
	@Autowired
    private TtsService ttsService;
    
    @Autowired
    private BookChapterRepository chapterRepository;
    
    @Autowired
    private AudioLanguageRepository languageRepository;
    
    @Autowired
    private AudioBookRepository audioBookRepository;

    /**
     * API Gửi yêu cầu chuyển Text thành Audio
     * Endpoint: POST /api/admin/audio/generate/1?languageId=1
     */
    @PostMapping("/generate/{chapterId}")
    public ResponseEntity<?> generateAudioForChapter(
            @PathVariable Integer chapterId, 
            @RequestParam Integer languageId) {

        // 1. Kiểm tra xem Chương sách và Ngôn ngữ có tồn tại trong SQL Server không
        BookChapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương sách với ID: " + chapterId));
                
        TTS_Voice language = languageRepository.findById(languageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giọng đọc với ID: " + languageId));

        // 2. Xóa các đoạn cũ của chương sách trước khi tạo (để tránh trùng lặp)
        audioBookRepository.deleteByChapterId(chapterId);

        // 3. GỌI DỊCH VỤ AI BẤT ĐỒNG BỘ CHO NHIỀU ĐOẠN (Ma thuật nằm ở đây)
        ttsService.requestMultiSegmentsTTS(chapter.getContentText(), language.getNarratorCode())
                .collectList()
                .subscribe(
                    audioUrls -> {
                        // KHI FPT AI THÀNH CÔNG: Sẽ nhảy vào khối này
                        for (int i = 0; i < audioUrls.size(); i++) {
                            AudioBook segment = new AudioBook();
                            segment.setChapter(chapter);
                            segment.setLanguage(language);
                            segment.setTtsStatus("SUCCESS");
                            segment.setAudioUrl(audioUrls.get(i));
                            segment.setSequenceOrder(i + 1);
                            audioBookRepository.save(segment);
                        }
                        System.out.println("✅ Đã tạo Audio thành công cho Chương: " + chapter.getTitle());
                    },
                    error -> {
                        // KHI FPT AI BÁO LỖI: Nhảy vào khối này
                        AudioBook failedSegment = new AudioBook();
                        failedSegment.setChapter(chapter);
                        failedSegment.setLanguage(language);
                        failedSegment.setTtsStatus("FAILED");
                        failedSegment.setSequenceOrder(1);
                        audioBookRepository.save(failedSegment);
                        System.err.println("❌ Lỗi AI cho Chương " + chapter.getTitle() + ": " + error.getMessage());
                    }
                );

        // 4. TRẢ VỀ KẾT QUẢ NGAY LẬP TỨC CHO GIAO DIỆN NEXT.JS
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã gửi yêu cầu sinh audio nhiều đoạn lên FPT.AI. Hệ thống đang chạy ngầm!");
        response.put("chapterId", chapterId);

        return ResponseEntity.ok(response);
    }
}
