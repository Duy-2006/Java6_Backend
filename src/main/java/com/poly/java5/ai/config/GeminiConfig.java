package com.poly.java5.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${google.api.key:}")
    private String apiKey;

    @Value("${gemini.chat.model:gemini-3.5-flash-lite}")
    private String chatModelName;

    @Value("${gemini.embedding.model:text-embedding-004}")
    private String embeddingModelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("GOOGLE_API_KEY is not set");
        }
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                .temperature(0.2)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel();
    }
}
