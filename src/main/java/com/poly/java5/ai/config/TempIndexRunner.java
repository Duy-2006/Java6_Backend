package com.poly.java5.ai.config;

import com.poly.java5.ai.service.BookIndexingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TempIndexRunner implements CommandLineRunner {

    private final BookIndexingService bookIndexingService;

    public TempIndexRunner(BookIndexingService bookIndexingService) {
        this.bookIndexingService = bookIndexingService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== TRIGGERING AI INDEXING ===");
        bookIndexingService.indexAllBooks();
        System.out.println("=== AI INDEXING COMPLETED ===");
    }
}
// Trigger restart
