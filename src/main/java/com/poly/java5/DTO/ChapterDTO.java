package com.poly.java5.DTO;

import java.util.List;

import lombok.Data;

/**
 * DTO đại diện cho một chương sách.
 * Quan hệ 1-N: mỗi chương có thể có nhiều audio segments
 * để hỗ trợ gapless playback trên frontend.
 */
@Data
public class ChapterDTO {
    private Long id;
    private String number;
    private String title;
    private String status;      // "completed", "processing", "pending", "failed"
    private Integer progress;

    /**
     * Danh sách các đoạn audio của chương này, sắp xếp theo sequenceOrder ASC.
     * Frontend dùng mảng này để phát liền mạch (gapless playback).
     */
    private List<AudioSegmentDTO> audioSegments;

    /** Tổng thời lượng của tất cả segments (format mm:ss) */
    private String duration;

    private String voiceModel;
    private String speed;
    private String textContent;
}
