package com.poly.java5.Controller;

import com.poly.java5.DTO.AudioSegmentDTO;
import com.poly.java5.DTO.ChapterDTO;
import com.poly.java5.Entity.AudioBook;
import com.poly.java5.Entity.TTS_Voice;
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
			List.of(TTS_Voice.builder().languageName("Ban Mai (Nữ miền Bắc)").narratorCode("banmai").build(),
					TTS_Voice.builder().languageName("Lê Minh (Nam miền Bắc)").narratorCode("leminh").build(),
					TTS_Voice.builder().languageName("Gia Huy (Nam miền Nam)").narratorCode("giahuy").build(),
					TTS_Voice.builder().languageName("Thu Minh (Nữ miền Nam)").narratorCode("thuminh").build(),
					TTS_Voice.builder().languageName("Ngọc Lâm (Nữ giọng chuẩn)").narratorCode("ngoclam").build(),
					TTS_Voice.builder().languageName("Bảo Tín (Nam đa ngôn ngữ)").narratorCode("baotin").build(),
					TTS_Voice.builder().languageName("Vy Vy (Nữ đa ngôn ngữ)").narratorCode("vyvy").build(),
					TTS_Voice.builder().languageName("Phước Lộc (Nam đa ngôn ngữ)").narratorCode("phuocloc").build())
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
	private TTS_Voice findLanguage(String voice) {
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
			System.out.println("ℹ️ [DTO Mapping] Chapter ID: " + chapter.getId() + " has " + audios.size() + " audio segments.");
			List<AudioSegmentDTO> segments = audios.stream()
					.map(a -> {
						String langCode = "vi";
						if (a.getLanguage() != null) {
							if (a.getLanguage().getSystemLanguage() != null) {
								langCode = a.getLanguage().getSystemLanguage().getCode();
							} else {
								System.out.println("⚠️ [DTO Mapping] TTS_Voice " + a.getLanguage().getId() + " has NULL systemLanguage!");
							}
						} else {
							System.out.println("⚠️ [DTO Mapping] AudioBook " + a.getId() + " has NULL language!");
						}
						System.out.println("   ➜ Segment ID: " + a.getId() + " | Voice: " + (a.getLanguage() != null ? a.getLanguage().getNarratorCode() : "null") + " | Mapped langCode: " + langCode);
						return AudioSegmentDTO.builder()
								.audioUrl(a.getAudioUrl())
								.sequenceOrder(a.getSequenceOrder())
								.durationSeconds(a.getDurationSeconds())
								.languageCode(langCode)
								.languageId(a.getLanguage() != null ? a.getLanguage().getId().intValue() : null)
								.ttsStatus(a.getTtsStatus())
								.isOutdated(a.getIsOutdated())
								.build();
					})
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
		case "INACTIVE" -> "completed";
		case "PROCESSING" -> "processing";
		case "FAILED" -> "failed";
		default -> "pending";
		};
	}

	
	/**
	 * Tạo hoặc lấy AudioBook cho một chapter, rồi kích hoạt TTS bất đồng bộ.
	 */
	private void triggerTts(BookChapter chapter, TTS_Voice language) {
		// 1. Chỉ xóa audio của chương này VÀ ngôn ngữ này
		audioBookRepository.deleteByChapterIdAndLanguageId(chapter.getId(), language.getId());

		// 2. Tạo và lưu bản ghi placeholder với trạng thái PROCESSING để UI hiển thị spin loader lập tức
		AudioBook placeholder = AudioBook.builder()
				.chapter(chapter)
				.language(language)
				.ttsStatus("PROCESSING")
				.sequenceOrder(1)
				.isOutdated(false)
				.build();
		final AudioBook savedPlaceholder = audioBookRepository.save(placeholder);

		// 3. Gọi service chuyển đổi nhiều đoạn bất đồng bộ
		ttsService.requestMultiSegmentsTTS(chapter.getContentText(), language.getNarratorCode())
				.collectList()
				.subscribe(
						audioUrls -> {
							try {
								// Xóa bản ghi tạm
								audioBookRepository.delete(savedPlaceholder);
							} catch (Exception e) {
								System.err.println("⚠️ Lỗi xóa placeholder: " + e.getMessage());
							}
							for (int i = 0; i < audioUrls.size(); i++) {
								AudioBook newAudioBook = AudioBook.builder()
										.chapter(chapter)
										.language(language)
										.ttsStatus("PROCESSING")
										.sequenceOrder(i + 1) // Gán thứ tự chuẩn
										.isOutdated(false)
										.build();
								AudioBook saved = audioBookRepository.save(newAudioBook);
								onTtsSuccess(saved, chapter, audioUrls.get(i));
							}
						},
						error -> {
							try {
								// Xóa bản ghi tạm
								audioBookRepository.delete(savedPlaceholder);
							} catch (Exception e) {
								System.err.println("⚠️ Lỗi xóa placeholder: " + e.getMessage());
							}
							AudioBook newAudioBook = AudioBook.builder()
									.chapter(chapter)
									.language(language)
									.ttsStatus("FAILED")
									.sequenceOrder(1) // Lỗi toàn cục thì đoạn 1 báo lỗi
									.isOutdated(false)
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
				existingAudio.setIsOutdated(false);
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

	@DeleteMapping("/{bookId}/chapters/{chapterId}/audio/{langCode}")
	public ResponseEntity<?> deleteChapterAudio(
			@PathVariable Integer bookId, 
			@PathVariable Integer chapterId,
			@PathVariable String langCode) {
		
		BookChapter chapter = chapterRepository.findById(chapterId).orElse(null);
		if (chapter == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		// Find all audiobooks for this chapter
		List<AudioBook> audios = audioBookRepository.findByChapterId(chapterId);
		if (audios != null) {
			List<AudioBook> toToggle = audios.stream().filter(a -> 
				a.getLanguage() != null && 
				a.getLanguage().getSystemLanguage() != null && 
				langCode.equalsIgnoreCase(a.getLanguage().getSystemLanguage().getCode())
			).collect(Collectors.toList());

			if (!toToggle.isEmpty()) {
				audioBookRepository.deleteAll(toToggle);
			}
		}
		
		// Trả về DTO cập nhật mới nhất
		return ResponseEntity.ok(convertToDTO(chapter));
	}

	@PutMapping("/{bookId}/chapters/{chapterId}/audio/{languageId}/toggle")
	public ResponseEntity<?> toggleAudioStatus(
			@PathVariable Integer bookId, 
			@PathVariable Integer chapterId,
			@PathVariable Integer languageId) {
		
		BookChapter chapter = chapterRepository.findById(chapterId).orElse(null);
		if (chapter == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Không tìm thấy chương với ID: " + chapterId));
		}

		List<AudioBook> audios = audioBookRepository.findByChapterId(chapterId);
		if (audios != null) {
			List<AudioBook> targetAudios = audios.stream()
					.filter(a -> a.getLanguage() != null && a.getLanguage().getId().intValue() == languageId)
					.collect(Collectors.toList());

			if (!targetAudios.isEmpty()) {
				String currentStatus = targetAudios.get(0).getTtsStatus();
				String newStatus = "INACTIVE".equalsIgnoreCase(currentStatus) ? "SUCCESS" : "INACTIVE";
				
				for (AudioBook a : targetAudios) {
					a.setTtsStatus(newStatus);
				}
				audioBookRepository.saveAll(targetAudios);
			}
		}

		return ResponseEntity.ok(convertToDTO(chapter));
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

		// Hỗ trợ thêm trường languageCode ("vi", "en", ...) trong body
		// Hiện tại được log lại để backend có thể dùng cho logic dịch thuật sau này.
		String languageCode = body.get("languageCode");
		if (languageCode != null && !languageCode.isBlank()) {
			System.out.println("ℹ️ [TTS] Ngôn ngữ đích được chọn: " + languageCode);
		}

		TTS_Voice language = findLanguage(body.get("voice"));
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

		// Hỗ trợ thêm trường languageCode ("vi", "en", ...) trong body
		String languageCode = body.get("languageCode");
		if (languageCode != null && !languageCode.isBlank()) {
			System.out.println("ℹ️ [TTS-Bulk] Ngôn ngữ đích được chọn: " + languageCode);
		}

		TTS_Voice language = findLanguage(body.get("voice"));
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
        
        TTS_Voice language = null;
        String languageIdStr = body.get("languageId");
        if (languageIdStr != null && !languageIdStr.isBlank()) {
            try {
                language = languageRepository.findById(Integer.parseInt(languageIdStr)).orElse(null);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        if (language == null && voice != null && !voice.isBlank()) {
            language = findLanguage(voice);
        }
        if (language == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy giọng đọc phù hợp."));
        }
        
        // Determine next sequence order
        List<AudioBook> existing = audioBookRepository.findByChapterIdOrderBySequenceOrderAsc(chapter.getId().intValue());
        int nextSeq = (existing == null || existing.isEmpty()) ? 1 : existing.get(existing.size() - 1).getSequenceOrder() + 1;
        // Create and save the new AudioBook segment synchronously first
        AudioBook newAudio = AudioBook.builder()
                .chapter(chapter)
                .language(language)
                .ttsStatus("PROCESSING")
                .sequenceOrder(nextSeq)
                .isOutdated(false)
                .build();
        AudioBook saved = audioBookRepository.save(newAudio);

        // Request TTS for the single segment asynchronously in the background
        final AudioBook finalSaved = saved;
        ttsService.requestMultiSegmentsTTS(textSegment, language.getNarratorCode())
                .subscribe(urls -> {
                    String audioUrl = (urls != null && !urls.isEmpty()) ? urls : null;
                    if (audioUrl != null) {
                        onTtsSuccess(finalSaved, chapter, audioUrl);
                    } else {
                        onTtsError(finalSaved, chapter, new RuntimeException("Không nhận được URL audio từ TTS"));
                    }
                }, err -> {
                    onTtsError(finalSaved, chapter, err);
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
			// Nếu nội dung thay đổi, đánh dấu các file audio cũ là outdated
			if (!textContent.equals(chapter.getContentText())) {
				chapter.setContentText(textContent);
				audioBookRepository.markAllAudiobooksAsOutdated(chapterId);
			}
		}

		BookChapter saved = chapterRepository.save(chapter);
		return ResponseEntity.ok(convertToDTO(saved));
	}

	// =====================================
	// 7. REGENERATE SINGLE LANGUAGE AUDIO
	// =====================================
	@PostMapping("/{bookId}/chapters/{chapterId}/audio/{languageId}/regenerate")
	public ResponseEntity<?> regenerateLanguageAudio(
			@PathVariable Integer bookId, 
			@PathVariable Integer chapterId,
			@PathVariable Integer languageId) {
		
		BookChapter chapter = chapterRepository.findById(chapterId).orElse(null);
		if (chapter == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Không tìm thấy chương với ID: " + chapterId));
		}

		TTS_Voice language = languageRepository.findById(languageId).orElse(null);
		if (language == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy giọng đọc phù hợp."));
		}

		triggerTts(chapter, language);

		return ResponseEntity.ok(convertToDTO(chapter));
	}
}