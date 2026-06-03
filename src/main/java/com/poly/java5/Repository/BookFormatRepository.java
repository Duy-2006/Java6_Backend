package com.poly.java5.Repository;

import com.poly.java5.Entity.BookFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookFormatRepository extends JpaRepository<BookFormat, Integer> {
    Optional<BookFormat> findByBookIdAndFormatType(Integer bookId, String formatType);
}
