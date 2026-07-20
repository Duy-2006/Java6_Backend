package com.poly.java5.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private String intent;
    private List<ChatSourceDto> sources;
    private String conversationId;
    private boolean fallback;
}
