package com.poly.java5.ai.service;

import com.poly.java5.ai.prompt.ChatbotPrompt;
import com.poly.java5.ai.tool.BookstoreTools;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class RagService {

    private final BookstoreAssistant assistant;
    private final BookstoreTools bookstoreTools;

    public RagService(ChatLanguageModel chatLanguageModel, 
                      EmbeddingStore<TextSegment> embeddingStore, 
                      EmbeddingModel embeddingModel,
                      BookstoreTools bookstoreTools) {
        
        this.bookstoreTools = bookstoreTools;

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(4)
                .minScore(0.65)
                .build();

        this.assistant = AiServices.builder(BookstoreAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever)
                .tools(bookstoreTools)
                .build();
    }

    interface BookstoreAssistant {
        @SystemMessage(ChatbotPrompt.SYSTEM_PROMPT)
        dev.langchain4j.service.Result<String> chat(@MemoryId String conversationId, @UserMessage String userMessage);
    }

    public dev.langchain4j.service.Result<String> getAnswer(String conversationId, String message, Integer userId) {
        if (userId != null) {
            bookstoreTools.setCurrentUserId(userId);
        }
        try {
            return assistant.chat(conversationId, message);
        } finally {
            bookstoreTools.clearCurrentUserId();
        }
    }
}
