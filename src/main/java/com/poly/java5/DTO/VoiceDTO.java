package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO đại diện cho một giọng đọc (Voice) trả về cho frontend */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceDTO {
    private Long id;
    /** Mã giọng đọc gửi lên FPT.AI, ví dụ: "banmai" */
    private String narratorCode;
    /** Tên hiển thị, ví dụ: "Ban Mai (Nữ miền Bắc)" */
    private String voiceName;
}
