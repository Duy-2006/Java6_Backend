package com.poly.java5.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class ChatIntentService {

    private final IntentClassifier classifier;

    public ChatIntentService(ChatLanguageModel chatLanguageModel) {
        this.classifier = AiServices.builder(IntentClassifier.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    public enum Intent {
        BOOK_RECOMMENDATION,
        BOOK_INFORMATION,
        PRICE_QUERY,
        INVENTORY_QUERY,
        POLICY_QUERY,
        ORDER_QUERY,
        AUDIOBOOK_QUERY,
        GENERAL_CHAT,
        OUT_OF_SCOPE
    }

    interface IntentClassifier {
        @SystemMessage("""
            You are an intent classification system for a bookstore chatbot.
            Classify the user's message into EXACTLY one of the following intents:
            - BOOK_RECOMMENDATION (asking for book suggestions)
            - BOOK_INFORMATION (asking about book content, author, etc.)
            - PRICE_QUERY (asking about prices, discounts)
            - INVENTORY_QUERY (asking if a book is in stock)
            - POLICY_QUERY (asking about return, shipping, or store policies)
            - ORDER_QUERY (asking about their order status)
            - AUDIOBOOK_QUERY (asking about listening or accessing audiobooks)
            - GENERAL_CHAT (simple greetings or general conversation)
            - OUT_OF_SCOPE (unrelated to books, bookstore, or reading)
            
            Return ONLY the intent name.
            """)
        Intent classify(@UserMessage String text);
    }

    public Intent detectIntent(String message) {
        try {
            return classifier.classify(message);
        } catch (Exception e) {
            return Intent.GENERAL_CHAT;
        }
    }
}
