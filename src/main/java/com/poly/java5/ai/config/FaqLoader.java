package com.poly.java5.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jakarta.annotation.PostConstruct;

@Component
public class FaqLoader {
    private static final Logger log = LoggerFactory.getLogger(FaqLoader.class);
    private String faqContent = "";

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("chatbot-documents/faq.md");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    faqContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    log.info("Loaded FAQ content from chatbot-documents/faq.md");
                }
            } else {
                log.warn("chatbot-documents/faq.md not found in classpath.");
            }
        } catch (Exception e) {
            log.error("Error loading FAQ content", e);
        }
    }

    public String getFaqContent() {
        return faqContent;
    }
}
