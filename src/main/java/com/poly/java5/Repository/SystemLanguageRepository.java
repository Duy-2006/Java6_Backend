package com.poly.java5.Repository;

import com.poly.java5.Entity.SystemLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemLanguageRepository extends JpaRepository<SystemLanguage, Long> {
    Optional<SystemLanguage> findByCode(String code);
}
