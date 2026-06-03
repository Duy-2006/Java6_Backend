package com.poly.java5.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIO_PLAYBACK_PROGRESS", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "book_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioPlaybackProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "segment_index")
    private Integer segmentIndex;

    @Column(name = "current_time_seconds")
    private Double currentTimeSeconds;

    @Column(name = "playback_rate")
    private Double playbackRate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
