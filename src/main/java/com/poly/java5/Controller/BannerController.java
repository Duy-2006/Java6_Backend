package com.poly.java5.Controller;

import com.poly.java5.Entity.Banner;
import com.poly.java5.Repository.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    @Autowired
    private BannerRepository bannerRepository;

    // 1. Lấy danh sách toàn bộ banner
    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerRepository.findAll());
    }

    // 2. Thêm mới banner
    @HttpPost
    public ResponseEntity<Banner> createBanner(@RequestBody Banner banner) {
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    // 3. Cập nhật (Sửa) banner
    @PutMapping("/{id}")
    public ResponseEntity<Banner> updateBanner(@PathVariable Integer id, @RequestBody Banner bannerDetails) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        
        // Cập nhật chuẩn theo thuộc tính biến có dấu gạch dưới của bạn
        banner.setImage_url(bannerDetails.getImage_url());
        banner.setLink(bannerDetails.getLink());
        banner.setActive(bannerDetails.getActive());
        banner.setPosition(bannerDetails.getPosition());
        
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    // 4. Xóa banner
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Integer id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        
        bannerRepository.delete(banner);
        return ResponseEntity.ok().body("{\"message\": \"Xóa banner thành công!\"}");
    }
}