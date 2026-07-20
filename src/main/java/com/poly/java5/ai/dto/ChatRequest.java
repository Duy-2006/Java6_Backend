package com.poly.java5.ai.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ChatRequest {
    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String message;
    
    private String conversationId;
}
