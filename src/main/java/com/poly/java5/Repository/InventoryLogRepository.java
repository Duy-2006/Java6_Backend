package com.poly.java5.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.poly.java5.Entity.InventoryLog;
import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    // Lấy lịch sử giảm dần theo ngày
    List<InventoryLog> findAllByOrderByLogDateDesc();
}