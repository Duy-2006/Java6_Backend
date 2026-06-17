package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đại diện cho một đoạn audio (segment) thuộc một chương sách.
 * Hỗ trợ quan hệ 1-N: một chương có nhiều segments để gapless playback.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioSegmentDTO {
    /** URL vĩnh viễn (Cloudinary) của file MP3 */
    private String audioUrl;

    /** Thứ tự phát (1, 2, 3, …) — dùng để sắp xếp trên frontend */
    private Integer sequenceOrder;

    /** Thời lượng của đoạn này tính bằng giây */
    private Integer durationSeconds;

    /** Mã ngôn ngữ (vi, en, ja...) */
    private String languageCode;

    /** ID của giọng đọc (TTS_Voice ID) */
    private Integer languageId;

    /** Trạng thái của file audio (SUCCESS, INACTIVE, PROCESSING, FAILED) */
    private String ttsStatus;

    /** Chỉ định xem audio có lỗi thời so với văn bản gốc hay không */
    private Boolean isOutdated;
}
