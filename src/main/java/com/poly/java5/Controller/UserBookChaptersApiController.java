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
import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/books")
@RequiredArgsConstructor
public class UserBookChaptersApiController {

    private final BookChapterRepository chapterRepository;
    private final AudioBookRepository audioBookRepository;
    private final OrderRepository orderRepository;
    private final AudioPlaybackProgressRepository progressRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final UserService userService;

    @GetMapping("/debug-list/{bookId}")
    public ResponseEntity<?> getDebugList(@PathVariable Integer bookId) {
        List<BookChapter> chapters = chapterRepository.findByBookIdOrderByChapterNumberAsc(bookId);
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (BookChapter c : chapters) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("chapterId", c.getId());
            map.put("title", c.getTitle());
            
            List<AudioBook> audios = audioBookRepository.findByChapterIdOrderBySequenceOrderAsc(c.getId().intValue());
            List<java.util.Map<String, Object>> segments = new java.util.ArrayList<>();
            if (audios != null) {
                for (AudioBook a : audios) {
                    java.util.Map<String, Object> sMap = new java.util.HashMap<>();
                    sMap.put("audioId", a.getId());
                    sMap.put("audioUrl", a.getAudioUrl());
                    sMap.put("sequenceOrder", a.getSequenceOrder());
                    sMap.put("voiceName", a.getLanguage() != null ? a.getLanguage().getLanguageName() : null);
                    sMap.put("narratorCode", a.getLanguage() != null ? a.getLanguage().getNarratorCode() : null);
                    sMap.put("languageCode", a.getLanguage() != null && a.getLanguage().getSystemLanguage() != null 
                            ? a.getLanguage().getSystemLanguage().getCode() 
                            : "vi");
                    segments.add(sMap);
                }
            }
            map.put("audioSegments", segments);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{bookId}/chapters")
    public ResponseEntity<?> getUserChapters(@PathVariable Integer bookId) {
        
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);

        boolean hasAccess = false;
        if (userId != null) {
            User user = userService.findById(userId);
            hasAccess = user != null && (user.isAdmin() || orderRepository.hasPurchasedBook(userId, bookId));
        }

        List<BookChapter> chapters = chapterRepository.findByBookIdOrderByChapterNumberAsc(bookId);
        
        final boolean finalHasAccess = hasAccess;
        
        List<UserChapterDTO> dtos = java.util.stream.IntStream.range(0, chapters.size())
                .mapToObj(index -> {
                    BookChapter chapter = chapters.get(index);
                    UserChapterDTO dto = convertToUserDTO(chapter, false, finalHasAccess);
                    
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

    @GetMapping("/{bookId}/chapters/media")
    public ResponseEntity<?> getMediaPlayerChapters(@PathVariable Integer bookId) {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);

        boolean hasAccess = false;
        if (userId != null) {
            User user = userService.findById(userId);
            hasAccess = user != null && (user.isAdmin() || orderRepository.hasPurchasedBook(userId, bookId));
        }

        List<BookChapter> chapters = chapterRepository.findByBookIdOrderByChapterNumberAsc(bookId);
        
        final boolean finalHasAccess = hasAccess;
        
        List<UserChapterDTO> dtos = java.util.stream.IntStream.range(0, chapters.size())
                .mapToObj(index -> {
                    BookChapter chapter = chapters.get(index);
                    UserChapterDTO dto = convertToUserDTO(chapter, true, finalHasAccess);
                    
                    if (!finalHasAccess && index > 0) {
                        dto.setLocked(true);
                        dto.setAudioSegments(List.of());
                        dto.setTextContent("Vui lòng mua sách để nghe chương này.");
                    } else {
                        dto.setLocked(false);
                        if (dto.getAudioSegments() == null || dto.getAudioSegments().isEmpty()) {
                            dto.setTextContent("Chương này đang bị ẩn hoặc chưa có âm thanh.");
                        }
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
            if (b.getAuthors() != null && !b.getAuthors().isEmpty()) {
                dto.setAuthorName(b.getAuthors().stream().map(a -> a.getName()).collect(Collectors.joining(", ")));
            } else {
                dto.setAuthorName(b.getAuthor() != null ? b.getAuthor().getName() : null);
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * API Proxy Audio Stream 
     * Hỗ trợ phát nhạc qua Backend để bảo mật DRM
     */
    @GetMapping("/audio/stream/{audioId}")
    public ResponseEntity<Resource> streamAudio(@PathVariable Integer audioId, HttpServletRequest request) {
        try {
            // 🛡️ BẢO MẬT: Hotlink Protection (Kiểm tra Referer)
            // Ngăn chặn copy link dán trực tiếp vào tab mới hoặc nhúng vào web khác
            String referer = request.getHeader("Referer");
            if (referer == null || (!referer.contains("localhost") && !referer.contains("192.168."))) {
                System.out.println("❌ DRM Blocked: Invalid Referer (" + referer + ")");
                return ResponseEntity.status(403).build();
            }

            AudioBook audio = audioBookRepository.findById(audioId).orElse(null);
            if (audio == null) {
                return ResponseEntity.notFound().build();
            }

            BookChapter chapter = audio.getChapter();
            com.poly.java5.Entity.Book book = chapter.getBook();

            // Kiểm tra xem có phải chương 1 không (Chương 1 luôn free)
            List<BookChapter> chapters = chapterRepository.findByBookIdOrderByChapterNumberAsc(book.getId());
            boolean isFirstChapter = (!chapters.isEmpty() && chapters.get(0).getId().equals(chapter.getId()));

            if (!isFirstChapter) {
                Integer userId = AuthUtil.getAuthenticatedUserId(userService);
                if (userId == null) {
                    return ResponseEntity.status(401).build();
                }
                User user = userService.findById(userId);
                boolean hasAccess = user != null && (user.isAdmin() || orderRepository.hasPurchasedBook(userId, book.getId()));
                if (!hasAccess) {
                    return ResponseEntity.status(403).build();
                }
            }

            // Chuyển tiếp luồng (stream) từ Cloudinary xuống Client
            Resource resource = new UrlResource(audio.getAudioUrl());
            
            // Trả về Resource, Spring Boot sẽ tự động xử lý byte range (Tua tới lui)
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType("audio/mpeg"))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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

    private UserChapterDTO convertToUserDTO(BookChapter chapter, boolean isMediaPlayer, boolean hasAccess) {
        UserChapterDTO dto = new UserChapterDTO();
        dto.setId(chapter.getId());
        dto.setNumber(chapter.getChapterNumber() != null ? String.format("%02d", chapter.getChapterNumber()) : "00");
        dto.setTitle(chapter.getTitle());
        dto.setTextContent(chapter.getContentText());

        List<AudioBook> audios;
        if (isMediaPlayer) {
            if (hasAccess) {
                audios = audioBookRepository.findByChapterIdAndTtsStatusInOrderBySequenceOrderAsc(
                    chapter.getId().intValue(), 
                    java.util.List.of("SUCCESS", "PROCESSING", "INACTIVE")
                );
            } else {
                audios = audioBookRepository.findByChapterIdAndTtsStatusInOrderBySequenceOrderAsc(
                    chapter.getId().intValue(), 
                    java.util.List.of("SUCCESS", "PROCESSING")
                );
            }
        } else {
            if (hasAccess) {
                audios = audioBookRepository.findByChapterIdAndTtsStatusInOrderBySequenceOrderAsc(
                    chapter.getId().intValue(), 
                    java.util.List.of("SUCCESS", "INACTIVE")
                );
            } else {
                audios = audioBookRepository.findByChapterIdAndTtsStatusOrderBySequenceOrderAsc(
                    chapter.getId().intValue(), 
                    "SUCCESS"
                );
            }
        }

        if (audios != null && !audios.isEmpty()) {
            List<AudioSegmentDTO> segments = audios.stream()
                    .map(a -> {
                        String langCode = "vi";
                        if (a.getLanguage() != null && a.getLanguage().getSystemLanguage() != null) {
                            langCode = a.getLanguage().getSystemLanguage().getCode();
                        }
                        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                        String token = request.getHeader("Authorization");
                        if (token != null && token.startsWith("Bearer ")) {
                            token = token.substring(7);
                        } else {
                            Cookie[] cookies = request.getCookies();
                            if (cookies != null) {
                                for (Cookie c : cookies) {
                                    if ("jwt".equals(c.getName())) token = c.getValue();
                                }
                            }
                        }
                        
                        String proxyUrl = "/api/user/books/audio/stream/" + a.getId();
                        if (token != null) {
                            proxyUrl += "?token=" + token;
                        }

                        return AudioSegmentDTO.builder()
                            .audioUrl(proxyUrl)
                            .sequenceOrder(a.getSequenceOrder())
                            .durationSeconds(a.getDurationSeconds())
                            .languageCode(langCode)
                            .languageId(a.getLanguage() != null ? a.getLanguage().getId().intValue() : null)
                            .ttsStatus(a.getTtsStatus())
                            .build();
                    })
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
