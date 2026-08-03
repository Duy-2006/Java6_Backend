package com.poly.java5.ai.controller;

import com.poly.java5.ai.dto.ChatRequest;
import com.poly.java5.ai.dto.ChatResponse;
import com.poly.java5.ai.service.ChatbotService;
import com.poly.java5.Utils.AuthUtil;
import com.poly.java5.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import com.poly.java5.ai.tool.BookstoreTools;
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final UserService userService;
    private final BookstoreTools bookstoreTools;

    public ChatbotController(ChatbotService chatbotService, UserService userService, BookstoreTools bookstoreTools) {
        this.chatbotService = chatbotService;
        this.userService = userService;
        this.bookstoreTools = bookstoreTools;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        Integer userId = null;
        try {
            // Lấy userId từ request attribute (do AuthFilter đã parse từ JWT)
            Object userIdAttr = httpRequest.getAttribute("userId");
            if (userIdAttr != null) {
                userId = Integer.parseInt(userIdAttr.toString());
            } else {
                // Fallback nếu cần
                userId = AuthUtil.getAuthenticatedUserId(userService);
            }
        } catch (Exception e) {
            System.err.println("Error extracting user ID: " + e.getMessage());
        }

        ChatResponse response = chatbotService.handleChat(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug-promo")
    public ResponseEntity<?> debugPromo() {
        return ResponseEntity.ok(bookstoreTools.getActivePromotions());
    }
}
