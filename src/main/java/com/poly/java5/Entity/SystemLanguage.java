package com.poly.java5.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "System_Language")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã ngôn ngữ ngắn, ví dụ: "vi", "en" */
    @Column(name = "code", length = 10, nullable = false, unique = true)
    private String code;

    /** Tên hiển thị, ví dụ: "Tiếng Việt", "English" */
    @Column(name = "name", length = 100, nullable = false)
    private String name;
}
