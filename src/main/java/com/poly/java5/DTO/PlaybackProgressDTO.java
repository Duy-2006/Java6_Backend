package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaybackProgressDTO {
    private Long chapterId;
    private Integer segmentIndex;
    private Double currentTimeSeconds;
    private Double playbackRate;
}
