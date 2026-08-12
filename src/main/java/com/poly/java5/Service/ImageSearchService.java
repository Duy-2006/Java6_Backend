package com.poly.java5.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.poly.java5.DTO.BookDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.BookFormat;
import com.poly.java5.Repository.BookFormatRepository;
import com.poly.java5.Repository.BookRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ImageSearchService {

    private final BookRepository bookRepository;
    private final BookFormatRepository bookFormatRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    @Data
    public static class ImageSearchResult {
        @JsonProperty("book_id")
        private Integer bookId;
        private Double similarity;
    }

    @Data
    public static class BookIndexItem {
        @JsonProperty("book_id")
        private Integer bookId;
        @JsonProperty("image_url")
        private String imageUrl;

        public BookIndexItem(Integer bookId, String imageUrl) {
            this.bookId = bookId;
            this.imageUrl = imageUrl;
        }
    }

    /**
     * Gửi file ảnh sang Python Service (OpenCLIP + FAISS) để tìm kiếm độ tương đồng,
     * sau đó chuyển đổi danh sách ID tìm được thành danh sách BookDTO đầy đủ thông tin từ Database.
     */
    public List<BookDTO> searchByImage(MultipartFile imageFile, int topK) {
        if (imageFile == null || imageFile.isEmpty()) {
            log.warn("File ảnh tìm kiếm rỗng.");
            return Collections.emptyList();
        }

        try {
            log.info("Gửi file ảnh sang Python Service ({}/api/image-search) để tìm kiếm vector...", pythonServiceUrl);

            ByteArrayResource resource = new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename() : "search_image.jpg";
                }
            };

            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("image", resource, MediaType.parseMediaType(
                    imageFile.getContentType() != null ? imageFile.getContentType() : MediaType.IMAGE_JPEG_VALUE
            ));
            bodyBuilder.part("top_k", String.valueOf(topK));

            WebClient webClient = webClientBuilder.baseUrl(pythonServiceUrl).build();

            List<ImageSearchResult> searchResults = webClient.post()
                    .uri("/api/image-search")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ImageSearchResult>>() {})
                    .onErrorReturn(Collections.emptyList())
                    .block();

            if (searchResults == null || searchResults.isEmpty()) {
                log.info("Không tìm thấy kết quả nào từ Python Image Search.");
                return Collections.emptyList();
            }

            log.info("Python trả về {} kết quả tương đồng. Đang truy vấn Database để lấy DTO...", searchResults.size());

            List<BookDTO> dtoList = new ArrayList<>();
            for (ImageSearchResult res : searchResults) {
                if (res.getBookId() == null) continue;

                Optional<Book> bookOpt = bookRepository.findById(res.getBookId());
                if (bookOpt.isPresent()) {
                    Book b = bookOpt.get();
                    // Chỉ lấy sách còn hoạt động và chưa bị xóa mềm
                    if (Boolean.TRUE.equals(b.getActive()) && !Boolean.TRUE.equals(b.getDeleted())) {
                        BookDTO dto = convertToDTO(b);
                        dtoList.add(dto);
                    }
                }
            }

            return dtoList;
        } catch (Exception e) {
            log.error("Lỗi trong quá trình tìm kiếm sách bằng hình ảnh: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Đồng bộ thêm mới hoặc cập nhật 1 sách vào FAISS Index trong Python Service.
     */
    public void updateBookIndex(Integer bookId, String imageUrl) {
        if (bookId == null || imageUrl == null || imageUrl.isBlank()) return;

        try {
            log.info("Đang gọi Python API để cập nhật FAISS Index cho sách ID: {}", bookId);
            WebClient webClient = webClientBuilder.baseUrl(pythonServiceUrl).build();

            BookIndexItem item = new BookIndexItem(bookId, imageUrl);
            webClient.post()
                    .uri("/api/index/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(item)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnError(err -> log.error("Lỗi cập nhật Index cho sách ID {}: {}", bookId, err.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Lỗi khi gửi request cập nhật Index cho sách ID {}: {}", bookId, e.getMessage());
        }
    }

    /**
     * Đồng bộ xóa 1 sách khỏi FAISS Index trong Python Service.
     */
    public void deleteBookIndex(Integer bookId) {
        if (bookId == null) return;

        try {
            log.info("Đang gọi Python API để xóa sách ID {} khỏi FAISS Index...", bookId);
            WebClient webClient = webClientBuilder.baseUrl(pythonServiceUrl).build();

            webClient.delete()
                    .uri("/api/index/delete/" + bookId)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnError(err -> log.error("Lỗi xóa Index cho sách ID {}: {}", bookId, err.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Lỗi khi gửi request xóa Index cho sách ID {}: {}", bookId, e.getMessage());
        }
    }

    /**
     * Đánh lại chỉ mục FAISS từ đầu cho toàn bộ sách active trong Database.
     */
    public Map<String, Object> reindexAllBooks() {
        try {
            List<Book> books = bookRepository.findByDeletedFalse();
            List<BookIndexItem> items = books.stream()
                    .filter(b -> Boolean.TRUE.equals(b.getActive()) && b.getImageUrl() != null && !b.getImageUrl().isBlank())
                    .map(b -> new BookIndexItem(b.getId(), b.getImageUrl()))
                    .toList();

            log.info("Bắt đầu Reindex toàn bộ {} sách sang Python Service...", items.size());
            WebClient webClient = webClientBuilder.baseUrl(pythonServiceUrl).build();

            Map<String, Object> requestBody = Map.of("books", items);

            Map response = webClient.post()
                    .uri("/api/index/build")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Reindex hoàn tất. Trả về: {}", response);
            return response != null ? response : Map.of("status", "success", "total", items.size());
        } catch (Exception e) {
            log.error("Lỗi Reindex toàn bộ sách: ", e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    private BookDTO convertToDTO(Book b) {
        BookDTO dto = new BookDTO();
        dto.setId(b.getId());
        dto.setTitle(b.getTitle());
        dto.setIsbn(b.getIsbn());
        dto.setPrice(b.getPrice());
        dto.setQuantity(b.getQuantity());
        dto.setActive(b.getActive());
        dto.setDescription(b.getDescription());
        dto.setImageUrl(b.getImageUrl());
        dto.setAuthorName(b.getAuthor() != null ? b.getAuthor().getName() : null);

        if (b.getCategory() != null) {
            dto.setCategoryId(b.getCategory().getId());
            dto.setCategoryName(b.getCategory().getName());
        }

        bookFormatRepository.findByBookIdAndFormatType(b.getId(), "AUDIO")
                .ifPresent(f -> dto.setAudioPrice(f.getPrice()));

        dto.setSoldCount(bookRepository.getSoldCountById(b.getId()));
        return dto;
    }
}
