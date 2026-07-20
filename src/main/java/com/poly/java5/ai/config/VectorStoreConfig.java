package com.poly.java5.ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.port:6334}")
    private int qdrantPort;

    @Value("${qdrant.collection:bookstore_index}")
    private String qdrantCollection;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        try {
            // Eagerly check if Qdrant is running by pinging the port
            try (java.net.Socket socket = new java.net.Socket(qdrantHost, qdrantPort)) {
                log.info("Qdrant is running. Initializing Qdrant Vector Store at {}:{} collection {}", qdrantHost, qdrantPort, qdrantCollection);
                return QdrantEmbeddingStore.builder()
                        .host(qdrantHost)
                        .port(qdrantPort)
                        .collectionName(qdrantCollection)
                        .build();
            } catch (java.net.ConnectException ce) {
                log.warn("Qdrant is NOT running at {}:{}. Falling back to InMemoryEmbeddingStore.", qdrantHost, qdrantPort);
                return new InMemoryEmbeddingStore<>();
            }
        } catch (Exception e) {
            log.warn("Failed to connect to Qdrant. Falling back to InMemoryEmbeddingStore. Please start Qdrant via docker-compose.", e);
            return new InMemoryEmbeddingStore<>();
        }
    }
}
