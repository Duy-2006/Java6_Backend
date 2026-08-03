package com.poly.java5.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class TtsService {

    // WebClient: công cụ gửi HTTP request bất đồng bộ của Spring Boot (thay thế
    // RestTemplate cũ)
    // Bất đồng bộ = gửi request rồi không chờ → server vẫn xử lý việc khác trong
    // lúc đợi Python trả lời
    private final WebClient webClient;

    // Giới hạn 1000 ký tự mỗi đoạn:
    // - edge-tts có thể timeout nếu văn bản quá dài (>2000 ký tự)
    // - 1000 ký tự ≈ 1-2 phút âm thanh, đủ nhỏ để xử lý nhanh và an toàn
    private static final int MAX_TEXT_LENGTH = 1000;

    // Constructor Injection: Spring tự động inject WebClient.Builder vào đây khi
    // khởi động
    public TtsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build(); // Tạo instance WebClient từ builder mặc định
    }

    /**
     * CẮT VĂN BẢN AN TOÀN - Không làm đứt câu giữa chừng.
     * 
     * Thuật toán ưu tiên cắt theo thứ tự:
     * 1. Dấu câu kết thúc câu (. ! ?) → cắt ở đây để câu hoàn chỉnh
     * 2. Khoảng trắng gần nhất → cắt ở đây để không đứt từ
     * 3. Vị trí cứng (maxLength) → cắt bắt buộc nếu không tìm được điểm tốt hơn
     */
    public List<String> splitTextSafely(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty())
            return chunks; // Trả về danh sách rỗng nếu không có văn bản

        int start = 0; // Vị trí bắt đầu của đoạn hiện tại
        while (start < text.length()) {

            // Nếu phần còn lại ngắn hơn giới hạn → thêm toàn bộ phần còn lại vào và dừng
            if (text.length() - start <= maxLength) {
                chunks.add(text.substring(start).trim());
                break;
            }

            int endPos = start + maxLength; // Vị trí tối đa có thể cắt (không vượt quá 1000 ký tự)
            int splitPos = -1;

            // ƯU TIÊN 1: Tìm dấu câu kết thúc (. ! ?) từ cuối đoạn ngược về đầu
            // → Đảm bảo câu hoàn chỉnh, âm thanh nghe tự nhiên hơn
            for (int i = endPos; i >= start; i--) {
                char c = text.charAt(i);
                if (c == '.' || c == '!' || c == '?') {
                    splitPos = i + 1; // Cắt SAU dấu câu (bao gồm dấu câu vào đoạn trước)
                    break;
                }
            }

            if (splitPos != -1 && splitPos > start) {
                // Tìm được dấu câu → cắt tại đó
                String chunk = text.substring(start, splitPos).trim();
                if (!chunk.isEmpty())
                    chunks.add(chunk);
                start = splitPos; // Dịch con trỏ sang đầu đoạn tiếp theo
            } else {
                // ƯU TIÊN 2: Không có dấu câu → tìm khoảng trắng gần nhất trước endPos
                // → Tránh cắt đứt giữa một từ (vd: "nghiên|cứu" → sai)
                int spacePos = text.lastIndexOf(' ', endPos);
                if (spacePos != -1 && spacePos > start) {
                    String chunk = text.substring(start, spacePos).trim();
                    if (!chunk.isEmpty())
                        chunks.add(chunk);
                    start = spacePos + 1; // Bỏ qua khoảng trắng, bắt đầu từ ký tự tiếp theo
                } else {
                    // ƯU TIÊN 3: Cắt cứng tại endPos (xấu nhất, hiếm xảy ra)
                    String chunk = text.substring(start, endPos).trim();
                    if (!chunk.isEmpty())
                        chunks.add(chunk);
                    start = endPos;
                }
            }
        }
        return chunks;
    }

    /**
     * LÀM SẠCH VĂN BẢN trước khi gửi lên Python TTS Microservice.
     * Mục đích: Loại bỏ những ký tự mà engine TTS không đọc được hoặc gây lỗi.
     */
    private String sanitizeText(String raw) {
        if (raw == null)
            return "";

        // Bước 1: Xóa ký tự điều khiển ASCII (mã 0-31, trừ \n=10 và \t=9)
        // Các ký tự này vô hình nhưng khiến edge-tts báo lỗi "NoAudioReceived"
        String cleaned = raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ");

        // Bước 2: Xóa toàn bộ HTML tags (vd: <p>, <strong>, <br/>, <span class="...">)
        // Văn bản chương sách có thể được nhập từ trình soạn thảo HTML (TinyMCE,
        // Quill...)
        cleaned = cleaned.replaceAll("<[^>]*>", " ");

        // Bước 3: Giải mã HTML entities phổ biến thành ký tự thật
        // vd: &amp; → &, &lt; → <, &nbsp; → (khoảng trắng thường)
        cleaned = cleaned.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&nbsp;", " ").replace("&quot;", "\"");

        // Bước 4: Xóa emoji và ký tự Unicode ngoài BMP (Supplementary Multilingual
        // Plane)
        // Surrogate pairs (D800-DFFF) là cách Java mã hóa emoji - edge-tts không đọc
        // được emoji
        cleaned = cleaned.replaceAll("[\\uD800-\\uDFFF]", "");

        // Bước 5: Chuẩn hóa khoảng trắng - gộp nhiều space/tab liên tiếp thành 1 khoảng
        // trắng
        cleaned = cleaned.replaceAll("[ \\t]+", " ").trim();
        return cleaned;
    }

    /**
     * GỬI VĂN BẢN ĐẾN PYTHON TTS MICROSERVICE.
     * 
     * Quy trình:
     * 1. Làm sạch văn bản (sanitizeText)
     * 2. Cắt thành các đoạn ≤1000 ký tự (splitTextSafely)
     * 3. Gửi TẤT CẢ đoạn trong 1 request duy nhất đến Python
     * 4. Python xử lý → upload Cloudinary → trả về 1 URL
     * 5. Trả URL đó về dưới dạng Flux<String>
     * 
     * Trả về Flux<String> vì đây là kiểu Reactive - cho phép xử lý bất đồng bộ
     * không block thread
     */
    public Flux<String> requestMultiSegmentsTTS(String textContent, String narratorCode) {
        // Kiểm tra đầu vào: không xử lý văn bản rỗng
        if (textContent == null || textContent.isBlank()) {
            return Flux.error(new IllegalArgumentException(
                    "Nội dung văn bản trống, không thể tạo audio."));
        }

        // Làm sạch văn bản (xóa HTML, emoji, ký tự lạ)
        String sanitized = sanitizeText(textContent);

        // Kiểm tra lại sau khi làm sạch (có thể văn bản chỉ toàn HTML tags, sau xóa thì
        // rỗng)
        if (sanitized.isBlank()) {
            return Flux.error(new IllegalArgumentException(
                    "Văn bản sau khi làm sạch bị rỗng. Vui lòng kiểm tra lại nội dung chương."));
        }

        // Cắt văn bản thành danh sách đoạn nhỏ an toàn (mỗi đoạn ≤ 1000 ký tự)
        List<String> chunks = splitTextSafely(sanitized, MAX_TEXT_LENGTH);

        System.out.printf("📤 [TTS] Chia thành %d đoạn để gửi tới Python TTS (voice=%s) trong 1 Request duy nhất%n",
                chunks.size(), narratorCode);

        // Gửi HTTP POST đến Python FastAPI với toàn bộ danh sách đoạn text
        return webClient.post()
                .uri("http://localhost:8000/api/generate-audio") // Địa chỉ Python TTS Microservice
                .contentType(MediaType.APPLICATION_JSON) // Báo Python biết data gửi là JSON
                .bodyValue(new PythonTtsRequest(chunks, narratorCode)) // Body: { text_chunks: [...], voice: "banmai" }
                .retrieve()
                // Xử lý lỗi HTTP 4xx (bad request) hoặc 5xx (server error) từ Python
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "Python TTS HTTP " + clientResponse.statusCode().value() + ": " + body))))
                // Parse JSON response từ Python thành đối tượng PythonTtsResponse
                .bodyToMono(PythonTtsResponse.class)
                // Chuyển Mono (1 giá trị) thành Flux (luồng giá trị) để caller dùng
                // .subscribe()
                .flatMapMany(response -> {
                    System.out.println("📡 [TTS] Python Response: status=" + response.getStatus()
                            + " | audio_url=" + response.getAudioUrl());

                    // Kiểm tra Python trả về thành công và có URL hợp lệ
                    if ("success".equalsIgnoreCase(response.getStatus())
                            && response.getAudioUrl() != null
                            && !response.getAudioUrl().isBlank()) {
                        return Flux.just(response.getAudioUrl()); // Phát URL ra cho subscriber nhận
                    }

                    // Python báo lỗi trong body response → ném exception
                    String errMsg = response.getMessage() != null ? response.getMessage()
                            : "Lỗi không xác định từ Python TTS";
                    return Flux.error(new RuntimeException("Python TTS Error: " + errMsg));
                })
                .doOnError(err -> System.err.println("❌ [TTS] Lỗi đoạn: " + err.getMessage())); // Log lỗi cuối cùng
    }

    // ==========================================
    // CÁC LỚP DỮ LIỆU NỘI BỘ (Inner Classes)
    // ==========================================

    /**
     * Cấu trúc JSON GỬI ĐI đến Python FastAPI.
     * @JsonProperty("text_chunks"): map field Java "textChunks" → JSON key
     * "text_chunks"
     * (Python dùng snake_case, Java dùng camelCase)
     */
    @Data
    private static class PythonTtsRequest {
        @JsonProperty("text_chunks") // Khi serialize sang JSON: textChunks → "text_chunks"
        private List<String> textChunks; // Danh sách các đoạn văn bản đã cắt

        private String voice; // Mã giọng đọc (vd: "banmai", "leminh")

        public PythonTtsRequest(List<String> textChunks, String voice) {
            this.textChunks = textChunks;
            this.voice = voice;
        }
    }

    /**
     * Cấu trúc JSON NHẬN VỀ từ Python FastAPI.
     * Python trả về: { "status": "success", "audio_url": "https://..." }
     */
    @Data
    private static class PythonTtsResponse {
        private String status; // "success" hoặc "error"

        @JsonProperty("audio_url") // Khi deserialize JSON: "audio_url" → audioUrl (Java camelCase)
        private String audioUrl; // URL HTTPS của file MP3 trên Cloudinary CDN

        private String message; // Thông báo lỗi (nếu có), null khi thành công
    }
}