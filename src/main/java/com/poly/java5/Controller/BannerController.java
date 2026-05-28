package com.poly.java5.Controller;

import com.poly.java5.Entity.Banner;
import com.poly.java5.Repository.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/banners")
@CrossOrigin("*") // Cho phép Frontend Next.js gọi API không bị lỗi CORS
public class BannerController {

    @Autowired
    private BannerRepository bannerRepository;

    // Định nghĩa thư mục lưu trữ file trên máy tính (nằm trong thư mục dự án)
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/banners/";

    // 1. Lấy danh sách toàn bộ banner (Đã tối ưu: Tự động sắp xếp theo thứ tự hiển thị)
    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerRepository.findAllByOrderByPositionAsc());
    }

    // 1.5 API Tải ảnh lên hệ thống (Phục vụ nút bấm "Tải ảnh từ thiết bị" ở Frontend)
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"message\": \"Vui lòng chọn file để tải lên!\"}");
        }
        try {
            // Tạo thư mục lưu trữ nếu chưa tồn tại
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Đổi tên file sang chuỗi ngẫu nhiên bằng UUID để tránh bị trùng lặp tên file
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + extension;

            // Lưu file vào thư mục vật lý trên ổ đĩa
            Path path = Paths.get(UPLOAD_DIR + newFileName);
            Files.write(path, file.getBytes());

            // Trả về đường dẫn ngắn chuẩn cấu trúc Json cho Frontend Next.js nhận diện
            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", "/uploads/banners/" + newFileName);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"Lỗi hệ thống khi lưu tệp tin!\"}");
        }
    }

    // 2. Thêm mới thông tin banner vào Database
    @PostMapping
    public ResponseEntity<Banner> createBanner(@RequestBody Banner banner) {
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    // 3. Cập nhật (Sửa) banner
    @PutMapping("/{id}")
    public ResponseEntity<Banner> updateBanner(@PathVariable Integer id, @RequestBody Banner bannerDetails) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        
        // Cập nhật chuẩn theo các thuộc tính entity của bạn
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