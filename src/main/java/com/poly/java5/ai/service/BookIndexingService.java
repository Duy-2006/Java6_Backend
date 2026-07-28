package com.poly.java5.ai.service;

import com.poly.java5.Entity.Book;
import com.poly.java5.Repository.BookRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import com.poly.java5.Repository.BookFormatRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;


@Service
@Transactional
public class BookIndexingService {

    private static final Logger log = LoggerFactory.getLogger(BookIndexingService.class);

    private final BookRepository bookRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final BookFormatRepository bookFormatRepository;

    public BookIndexingService(BookRepository bookRepository, 
                               EmbeddingStore<TextSegment> embeddingStore, 
                               EmbeddingModel embeddingModel,
                               BookFormatRepository bookFormatRepository) {
        this.bookRepository = bookRepository;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.bookFormatRepository = bookFormatRepository;
    }

    public void indexAllBooks() {
        log.info("Starting to index all books into Vector Store...");
        List<Book> activeBooks = bookRepository.findAll().stream()
                .filter(Book::isAvailable)
                .collect(Collectors.toList());

        for (Book book : activeBooks) {
            indexBook(book);
        }
        log.info("Finished indexing {} books.", activeBooks.size());
    }

    public void indexBook(Integer bookId) {
        bookRepository.findById(bookId).ifPresent(this::indexBook);
    }

    public void indexBook(Book book) {
        if (!book.isAvailable()) {
            removeBookFromIndex(book.getId());
            return;
        }

        String authorNames = book.getAuthor() != null ? book.getAuthor().getName() : "Unknown";
        String categoryName = book.getCategory() != null ? book.getCategory().getName() : "Unknown";
        String publisherName = book.getPublisher();

        boolean isAudiobook = bookFormatRepository.findByBookIdAndFormatType(book.getId(), "AUDIO").isPresent();
        String formatInfo = isAudiobook ? "Sách nói, Audiobook, Nghe audio" : "Sách giấy, Sách in";
        
        String chapterInfo = "";
        if (isAudiobook) {
            int totalChapters = 0;
            java.util.Set<String> languages = new java.util.HashSet<>();
            java.util.Set<String> voices = new java.util.HashSet<>();
            if (book.getChapters() != null) {
                totalChapters = book.getChapters().size();
                for (com.poly.java5.Entity.BookChapter ch : book.getChapters()) {
                    if (ch.getAudioBooks() != null) {
                        for (com.poly.java5.Entity.AudioBook ab : ch.getAudioBooks()) {
                            if (ab.getLanguage() != null && "SUCCESS".equalsIgnoreCase(ab.getTtsStatus())) {
                                voices.add(ab.getLanguage().getLanguageName());
                                if (ab.getLanguage().getSystemLanguage() != null) {
                                    languages.add(ab.getLanguage().getSystemLanguage().getName());
                                }
                            }
                        }
                    }
                }
            }
            if (totalChapters > 0) {
                chapterInfo = String.format("\nAudiobook Chapters: %d (Languages: %s, Voices: %s)", 
                        totalChapters, 
                        languages.isEmpty() ? "Unknown" : String.join(", ", languages),
                        voices.isEmpty() ? "Unknown" : String.join(", ", voices));
            }
        }

        String content = String.format(
                "Book ID: %d\nTitle: %s\nAuthors: %s\nCategories: %s\nPublisher: %s\nFormats: %s%s\nDescription: %s",
                book.getId(),
                book.getTitle(),
                authorNames,
                categoryName,
                publisherName,
                formatInfo,
                chapterInfo,
                book.getDescription() != null ? book.getDescription() : ""
        );

        Metadata metadata = new Metadata()
                .put("bookId", book.getId())
                .put("title", book.getTitle())
                .put("active", book.getActive().toString());

        Document document = Document.from(content, metadata);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(800, 100))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);
        log.info("Indexed book ID: {}", book.getId());
    }

    public void removeBookFromIndex(Integer bookId) {
        log.info("Bắt đầu tiến hành xóa Vector Index của sách ID: {}", bookId);
        try {
            // Tìm và xóa tất cả các Vector có metadata 'bookId' bằng với bookId được truyền vào
            embeddingStore.removeAll(metadataKey("bookId").isEqualTo(bookId));
            log.info("Đã xóa thành công toàn bộ Vector của sách ID: {}", bookId);
        } catch (UnsupportedOperationException e) {
            log.error("Vector Store hiện tại chưa hỗ trợ xóa bằng Filter. Lỗi: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không xác định khi xóa index sách ID {}: {}", bookId, e.getMessage());
        }
    }
}
