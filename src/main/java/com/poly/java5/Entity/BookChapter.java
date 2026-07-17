package com.poly.java5.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "Book_Chapter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookChapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_number")
    private Integer chapterNumber; // Số thứ tự chương (1, 2, 3...)

    @Column(name = "title")
    private String title; // Tiêu đề chương

    @Lob
    @Column(name = "content_text", columnDefinition = "LONGTEXT")
    private String contentText; // Đã sửa NVARCHAR(MAX) thành LONGTEXT để tương thích với MySQL

    // NHIỀU Chương thuộc về 1 Cuốn Sách
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    @JsonBackReference // Ngắt vòng lặp JSON (Chiều về)
    private Book book;

    // 1 Chương có thể sinh ra NHIỀU file Audio (với các giọng đọc khác nhau)
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @OrderBy("sequenceOrder ASC") // TỰ ĐỘNG sắp xếp khi lấy từ DB
    private List<AudioBook> audioBooks;
}