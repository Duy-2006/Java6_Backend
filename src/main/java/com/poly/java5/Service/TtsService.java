package com.poly.java5.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class TtsService {

    private final WebClient webClient;

    @Value("${fpt.ai.tts.api-key}")
    private String apiKey;

    // Thay đổi giới hạn từ 3000 xuống 1000 ký tự để FPT.AI phản hồi nhanh nhất
    private static final int MAX_TEXT_LENGTH = 1000;

    public TtsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Cắt văn bản an toàn không làm đứt câu.
     */
    public List<String> splitTextSafely(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        int start = 0;
        while (start < text.length()) {
            if (text.length() - start <= maxLength) {
                chunks.add(text.substring(start).trim());
                break;
            }

            int endPos = start + maxLength;
            int splitPos = -1;
            for (int i = endPos; i >= start; i--) {
                char c = text.charAt(i);
                if (c == '.' || c == '!' || c == '?') {
                    splitPos = i + 1;
                    break;
                }
            }

            if (splitPos != -1 && splitPos > start) {
                String chunk = text.substring(start, splitPos).trim();
                if (!chunk.isEmpty()) chunks.add(chunk);
                start = splitPos;
            } else {
                int spacePos = text.lastIndexOf(' ', endPos);
                if (spacePos != -1 && spacePos > start) {
                    String chunk = text.substring(start, spacePos).trim();
                    if (!chunk.isEmpty()) chunks.add(chunk);
                    start = spacePos + 1;
                } else {
                    String chunk = text.substring(start, endPos).trim();
                    if (!chunk.isEmpty()) chunks.add(chunk);
                    start = endPos;
                }
            }
        }
        return chunks;
    }

    /**
     * Làm sạch văn bản trước khi gửi lên FPT.AI:
     * - Loại bỏ ký tự điều khiển (ASCII < 32, trừ newline/tab)
     * - Loại bỏ ký tự HTML entity, emoji không hỗ trợ
     * - Chuẩn hóa khoảng trắng thừa
     */
    private String sanitizeText(String raw) {
        if (raw == null) return "";
        // 1. Loại ký tự điều khiển
        String cleaned = raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ");
        // 2. Loại HTML tags
        cleaned = cleaned.replaceAll("<[^>]*>", " ");
        // 3. Giải mã HTML entities phổ biến
        cleaned = cleaned.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                         .replace("&nbsp;", " ").replace("&quot;", "\"");
        // 4. Loại emoji / ký tự Unicode ngoài BMP (Supplementary Multilingual Plane)
        cleaned = cleaned.replaceAll("[\\uD800-\\uDFFF]", "");
        // 5. Chuẩn hóa khoảng trắng
        cleaned = cleaned.replaceAll("[ \\t]+", " ").trim();
        return cleaned;
    }

    /**
     * Gửi văn bản đến FPT.AI TTS hỗ trợ tự động tách nhiều đoạn và trả về Flux các URL file mp3.
     */
    public Flux<String> requestMultiSegmentsTTS(String textContent, String narratorCode) {
        if (textContent == null || textContent.isBlank()) {
            return Flux.error(new IllegalArgumentException(
                    "Nội dung văn bản trống, không thể tạo audio."));
        }

        // Làm sạch văn bản
        String sanitized = sanitizeText(textContent);

        if (sanitized.isBlank()) {
            return Flux.error(new IllegalArgumentException(
                    "Văn bản sau khi làm sạch bị rỗng. Vui lòng kiểm tra lại nội dung chương."));
        }

        // Cắt văn bản thành các đoạn nhỏ an toàn theo cấu hình mới (1000 ký tự)
        List<String> chunks = splitTextSafely(sanitized, MAX_TEXT_LENGTH);

        System.out.printf("📤 [TTS] Chia thành %d đoạn để gửi tới Python TTS (voice=%s)%n", chunks.size(), narratorCode);

        return Flux.fromIterable(chunks)
                // Delay 2.5s between requests to prevent overwhelming the Python TTS server if needed
                .delayElements(Duration.ofMillis(2500))
                .concatMap(chunk -> webClient.post()
                        .uri("http://localhost:8000/api/generate-audio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new PythonTtsRequest(chunk, narratorCode))
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                clientResponse -> clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> Mono.error(new RuntimeException(
                                                "Python TTS HTTP " + clientResponse.statusCode().value() + ": " + body))))
                        .bodyToMono(PythonTtsResponse.class)
                        .flatMap(response -> {
                            System.out.println("📡 [TTS] Python Response: status=" + response.getStatus()
                                    + " | audio_url=" + response.getAudioUrl());

                            if ("success".equalsIgnoreCase(response.getStatus())
                                    && response.getAudioUrl() != null
                                    && !response.getAudioUrl().isBlank()) {
                                return Mono.just(response.getAudioUrl());
                            }

                            String errMsg = response.getMessage() != null ? response.getMessage() : "Lỗi không xác định từ Python TTS";
                            return Mono.error(new RuntimeException("Python TTS Error: " + errMsg));
                        })
                        .doOnError(err -> System.err.println("❌ [TTS] Lỗi đoạn: " + err.getMessage()))
                );
    }

    @Data
    private static class PythonTtsRequest {
        private String text;
        private String voice;

        public PythonTtsRequest(String text, String voice) {
            this.text = text;
            this.voice = voice;
        }
    }

    @Data
    private static class PythonTtsResponse {
        private String status;
        @JsonProperty("audio_url")
        private String audioUrl;
        private String message;
    }
}