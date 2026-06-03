package com.poly.java5.Entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Audio_Language")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioLanguage {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "language_name")
    private String languageName; // VD: "Tiếng Việt - Giọng Nữ Miền Bắc"

    @Column(name = "narrator_code", length = 50)
    private String narratorCode; // VD: "banmai" - Cực kỳ quan trọng để gửi qua API FPT.AI
}
