package com.poly.java5.Repository;


import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;

import jakarta.transaction.Transactional;

public interface UserRepository extends JpaRepository<User, Integer > {
	User findByUsername(String username);

    List<User> findByRole(UserRole role);
    
    // 🔥 UPDATE TRỰC TIẾP DB (CHẮC CHẮN CHẠY)
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.active = :status WHERE u.username = :username")
    int updateActiveStatus(@Param("username") String username,
                           @Param("status") Boolean status);
}
