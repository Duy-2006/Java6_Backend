package com.poly.java5.Service;


import com.poly.java5.Entity.Author;
import com.poly.java5.Repository.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    // Lấy tất cả tác giả
    public List<Author> findAll() {
        return authorRepository.findAll();
    }

    // Tìm theo ID
    public Author findById(Long id) {
        return authorRepository.findById(id).orElse(null);
    }

    // Lưu (thêm mới hoặc cập nhật)
    public Author save(Author author) {
        return authorRepository.save(author);
    }

    // Xóa theo ID
    public void deleteById(Long id) {
        authorRepository.deleteById(id);
    }
}