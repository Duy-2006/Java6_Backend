package com.poly.java5.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poly.java5.Entity.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}