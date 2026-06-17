package com.poly.java5.Entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TTS_Voice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TTS_Voice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voice_name")
    private String languageName; // Tên giọng đọc, VD: "Ban Mai (Nữ miền Bắc)"

    @Column(name = "narrator_code", length = 50)
    private String narratorCode; // Mã giọng gửi lên FPT.AI, VD: "banmai"

    /** Ngôn ngữ mà giọng này thuộc về (quan hệ N-1 → System_Language) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private SystemLanguage systemLanguage;
}
