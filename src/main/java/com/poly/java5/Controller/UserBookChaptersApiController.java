package com.poly.java5.Controller;

import com.poly.java5.DTO.AudioSegmentDTO;
import com.poly.java5.DTO.PlaybackProgressDTO;
import com.poly.java5.DTO.UserChapterDTO;
import com.poly.java5.Entity.AudioBook;
import com.poly.java5.Entity.AudioPlaybackProgress;
import com.poly.java5.Entity.BookChapter;
import com.poly.java5.Repository.AudioBookRepository;
import com.poly.java5.Repository.AudioPlaybackProgressRepository;
import com.poly.java5.Repository.BookChapterRepository;
import com.poly.java5.Repository.OrderRepository;
import com.poly.java5.Repository.UserLibraryRepository;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/books")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class UserBookChaptersApiController {

    private final BookChapterRepository chapterRepository;
    private final AudioBookRepository audioBookRepository;
    private final OrderRepository orderRepository;
    private final AudioPlaybackProgressRepository progressRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final UserService userService;

    @GetMapping("/{bookId}/chapters")
    public ResponseEntity<?> getUserChapters(@PathVariable Integer bookId) {
        
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);

        boolean hasAccess = false;
        if (userId != null) {
            hasAccess = orderRepository.hasPurchasedBook(userId, bookId);
        }

        List<BookChapter> chapters = chapterRepository.findByBookIdOrderByChapterNumberAsc(bookId);
        
        final boolean finalHasAccess = hasAccess;
        
        List<UserChapterDTO> dtos = java.util.stream.IntStream.range(0, chapters.size())
                .mapToObj(index -> {
                    BookChapter chapter = chapters.get(index);
                    UserChapterDTO dto = convertToUserDTO(chapter);
                    
                    if (!finalHasAccess && index > 0) {
                        dto.setLocked(true);
                        dto.setAudioSegments(List.of());
                        dto.setTextContent("Vui lòng mua sách để xem nội dung chương này.");
                    } else {
                        dto.setLocked(false);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/my-audiobooks")
    public ResponseEntity<?> getMyAudiobooks() {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
        if (userId == null) {
            return ResponseEntity.status(401).body("{\"message\":\"Cần đăng nhập\"}");
        }
        
        List<com.poly.java5.Entity.UserLibrary> libs = userLibraryRepository.findByUser_IdAndVariant_FormatType(userId, "AUDIO");
        List<com.poly.java5.DTO.BookDTO> result = libs.stream().map(lib -> {
            com.poly.java5.Entity.Book b = lib.getBook();
            com.poly.java5.DTO.BookDTO dto = new com.poly.java5.DTO.BookDTO();
            dto.setId(b.getId());
            dto.setTitle(b.getTitle());
            dto.setImageUrl(b.getImageUrl());
            dto.setAuthorName(b.getAuthor() != null ? b.getAuthor().getName() : null);
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ========== PLAYBACK PROGRESS ==========

    /**
     * GET /api/user/books/{bookId}/progress
     * Trả về vị trí nghe gần nhất của user cho cuốn sách này.
     */
    @GetMapping("/{bookId}/progress")
    public ResponseEntity<?> getProgress(@PathVariable Integer bookId) {

        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
        if (userId == null) {
            return ResponseEntity.ok(PlaybackProgressDTO.builder().build());
        }

        return progressRepository.findByUserIdAndBookId(userId, bookId)
                .map(p -> ResponseEntity.ok(PlaybackProgressDTO.builder()
                        .chapterId(p.getChapterId())
                        .segmentIndex(p.getSegmentIndex())
                        .currentTimeSeconds(p.getCurrentTimeSeconds())
                        .playbackRate(p.getPlaybackRate())
                        .build()))
                .orElse(ResponseEntity.ok(PlaybackProgressDTO.builder().build()));
    }

    /**
     * POST /api/user/books/{bookId}/progress
     * Lưu vị trí nghe hiện tại. Frontend gọi mỗi ~10 giây.
     */
    @PostMapping("/{bookId}/progress")
    public ResponseEntity<?> saveProgress(
            @PathVariable Integer bookId,
            @RequestBody PlaybackProgressDTO dto) {

        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
        if (userId == null) {
            return ResponseEntity.status(401).body("{\"message\":\"Cần đăng nhập để lưu tiến trình\"}");
        }

        AudioPlaybackProgress progress = progressRepository.findByUserIdAndBookId(userId, bookId)
                .orElse(AudioPlaybackProgress.builder()
                        .userId(userId)
                        .bookId(bookId)
                        .build());

        progress.setChapterId(dto.getChapterId());
        progress.setSegmentIndex(dto.getSegmentIndex() != null ? dto.getSegmentIndex() : 0);
        progress.setCurrentTimeSeconds(dto.getCurrentTimeSeconds() != null ? dto.getCurrentTimeSeconds() : 0.0);
        progress.setPlaybackRate(dto.getPlaybackRate() != null ? dto.getPlaybackRate() : 1.0);

        progressRepository.save(progress);
        return ResponseEntity.ok("{\"message\":\"OK\"}");
    }

    // ========== PRIVATE HELPERS ==========

    private UserChapterDTO convertToUserDTO(BookChapter chapter) {
        UserChapterDTO dto = new UserChapterDTO();
        dto.setId(chapter.getId());
        dto.setNumber(chapter.getChapterNumber() != null ? String.format("%02d", chapter.getChapterNumber()) : "00");
        dto.setTitle(chapter.getTitle());
        dto.setTextContent(chapter.getContentText());

        List<AudioBook> audios = audioBookRepository.findByChapterIdOrderBySequenceOrderAsc(chapter.getId().intValue());

        if (audios != null && !audios.isEmpty()) {
            List<AudioSegmentDTO> segments = audios.stream()
                    .map(a -> AudioSegmentDTO.builder()
                            .audioUrl(a.getAudioUrl())
                            .sequenceOrder(a.getSequenceOrder())
                            .durationSeconds(a.getDurationSeconds())
                            .build())
                    .collect(Collectors.toList());
            dto.setAudioSegments(segments);

            int totalSeconds = audios.stream()
                    .mapToInt(a -> a.getDurationSeconds() != null ? a.getDurationSeconds() : 0)
                    .sum();
            dto.setDuration(formatDuration(totalSeconds));

            boolean anyProcessing = audios.stream()
                    .anyMatch(a -> "PROCESSING".equalsIgnoreCase(a.getTtsStatus()));
            if (anyProcessing) {
                dto.setStatus("processing");
            } else {
                dto.setStatus(mapTtsStatus(audios.get(audios.size() - 1).getTtsStatus()));
            }

            dto.setVoiceModel(
                    audios.get(0).getLanguage() != null ? audios.get(0).getLanguage().getLanguageName() : "—");
        } else {
            dto.setAudioSegments(List.of());
            dto.setStatus("pending");
            dto.setDuration("—");
            dto.setVoiceModel("—");
        }

        dto.setSpeed("1.0x");
        return dto;
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null) return "—";
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private String mapTtsStatus(String dbStatus) {
        if (dbStatus == null) return "pending";
        return switch (dbStatus.toUpperCase()) {
            case "SUCCESS" -> "completed";
            case "PROCESSING" -> "processing";
            case "FAILED" -> "failed";
            default -> "pending";
        };
    }
}
