package com.poly.java5.Controller;

import com.poly.java5.DTO.LanguageWithVoicesDTO;
import com.poly.java5.DTO.VoiceDTO;
import com.poly.java5.Entity.TTS_Voice;
import com.poly.java5.Entity.SystemLanguage;
import com.poly.java5.Repository.AudioLanguageRepository;
import com.poly.java5.Repository.SystemLanguageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cung cấp danh sách ngôn ngữ kèm theo danh sách giọng đọc thuộc từng ngôn ngữ.
 * GET /api/admin/languages-with-voices
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class LanguageController {

    private final SystemLanguageRepository systemLanguageRepository;
    private final AudioLanguageRepository audioLanguageRepository;

    /**
     * Seed dữ liệu mặc định cho System_Language nếu bảng còn trống.
     * Chạy một lần khi Spring Boot khởi động.
     */
    @PostConstruct
    public void initSystemLanguages() {
        if (systemLanguageRepository.count() == 0) {
            systemLanguageRepository.saveAll(List.of(
                SystemLanguage.builder().code("vi").name("Tiếng Việt").build(),
                SystemLanguage.builder().code("en").name("English").build()
            ));
        }
    }

    /**
     * Trả về danh sách tất cả ngôn ngữ, mỗi ngôn ngữ kèm
     * danh sách giọng đọc thuộc ngôn ngữ đó.
     *
     * Response format:
     * [
     *   {
     *     "id": 1, "code": "vi", "name": "Tiếng Việt",
     *     "voices": [
     *       { "id": 1, "narratorCode": "banmai", "voiceName": "Ban Mai (Nữ miền Bắc)" },
     *       ...
     *     ]
     *   },
     *   { "id": 2, "code": "en", "name": "English", "voices": [...] }
     * ]
     */
    @GetMapping("/languages-with-voices")
    public ResponseEntity<List<LanguageWithVoicesDTO>> getLanguagesWithVoices() {
        // Lấy tất cả ngôn ngữ
        List<SystemLanguage> languages = systemLanguageRepository.findAll();

        // Lấy tất cả giọng đọc và group theo language_id
        List<TTS_Voice> allVoices = audioLanguageRepository.findAll();
        Map<Long, List<TTS_Voice>> voicesByLanguageId = allVoices.stream()
                .filter(v -> v.getSystemLanguage() != null)
                .collect(Collectors.groupingBy(v -> v.getSystemLanguage().getId()));

        // Xây dựng danh sách response
        List<LanguageWithVoicesDTO> result = languages.stream()
                .map(lang -> {
                    List<VoiceDTO> voices = voicesByLanguageId
                            .getOrDefault(lang.getId(), List.of())
                            .stream()
                            .map(v -> VoiceDTO.builder()
                                    .id(v.getId())
                                    .narratorCode(v.getNarratorCode())
                                    .voiceName(v.getLanguageName())
                                    .build())
                            .collect(Collectors.toList());

                    return LanguageWithVoicesDTO.builder()
                            .id(lang.getId())
                            .code(lang.getCode())
                            .name(lang.getName())
                            .voices(voices)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
