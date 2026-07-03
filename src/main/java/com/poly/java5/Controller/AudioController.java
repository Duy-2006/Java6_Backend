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

// @RestController: Class này là REST API Controller - mọi method đều tự động trả về JSON
// @RequestMapping: Tất cả API trong class này đều có tiền tố "/api/admin/audio"
@RestController
@RequestMapping("/api/admin/audio")
@RequiredArgsConstructor // Lombok: tự tạo constructor cho tất cả field final (thay thế @Autowired cũ)
public class AudioController {

    @Autowired
    private TtsService ttsService;            // Service xử lý logic TTS (cắt text, gọi Python)

    @Autowired
    private BookChapterRepository chapterRepository;    // Repository truy xuất dữ liệu chương sách từ DB

    @Autowired
    private AudioLanguageRepository languageRepository; // Repository truy xuất thông tin giọng đọc từ DB

    @Autowired
    private AudioBookRepository audioBookRepository;    // Repository lưu/xóa bản ghi AudioBook vào DB

    /**
     * API KÍCH HOẠT TẠO AUDIO CHO MỘT CHƯƠNG SÁCH.
     * 
     * Endpoint: POST /api/admin/audio/generate/{chapterId}?languageId=1
     * Ví dụ:    POST /api/admin/audio/generate/5?languageId=2
     * 
     * Cơ chế BẤT ĐỒNG BỘ (Asynchronous):
     *   - Spring Boot nhận request → khởi động quá trình TTS ngầm → TRẢ VỀ NGAY HTTP 200
     *   - Python xử lý có thể mất 30-120 giây, nhưng Next.js KHÔNG phải chờ
     *   - Khi Python xong → callback .subscribe() tự động chạy → lưu kết quả vào DB
     */
    @PostMapping("/generate/{chapterId}")
    public ResponseEntity<?> generateAudioForChapter(
            @PathVariable Integer chapterId,   // Lấy ID chương từ URL path (vd: /generate/5 → chapterId=5)
            @RequestParam Integer languageId) { // Lấy ID giọng đọc từ query param (vd: ?languageId=2)

        // ── BƯỚC 1: KIỂM TRA DỮ LIỆU ĐẦU VÀO TỪ DATABASE ──────────────────────
        // Kiểm tra chương sách có tồn tại trong SQL Server không
        // .orElseThrow(): nếu không tìm thấy → ném exception → Spring tự trả lỗi 500
        BookChapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương sách với ID: " + chapterId));

        // Kiểm tra giọng đọc có tồn tại không (vd: languageId=2 → "banmai" → vi-VN-HoaiMyNeural)
        TTS_Voice language = languageRepository.findById(languageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giọng đọc với ID: " + languageId));

        // ── BƯỚC 2: XÓA AUDIO CŨ CỦA CHƯƠNG NÀY ────────────────────────────────
        // Trước khi tạo audio mới, xóa toàn bộ bản ghi AudioBook cũ của chương này
        // Lý do: Tránh lưu trùng (Admin có thể nhấn "Tạo lại" nhiều lần), dữ liệu luôn mới nhất
        audioBookRepository.deleteByChapterId(chapterId);

        // ── BƯỚC 3: GỌI DỊCH VỤ TTS BẤT ĐỒNG BỘ ───────────────────────────────
        // requestMultiSegmentsTTS() trả về Flux<String> (luồng reactive)
        // .collectList(): thu thập tất cả URL trong Flux thành List<String>
        // .subscribe(onSuccess, onError): đăng ký 2 callback - 1 khi thành công, 1 khi lỗi
        // QUAN TRỌNG: .subscribe() KHÔNG BLOCK - nó đăng ký callback rồi trả về ngay lập tức
        ttsService.requestMultiSegmentsTTS(chapter.getContentText(), language.getNarratorCode())
                .collectList() // Thu thập tất cả URL từ Flux vào một List (chờ Python xử lý xong)
                .subscribe(
                    // CALLBACK THÀNH CÔNG: Chạy khi Python trả về URL thành công
                    audioUrls -> {
                        // Lưu từng URL audio vào bảng AudioBook trong SQL Server
                        for (int i = 0; i < audioUrls.size(); i++) {
                            AudioBook segment = new AudioBook();
                            segment.setChapter(chapter);          // Liên kết với chương sách
                            segment.setLanguage(language);        // Liên kết với giọng đọc đã dùng
                            segment.setTtsStatus("SUCCESS");      // Đánh dấu trạng thái: Thành công
                            segment.setAudioUrl(audioUrls.get(i)); // URL MP3 trên Cloudinary CDN
                            segment.setSequenceOrder(i + 1);      // Thứ tự phát (bắt đầu từ 1)
                            audioBookRepository.save(segment);    // Lưu vào SQL Server
                        }
                        System.out.println("✅ Đã tạo Audio thành công cho Chương: " + chapter.getTitle());
                    },
                    // CALLBACK LỖI: Chạy khi Python báo lỗi hoặc mạng bị ngắt
                    error -> {
                        // Tạo bản ghi "FAILED" để Admin biết chương này bị lỗi khi tạo audio
                        // (Frontend sẽ hiển thị biểu tượng ❌ thay vì ✅)
                        AudioBook failedSegment = new AudioBook();
                        failedSegment.setChapter(chapter);
                        failedSegment.setLanguage(language);
                        failedSegment.setTtsStatus("FAILED");  // Đánh dấu trạng thái: Thất bại
                        failedSegment.setSequenceOrder(1);
                        audioBookRepository.save(failedSegment); // Lưu bản ghi lỗi vào DB
                        System.err.println("❌ Lỗi AI cho Chương " + chapter.getTitle() + ": " + error.getMessage());
                    }
                );

        // ── BƯỚC 4: TRẢ VỀ HTTP 200 NGAY LẬP TỨC CHO NEXT.JS ──────────────────
        // Không đợi Python hoàn thành (có thể mất 30-120 giây)
        // Next.js nhận được 200 OK → hiển thị "Đang xử lý..." và có thể polling để kiểm tra trạng thái
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã gửi yêu cầu sinh audio nhiều đoạn lên FPT.AI. Hệ thống đang chạy ngầm!");
        response.put("chapterId", chapterId); // Trả lại chapterId để Frontend biết đang xử lý cho chương nào

        return ResponseEntity.ok(response); // HTTP 200 OK + JSON body
    }
}
