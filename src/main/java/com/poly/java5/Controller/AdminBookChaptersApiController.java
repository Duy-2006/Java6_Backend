package com.poly.java5.Controller;

import com.poly.java5.DTO.AudioSegmentDTO;
import com.poly.java5.DTO.ChapterDTO;
import com.poly.java5.Entity.AudioBook;
import com.poly.java5.Entity.AudioLanguage;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.BookChapter;
import com.poly.java5.Repository.AudioBookRepository;
import com.poly.java5.Repository.AudioLanguageRepository;
import com.poly.java5.Repository.BookChapterRepository;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Service.CloudinaryAudioService;
import com.poly.java5.Service.TtsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/books")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class AdminBookChaptersApiController {

	private final BookRepository bookRepository;
	private final BookChapterRepository chapterRepository;
	private final AudioBookRepository audioBookRepository;
	private final AudioLanguageRepository languageRepository;
	private final TtsService ttsService;
	private final CloudinaryAudioService cloudinaryAudioService;

	// ===========================
	// INIT DEFAULT LANGUAGES
	// ===========================

	@PostConstruct
	public void initLanguages() {
		if (languageRepository.count() == 0) {
			List.of(AudioLanguage.builder().languageName("Ban Mai (Nữ miền Bắc)").narratorCode("banmai").build(),
					AudioLanguage.builder().languageName("Lê Minh (Nam miền Bắc)").narratorCode("leminh").build(),
					AudioLanguage.builder().languageName("Gia Huy (Nam miền Nam)").narratorCode("giahuy").build(),
					AudioLanguage.builder().languageName("Thu Minh (Nữ miền Nam)").narratorCode("thuminh").build(),
					AudioLanguage.builder().languageName("Ngọc Lâm (Nữ giọng chuẩn)").narratorCode("ngoclam").build(),
					AudioLanguage.builder().languageName("Bảo Tín (Nam đa ngôn ngữ)").narratorCode("baotin").build(),
					AudioLanguage.builder().languageName("Vy Vy (Nữ đa ngôn ngữ)").narratorCode("vyvy").build(),
					AudioLanguage.builder().languageName("Phước Lộc (Nam đa ngôn ngữ)").narratorCode("phuocloc").build())
					.forEach(languageRepository::save);
		}
	}

	// ===========================
	// PRIVATE HELPERS
	// ===========================

	/**
	 * Tìm AudioLanguage theo tên hoặc narratorCode. Nếu không tìm thấy, trả về bản
	 * ghi đầu tiên (mặc định).
	 */
	private AudioLanguage findLanguage(String voice) {
		if (voice == null || voice.isBlank()) {
			return languageRepository.findAll().stream().findFirst().orElse(null);
		}

		String cleanVoice = voice.trim().toLowerCase();

		return languageRepository.findAll().stream().filter(l -> l.getLanguageName().equalsIgnoreCase(voice)
				|| l.getNarratorCode().equalsIgnoreCase(cleanVoice)
				|| l.getLanguageName().toLowerCase().contains(cleanVoice) || cleanVoice.contains(l.getNarratorCode()))
				.findFirst().orElseGet(() -> languageRepository.findAll().stream().findFirst().orElse(null));
	}

	/**
	 * Chuyển BookChapter → ChapterDTO, kèm thông tin audio nếu có.
	 */
	private ChapterDTO convertToDTO(BookChapter chapter) {
		ChapterDTO dto = new ChapterDTO();
		dto.setId(chapter.getId());
		dto.setNumber(chapter.getChapterNumber() != null ? String.format("%02d", chapter.getChapterNumber()) : "00");
		dto.setTitle(chapter.getTitle());
		dto.setTextContent(chapter.getContentText());

		// Lấy toàn bộ danh sách audio của chương theo thứ tự sequenceOrder ASC
		List<AudioBook> audios = audioBookRepository.findByChapterIdOrderBySequenceOrderAsc(chapter.getId().intValue());

		if (audios != null && !audios.isEmpty()) {
			// Map sang List<AudioSegmentDTO> để frontend dùng cho gapless playback
			List<AudioSegmentDTO> segments = audios.stream()
					.map(a -> AudioSegmentDTO.builder()
							.audioUrl(a.getAudioUrl())
							.sequenceOrder(a.getSequenceOrder())
							.durationSeconds(a.getDurationSeconds())
							.build())
					.collect(Collectors.toList());
			dto.setAudioSegments(segments);

			// Tính tổng thời lượng (format mm:ss) của toàn bộ chương
			int totalSeconds = audios.stream()
					.mapToInt(a -> a.getDurationSeconds() != null ? a.getDurationSeconds() : 0)
					.sum();
			dto.setDuration(formatDuration(totalSeconds));

			// Trạng thái dựa trên segment cuối cùng; nếu bất kỳ segment nào đang PROCESSING
			// thì toàn chương cũng đang processing
			boolean anyProcessing = audios.stream()
					.anyMatch(a -> "PROCESSING".equalsIgnoreCase(a.getTtsStatus()));
			if (anyProcessing) {
				dto.setStatus("processing");
			} else {
				dto.setStatus(mapTtsStatus(audios.get(audios.size() - 1).getTtsStatus()));
			}

			// Lấy tên giọng đọc từ segment đầu tiên
			dto.setVoiceModel(
					audios.get(0).getLanguage() != null ? audios.get(0).getLanguage().getLanguageName() : "—");
		} else {
			dto.setAudioSegments(List.of()); // Trả về mảng rỗng thay vì null
			dto.setStatus("pending");
			dto.setDuration("—");
			dto.setVoiceModel("—");
		}

		dto.setSpeed("1.0x");
		return dto;
	}

	private String formatDuration(Integer seconds) {
		if (seconds == null)
			return "—";
		return String.format("%02d:%02d", seconds / 60, seconds % 60);
	}

	private String mapTtsStatus(String dbStatus) {
		if (dbStatus == null)
			return "pending";
		return switch (dbStatus.toUpperCase()) {
		case "SUCCESS" -> "completed";
		case "PROCESSING" -> "processing";
		case "FAILED" -> "failed";
		default -> "pending";
		};
	}

	
	/**
	 * Tạo hoặc lấy AudioBook cho một chapter, rồi kích hoạt TTS bất đồng bộ.
	 */
	private void triggerTts(BookChapter chapter, AudioLanguage language) {
		// 1. Xóa dữ liệu cũ của chương để tránh trùng lặp
		audioBookRepository.deleteByChapterId(chapter.getId().intValue());

		// 2. Gọi service chuyển đổi nhiều đoạn
		ttsService.requestMultiSegmentsTTS(chapter.getContentText(), language.getNarratorCode())
				.collectList()
				.subscribe(
						audioUrls -> {
							for (int i = 0; i < audioUrls.size(); i++) {
								AudioBook newAudioBook = AudioBook.builder()
										.chapter(chapter)
										.language(language)
										.ttsStatus("PROCESSING")
										.sequenceOrder(i + 1) // Gán thứ tự chuẩn
										.build();
								AudioBook saved = audioBookRepository.save(newAudioBook);
								onTtsSuccess(saved, chapter, audioUrls.get(i));
							}
						},
						error -> {
                            // Nếu lỗi ngay từ lúc request Python TTS, tạo 1 bản ghi báo lỗi
							AudioBook newAudioBook = AudioBook.builder()
									.chapter(chapter)
									.language(language)
									.ttsStatus("FAILED")
									.sequenceOrder(1) // Lỗi toàn cục thì đoạn 1 báo lỗi
									.build();
							AudioBook saved = audioBookRepository.save(newAudioBook);
							onTtsError(saved, chapter, error);
						}
				);
	}

	private void onTtsSuccess(AudioBook audioBook, BookChapter chapter, String audioUrl) {
		try {
			// Lấy lại entity từ DB để đảm bảo nó chưa bị xóa (ví dụ: người dùng bấm "Dịch lại" hoặc xóa chương)
			audioBookRepository.findById(audioBook.getId().intValue()).ifPresent(existingAudio -> {
				existingAudio.setAudioUrl(audioUrl); // audioUrl is already the final Cloudinary URL from Python
				existingAudio.setTtsStatus("SUCCESS");
				int wordCount = chapter.getContentText() != null ? chapter.getContentText().split("\\s+").length : 0;
				existingAudio.setDurationSeconds(Math.max(5, wordCount / 2));
				audioBookRepository.save(existingAudio);
				System.out.println("✅ [TTS Python] Hoàn tất cho chương: " + chapter.getTitle() + " - Đoạn " + existingAudio.getSequenceOrder());
				System.out.println("🔗 URL vĩnh viễn: " + audioUrl);
			});
		} catch (Exception e) {
			System.err.println("⚠️ [TTS Python] Bỏ qua cập nhật do dữ liệu đã thay đổi: " + e.getMessage());
		}
	}
	

	private void onTtsError(AudioBook audioBook, BookChapter chapter, Throwable error) {
		try {
			audioBookRepository.findById(audioBook.getId().intValue()).ifPresent(existingAudio -> {
				existingAudio.setTtsStatus("FAILED");
				audioBookRepository.save(existingAudio);
			});
		} catch (Exception e) {
			System.err.println("⚠️ [TTS] Bỏ qua cập nhật lỗi do dữ liệu đã thay đổi: " + e.getMessage());
		} 
        
		String errMsg = error != null ? error.getMessage() : "Không rõ lỗi";
		System.err.println("❌ [TTS] Thất bại cho chương \"" + chapter.getTitle() + "\" (Đoạn thứ: " + audioBook.getSequenceOrder() + ")");
		System.err.println("   ➜ Nguyên nhân: " + errMsg);
		System.err.println("   ➜ Gợi ý: Kiểm tra xem Python TTS Service đã khởi động chưa (port 8000), hoặc xem xét lại nội dung văn bản.");
	}



	// ===========================
	// 1. LẤY DANH SÁCH CHƯƠNG
	// ===========================

	@GetMapping("/{bookId}/chapters")
	public ResponseEntity<List<ChapterDTO>> getChapters(@PathVariable Integer bookId) {
		List<ChapterDTO> dtos = chapterRepository.findByBookIdOrderByChapterNumberAsc(bookId).stream()
				.map(this::convertToDTO).collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	// ===========================
	// 2. TẠO / CẬP NHẬT CHƯƠNG
	// ===========================

	// 2. TẠO CHƯƠNG MỚI (chỉ INSERT, id do DB tự sinh)
	@PostMapping(value = "/{bookId}/chapters", consumes = "multipart/form-data")
	public ResponseEntity<?> createChapter(@PathVariable Integer bookId, @RequestParam("number") String number,
			@RequestParam("title") String title,
			@RequestParam(value = "textContent", required = false) String textContent,
			@RequestParam(value = "textFile", required = false) MultipartFile textFile) {

		Book book = bookRepository.findById(bookId).orElse(null);
		if (book == null)
			return ResponseEntity.notFound().build();

		// Ưu tiên nội dung từ file nếu có
		String content = textContent;
		if (textFile != null && !textFile.isEmpty()) {
			try {
				content = new String(textFile.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
			} catch (Exception e) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Không thể đọc file văn bản: " + e.getMessage()));
			}
		}

		int chapNumParsed;
		try {
			chapNumParsed = Integer.parseInt(number.trim());
		} catch (NumberFormatException e) {
			chapNumParsed = 1;
		}
		final int chapNum = chapNumParsed;

		// LUÔN tạo object mới — KHÔNG tìm lại chapter cũ
		// id = null → JPA sẽ INSERT, DB tự sinh id mới
		BookChapter chapter = BookChapter.builder().chapterNumber(chapNum).title(title.trim()).contentText(content)
				.book(book).build();

		BookChapter saved = chapterRepository.save(chapter);
		return ResponseEntity.ok(convertToDTO(saved));
	}

	// ===========================
	// 3. XÓA CHƯƠNG
	// ===========================

	@DeleteMapping("/{bookId}/chapters/{chapterId}")
	public ResponseEntity<Void> deleteChapter(@PathVariable Integer bookId, @PathVariable Integer chapterId) {

		if (!chapterRepository.existsById(chapterId)) {
			return ResponseEntity.notFound().build();
		}

		// Xóa audio liên quan trước
		List<AudioBook> audios = audioBookRepository.findByChapterId(chapterId);
		if (audios != null && !audios.isEmpty()) {
			audioBookRepository.deleteAll(audios);
		}

		chapterRepository.deleteById(chapterId);
		return ResponseEntity.noContent().build();
	}

	// ===========================
	// 4. TTS MỘT CHƯƠNG
	// ===========================

	@PostMapping("/{bookId}/chapters/{chapterId}/tts")
	public ResponseEntity<?> generateTTS(@PathVariable Integer bookId, @PathVariable Integer chapterId,
			@RequestBody Map<String, String> body) {

		BookChapter chapter = chapterRepository.findById(chapterId).orElse(null);
		if (chapter == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Không tìm thấy chương với ID: " + chapterId));
		}

		if (chapter.getContentText() == null || chapter.getContentText().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Chương này chưa có nội dung văn bản."));
		}

		AudioLanguage language = findLanguage(body.get("voice"));
		if (language == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy giọng đọc phù hợp."));
		}

		triggerTts(chapter, language);

		return ResponseEntity.ok(convertToDTO(chapter));
	}

	@PostMapping("/{bookId}/chapters/{chapterId}/tts/stop")
	public ResponseEntity<?> stopTTS(@PathVariable Integer bookId, @PathVariable Integer chapterId) {
		BookChapter chapter = chapterRepository.findById(chapterId).orElse(null);
		if (chapter == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Không tìm thấy chương với ID: " + chapterId));
		}
		
		// Xóa các AudioBook đang processing/pending
		audioBookRepository.deleteByChapterId(chapterId);

		return ResponseEntity.ok(convertToDTO(chapter));
	}

	// ===========================
	// 5. TTS NHIỀU CHƯƠNG
	// ===========================

	@PostMapping("/{bookId}/chapters/tts-bulk")
	public ResponseEntity<?> generateTTSBulk(@PathVariable Integer bookId, @RequestBody Map<String, String> body) {

		AudioLanguage language = findLanguage(body.get("voice"));
		if (language == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy giọng đọc phù hợp."));
		}

		List<BookChapter> chapters = chapterRepository.findByBookIdOrderByChapterNumberAsc(bookId);

		for (BookChapter chapter : chapters) {
			List<AudioBook> existings = audioBookRepository.findByChapterId(chapter.getId().intValue());

			// Bỏ qua nếu đã SUCCESS hoặc đang PROCESSING (tránh chạy đôi)
			boolean skipChapter = existings != null
					&& existings.stream().anyMatch(a -> "SUCCESS".equalsIgnoreCase(a.getTtsStatus())
							|| "PROCESSING".equalsIgnoreCase(a.getTtsStatus()));

			if (!skipChapter) {
				triggerTts(chapter, language);
			}
		}

		List<ChapterDTO> dtos = chapters.stream().map(this::convertToDTO).collect(Collectors.toList());

		return ResponseEntity.ok(dtos);
	}

    @PostMapping("/{bookId}/chapters/{chapterId}/tts-append")
    public ResponseEntity<?> appendTtsSegment(@PathVariable Integer bookId, @PathVariable Integer chapterId, @RequestBody Map<String, String> body) {
        BookChapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Không tìm thấy chương với ID: " + chapterId));
        }
        String voice = body.get("voice");
        String textSegment = body.get("textSegment");
        if (textSegment == null || textSegment.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "textSegment không được để trống."));
        }
        AudioLanguage language = findLanguage(voice);
        if (language == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy giọng đọc phù hợp."));
        }
        // Determine next sequence order
        List<AudioBook> existing = audioBookRepository.findByChapterIdOrderBySequenceOrderAsc(chapter.getId().intValue());
        int nextSeq = (existing == null || existing.isEmpty()) ? 1 : existing.get(existing.size() - 1).getSequenceOrder() + 1;
        // Request TTS for the single segment
        ttsService.requestMultiSegmentsTTS(textSegment, language.getNarratorCode())
                .subscribe(urls -> {
                    String audioUrl = (urls != null && !urls.isEmpty()) ? urls : null;
                    AudioBook newAudio = AudioBook.builder()
                            .chapter(chapter)
                            .language(language)
                            .ttsStatus("PROCESSING")
                            .sequenceOrder(nextSeq)
                            .build();
                    AudioBook saved = audioBookRepository.save(newAudio);
                    if (audioUrl != null) {
                        onTtsSuccess(saved, chapter, audioUrl);
                    } else {
                        onTtsError(saved, chapter, new RuntimeException("Không nhận được URL audio từ TTS"));
                    }
                }, err -> {
                    AudioBook errorAudio = AudioBook.builder()
                            .chapter(chapter)
                            .language(language)
                            .ttsStatus("FAILED")
                            .sequenceOrder(nextSeq)
                            .build();
                    AudioBook saved = audioBookRepository.save(errorAudio);
                    onTtsError(saved, chapter, err);
                });
        return ResponseEntity.ok(convertToDTO(chapter));
    }

	// ===========================
	// 6. CẬP NHẬT CHƯƠNG (SỬA TEXT)
	// ===========================
	@PutMapping("/{bookId}/chapters/{chapterId}")
	public ResponseEntity<?> updateChapter(@PathVariable Integer bookId, @PathVariable Integer chapterId,
			@RequestBody Map<String, String> body) {
		BookChapter chapter = chapterRepository.findById(chapterId).orElse(null);
		if (chapter == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Không tìm thấy chương với ID: " + chapterId));
		}

		String number = body.get("number");
		String title = body.get("title");
		String textContent = body.get("textContent");

		if (number != null && !number.isBlank()) {
			try {
				chapter.setChapterNumber(Integer.parseInt(number.trim()));
			} catch (NumberFormatException e) {
				return ResponseEntity.badRequest().body(Map.of("error", "Số chương không hợp lệ."));
			}
		}
		if (title != null && !title.isBlank()) {
			chapter.setTitle(title.trim());
		}
		
		if (textContent != null) {
			// Nếu nội dung thay đổi, xóa audio cũ để người dùng dịch lại
			if (!textContent.equals(chapter.getContentText())) {
				chapter.setContentText(textContent);
				List<AudioBook> audios = audioBookRepository.findByChapterId(chapterId);
				if (audios != null && !audios.isEmpty()) {
					audioBookRepository.deleteAll(audios);
				}
			}
		}

		BookChapter saved = chapterRepository.save(chapter);
		return ResponseEntity.ok(convertToDTO(saved));
	}
}