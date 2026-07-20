package com.poly.java5.Controller;

import com.poly.java5.Entity.Banner;
import com.poly.java5.Repository.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

import java.util.UUID;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    @Autowired
    private BannerRepository bannerRepository;


    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/banners/";

    private java.time.LocalDateTime parseDateString(String dateStr, boolean isEnd) {
        if (dateStr == null) return null;
        dateStr = dateStr.trim();
        if (dateStr.isEmpty() || dateStr.equalsIgnoreCase("null") || dateStr.equalsIgnoreCase("undefined")) {
            return null;
        }
        try {
            if (dateStr.length() == 10) {
                if (isEnd) {
                    return java.time.LocalDate.parse(dateStr).atTime(23, 59, 59);
                } else {
                    return java.time.LocalDate.parse(dateStr).atStartOfDay();
                }
            } else {
                if (dateStr.contains(" ") && !dateStr.contains("T")) {
                    dateStr = dateStr.replace(" ", "T");
                }
                return java.time.LocalDateTime.parse(dateStr);
            }
        } catch (Exception e) {
            System.err.println("Error parsing date: " + dateStr + " - " + e.getMessage());
            return null;
        }
    }

    private String saveImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        return filename;
    }

    // 1. Lấy danh sách toàn bộ banner
    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerRepository.findAllByOrderByPositionAsc());
    }

    // 1.5. Lấy chi tiết banner bằng ID
    @GetMapping("/{id}")
    public ResponseEntity<Banner> getBannerById(@PathVariable Integer id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        return ResponseEntity.ok(banner);
    }

    // 2. Thêm mới banner
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createBanner(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image_url", required = false) String imageUrl,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "position", defaultValue = "0") Integer position,
            @RequestParam(value = "active", defaultValue = "true") Boolean active,
            @RequestParam(value = "start_date", required = false) String startDateStr,
            @RequestParam(value = "end_date", required = false) String endDateStr,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        
        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setDescription(description);
        banner.setLink(link);
        banner.setPosition(position);
        banner.setActive(active);
        
        banner.setStart_date(parseDateString(startDateStr, false));
        banner.setEnd_date(parseDateString(endDateStr, true));

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String filename = saveImage(imageFile);
                banner.setImage_url("/uploads/banners/" + filename);
            } catch (IOException e) {
                return ResponseEntity.status(500).body("{\"message\": \"Lỗi lưu ảnh: " + e.getMessage() + "\"}");
            }
        } else {
            banner.setImage_url(imageUrl);
        }
        
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    // 3. Cập nhật (Sửa) banner
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateBanner(
            @PathVariable Integer id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image_url", required = false) String imageUrl,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "position", defaultValue = "0") Integer position,
            @RequestParam(value = "active", defaultValue = "true") Boolean active,
            @RequestParam(value = "start_date", required = false) String startDateStr,
            @RequestParam(value = "end_date", required = false) String endDateStr,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
            
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        
        banner.setTitle(title);
        banner.setDescription(description);
        banner.setLink(link);
        banner.setPosition(position);
        banner.setActive(active);
        
        banner.setStart_date(parseDateString(startDateStr, false));
        banner.setEnd_date(parseDateString(endDateStr, true));

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String filename = saveImage(imageFile);
                banner.setImage_url("/uploads/banners/" + filename);
            } catch (IOException e) {
                return ResponseEntity.status(500).body("{\"message\": \"Lỗi lưu ảnh: " + e.getMessage() + "\"}");
            }
        } else {
            banner.setImage_url(imageUrl);
        }
        
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