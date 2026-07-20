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

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final UserService userService;

    public ChatbotController(ChatbotService chatbotService, UserService userService) {
        this.chatbotService = chatbotService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        Integer userId = null;
        try {
            userId = AuthUtil.getAuthenticatedUserId(userService); 
        } catch (Exception e) {
            // Ignore error, guest users can chat
        }

        ChatResponse response = chatbotService.handleChat(request, userId);
        return ResponseEntity.ok(response);
    }
}
