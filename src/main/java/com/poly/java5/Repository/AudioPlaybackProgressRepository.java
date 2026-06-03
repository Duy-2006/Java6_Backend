package com.poly.java5.Repository;

import com.poly.java5.Entity.AudioPlaybackProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AudioPlaybackProgressRepository extends JpaRepository<AudioPlaybackProgress, Long> {
    Optional<AudioPlaybackProgress> findByUserIdAndBookId(Integer userId, Integer bookId);
}
