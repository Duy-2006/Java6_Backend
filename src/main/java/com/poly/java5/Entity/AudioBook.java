package com.poly.java5.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AUDIO_BOOK")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioBook {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audio_url", columnDefinition = "NVARCHAR(MAX)")
    private String audioUrl; // Link file mp3 sau khi AI trả về

    @Column(name = "duration_seconds")
    private Integer durationSeconds; // Thời lượng

    // Các cột phục vụ nghiệp vụ gọi API Bất đồng bộ
    @Column(name = "tts_status", length = 20)
    private String ttsStatus; // PENDING, PROCESSING, SUCCESS, FAILED

    @Column(name = "task_id")
    private String taskId; // ID tiến trình của FPT.AI/Google

    // NHIỀU File Audio được tạo ra từ 1 Chương Sách
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    @JsonBackReference
    private BookChapter chapter;

    // File Audio này đọc bằng ngôn ngữ/giọng nào?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private AudioLanguage language;
    
 // Thêm vào class AudioBook của bạn
    @Column(name = "sequence_order")
    private Integer sequenceOrder; // Lưu thứ tự: 0, 1, 2, 3...
}
