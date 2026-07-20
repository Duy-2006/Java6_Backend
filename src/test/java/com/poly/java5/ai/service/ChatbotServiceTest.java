package com.poly.java5.ai.service;

import com.poly.java5.ai.dto.ChatRequest;
import com.poly.java5.ai.dto.ChatResponse;
import com.poly.java5.ai.tool.BookstoreTools;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ChatbotServiceTest {

    @Mock
    private RagService ragService;

    @Mock
    private ChatIntentService chatIntentService;

    @Mock
    private BookstoreTools bookstoreTools;

    @InjectMocks
    private ChatbotService chatbotService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testBookRecommendationRetrieval() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Gợi ý sách cho tôi");

        when(chatIntentService.detectIntent(anyString())).thenReturn(ChatIntentService.Intent.BOOK_RECOMMENDATION);
        
        Result<String> mockResult = Result.<String>builder()
                .content("Tôi gợi ý Nhà Giả Kim")
                .sources(Collections.emptyList()) // Ideally mocked Content items here
                .build();
                
        when(ragService.getAnswer(anyString(), anyString(), any())).thenReturn(mockResult);

        ChatResponse res = chatbotService.handleChat(req, 1);

        assertEquals("Tôi gợi ý Nhà Giả Kim", res.getAnswer());
        assertEquals("BOOK_RECOMMENDATION", res.getIntent());
        assertFalse(res.isFallback());
    }

    @Test
    public void testUnknownBookQueryFallback() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Bạn biết sách ABCXYZ không?");

        when(chatIntentService.detectIntent(anyString())).thenReturn(ChatIntentService.Intent.BOOK_INFORMATION);
        
        when(ragService.getAnswer(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Connection failed"));

        ChatResponse res = chatbotService.handleChat(req, 1);

        assertTrue(res.isFallback());
        assertTrue(res.getAnswer().contains("quá tải hoặc gặp sự cố"));
    }

    @Test
    public void testUnauthenticatedOrderQuery() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Đơn hàng 123 của tôi đâu?");

        when(chatIntentService.detectIntent(anyString())).thenReturn(ChatIntentService.Intent.ORDER_QUERY);
        
        Result<String> mockResult = Result.<String>builder()
                .content("Vui lòng đăng nhập để xem đơn hàng.")
                .build();
                
        // Passing null userId
        when(ragService.getAnswer(anyString(), anyString(), isNull())).thenReturn(mockResult);

        ChatResponse res = chatbotService.handleChat(req, null);

        assertEquals("Vui lòng đăng nhập để xem đơn hàng.", res.getAnswer());
    }

    @Test
    public void testOutOfScopeQuery() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Thời tiết hôm nay thế nào?");

        when(chatIntentService.detectIntent(anyString())).thenReturn(ChatIntentService.Intent.OUT_OF_SCOPE);

        ChatResponse res = chatbotService.handleChat(req, 1);

        assertTrue(res.isFallback());
        assertTrue(res.getAnswer().contains("chỉ có thể trả lời các câu hỏi liên quan đến sách"));
    }
}
