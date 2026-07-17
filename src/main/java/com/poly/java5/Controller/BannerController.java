package com.poly.java5.Controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.poly.java5.Entity.Banner;
import com.poly.java5.Repository.BannerRepository;
import com.poly.java5.Service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private Cloudinary cloudinary; // Tiêm Bean Cloudinary vừa cấu hình ở Bước 3

    // Hàm upload ảnh lên Cloudinary và lấy URL về
    private String uploadToCloudinary(MultipartFile file) throws IOException {
        // Cấu hình thư mục lưu trữ trên Cloudinary là "libris/banners" để dễ quản lý
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
            "folder", "libris/banners"
        ));
        return uploadResult.get("secure_url").toString(); // Trả về link https tuyệt đối
    }

    // 1. Lấy danh sách cho Admin
    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerRepository.findAllByOrderByPositionAsc());
    }

    // 2. API mới bổ sung dành riêng cho trang chủ Next.js Client
    @GetMapping("/active")
    public ResponseEntity<List<Banner>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    // 3. Thêm mới banner
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createBanner(
            @RequestParam(value = "image_url", required = false) String imageUrl,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "position", defaultValue = "0") Integer position,
            @RequestParam(value = "active", defaultValue = "true") Boolean active,
            @RequestParam(value = "start_date", required = false) String startDateStr,
            @RequestParam(value = "end_date", required = false) String endDateStr,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        
        Banner banner = new Banner();
        banner.setLink(link);
        banner.setPosition(position);
        banner.setActive(active);
        
        if (startDateStr != null && !startDateStr.isEmpty()) {
            banner.setStart_date(java.time.LocalDateTime.parse(startDateStr));
        }
        if (endDateStr != null && !endDateStr.isEmpty()) {
            banner.setEnd_date(java.time.LocalDateTime.parse(endDateStr));
        }

        // Thay đổi ở đây: Upload thẳng lên Cloudinary
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String cloudinaryUrl = uploadToCloudinary(imageFile);
                banner.setImage_url(cloudinaryUrl); // URL lưu vào DB bây giờ là link https://res.cloudinary...
            } catch (IOException e) {
                return ResponseEntity.status(500).body("{\"message\": \"Lỗi upload lên Cloudinary: " + e.getMessage() + "\"}");
            }
        } else {
            banner.setImage_url(imageUrl);
        }
        
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    // 4. Cập nhật (Sửa) banner
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateBanner(
            @PathVariable Integer id,
            @RequestParam(value = "image_url", required = false) String imageUrl,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "position", defaultValue = "0") Integer position,
            @RequestParam(value = "active", defaultValue = "true") Boolean active,
            @RequestParam(value = "start_date", required = false) String startDateStr,
            @RequestParam(value = "end_date", required = false) String endDateStr,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
            
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        
        banner.setLink(link);
        banner.setPosition(position);
        banner.setActive(active);
        
        if (startDateStr != null && !startDateStr.isEmpty()) {
            banner.setStart_date(java.time.LocalDateTime.parse(startDateStr));
        } else {
            banner.setStart_date(null);
        }
        
        if (endDateStr != null && !endDateStr.isEmpty()) {
            banner.setEnd_date(java.time.LocalDateTime.parse(endDateStr));
        } else {
            banner.setEnd_date(null);
        }

        // Thay đổi ở đây: Upload thẳng lên Cloudinary khi cập nhật ảnh mới
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String cloudinaryUrl = uploadToCloudinary(imageFile);
                banner.setImage_url(cloudinaryUrl);
            } catch (IOException e) {
                return ResponseEntity.status(500).body("{\"message\": \"Lỗi upload lên Cloudinary: " + e.getMessage() + "\"}");
            }
        } else {
            banner.setImage_url(imageUrl);
        }
        
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    // 5. Xóa banner
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Integer id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        
        bannerRepository.delete(banner);
        return ResponseEntity.ok().body("{\"message\": \"Xóa banner thành công!\"}");
    }
}