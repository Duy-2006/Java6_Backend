package com.poly.java5.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.poly.java5.Entity.UserLibrary;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Integer> {
    
	boolean existsByUser_IdAndBook_IdAndVariant_FormatType(
            Integer userId,
            Integer bookId,
            String formatType
    );

    List<UserLibrary> findByUser_IdAndVariant_FormatType(
            Integer userId,
            String formatType
    );

	boolean existsByBook_Id(Integer bookId);
}
