package com.poly.java5.Repository;

import com.poly.java5.Entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {

    /**
     * Lấy tất cả banner và tự động sắp xếp theo thứ tự (vị trí) tăng dần
     * Phục vụ hiển thị chuẩn trên bảng danh sách quản lý của Admin
     */
    List<Banner> findAllByOrderByPositionAsc();

    /**
     * Lấy danh sách các banner đang ở trạng thái Kích hoạt (active = true)
     * và sắp xếp theo vị trí tăng dần để hiển thị ngoài trang chủ (Client)
     */
    List<Banner> findByActiveTrueOrderByPositionAsc();
    
    /**
     * Tìm kiếm nhanh các banner theo đường dẫn liên kết điều hướng
     */
    List<Banner> findByLinkContaining(String link);
}