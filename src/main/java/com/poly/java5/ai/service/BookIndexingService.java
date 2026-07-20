package com.poly.java5.ai.service;

import com.poly.java5.Entity.Book;
import com.poly.java5.Repository.BookRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookIndexingService {

    private static final Logger log = LoggerFactory.getLogger(BookIndexingService.class);

    private final BookRepository bookRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public BookIndexingService(BookRepository bookRepository, 
                               EmbeddingStore<TextSegment> embeddingStore, 
                               EmbeddingModel embeddingModel) {
        this.bookRepository = bookRepository;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
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

        String content = String.format(
                "Book ID: %d\nTitle: %s\nAuthors: %s\nCategories: %s\nPublisher: %s\nDescription: %s",
                book.getId(),
                book.getTitle(),
                authorNames,
                categoryName,
                publisherName,
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
        // Langchain4j EmbeddingStore doesn't natively support easy deletion by metadata across all stores yet.
        // For production Qdrant, we'd use native QdrantClient to delete by payload filter.
        // For now, this is a placeholder or we can implement a custom delete if using Qdrant client directly.
        log.warn("removeBookFromIndex not fully supported by generic EmbeddingStore abstraction. Id: {}", bookId);
    }
}
