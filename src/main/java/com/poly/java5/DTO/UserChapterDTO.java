package com.poly.java5.DTO;

import java.util.List;
import lombok.Data;

@Data
public class UserChapterDTO {
    private Long id;
    private String number;
    private String title;
    private String status;
    private Integer progress;
    private List<AudioSegmentDTO> audioSegments;
    private String duration;
    private String voiceModel;
    private String speed;
    private String textContent;
    @com.fasterxml.jackson.annotation.JsonProperty("isLocked")
    private boolean isLocked; // Hook logic: Chapter 1 free, others locked if not purchased
}
