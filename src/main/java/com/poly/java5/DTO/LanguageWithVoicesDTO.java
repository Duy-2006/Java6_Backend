package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** DTO đại diện cho một ngôn ngữ kèm danh sách giọng đọc thuộc ngôn ngữ đó */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageWithVoicesDTO {
    private Long id;
    /** Mã ngôn ngữ ngắn, ví dụ: "vi", "en" */
    private String code;
    /** Tên hiển thị, ví dụ: "Tiếng Việt", "English" */
    private String name;
    /** Danh sách giọng đọc thuộc ngôn ngữ này */
    private List<VoiceDTO> voices;
}
