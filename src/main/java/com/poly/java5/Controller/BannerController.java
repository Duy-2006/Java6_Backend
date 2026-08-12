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

    // 2.5. Lấy chi tiết một banner (để hiển thị lên Form khi chỉnh sửa)
    @GetMapping("/{id}")
    public ResponseEntity<Banner> getBannerById(@PathVariable Integer id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        return ResponseEntity.ok(banner);
    }

    // 3. Thêm mới banner
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
        
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên banner không được để trống."));
        }
        if ((imageFile == null || imageFile.isEmpty()) && (imageUrl == null || imageUrl.trim().isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng tải lên hình ảnh hoặc nhập URL hình ảnh."));
        }
        if (startDateStr == null || startDateStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn ngày bắt đầu."));
        }
        if (endDateStr == null || endDateStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn ngày kết thúc."));
        }

        java.time.LocalDate startDate;
        java.time.LocalDate endDate;
        try {
            startDate = java.time.LocalDate.parse(startDateStr.substring(0, 10));
            endDate = java.time.LocalDate.parse(endDateStr.substring(0, 10));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Định dạng ngày không hợp lệ."));
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        if (startDate.isBefore(today)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày bắt đầu không được nằm trong quá khứ."));
        }
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày kết thúc không được trước ngày bắt đầu."));
        }

        Banner banner = new Banner();
        banner.setTitle(title.trim());
        banner.setDescription(description);
        banner.setLink(link);
        banner.setPosition(position != null ? position : 0);
        banner.setActive(active != null ? active : true);
        banner.setStart_date(startDate);
        banner.setEnd_date(endDate);

        // Thay đổi ở đây: Upload thẳng lên Cloudinary
        if (imageFile != null && !imageFile.isEmpty()) {
            if (imageFile.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("message", "Dung lượng ảnh vượt quá giới hạn cho phép (tối đa 5MB)."));
            }
            String contentType = imageFile.getContentType();
            if (contentType != null && !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tệp tải lên không phải là định dạng hình ảnh hợp lệ."));
            }
            try {
                String cloudinaryUrl = uploadToCloudinary(imageFile);
                banner.setImage_url(cloudinaryUrl); // URL lưu vào DB bây giờ là link https://res.cloudinary...
            } catch (IOException e) {
                return ResponseEntity.status(500).body(Map.of("message", "Lỗi upload lên Cloudinary: " + e.getMessage()));
            }
        } else {
            banner.setImage_url(imageUrl);
        }
        
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    // 4. Cập nhật (Sửa) banner
    @PostMapping(value = "/{id}", consumes = "multipart/form-data")
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
        
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên banner không được để trống."));
        }
        if ((imageFile == null || imageFile.isEmpty()) && (imageUrl == null || imageUrl.trim().isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng tải lên hình ảnh hoặc nhập URL hình ảnh."));
        }
        if (startDateStr == null || startDateStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn ngày bắt đầu."));
        }
        if (endDateStr == null || endDateStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn ngày kết thúc."));
        }

        java.time.LocalDate startDate;
        java.time.LocalDate endDate;
        try {
            startDate = java.time.LocalDate.parse(startDateStr.substring(0, 10));
            endDate = java.time.LocalDate.parse(endDateStr.substring(0, 10));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Định dạng ngày không hợp lệ."));
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        if (startDate.isBefore(today) && (banner.getStart_date() == null || !startDate.equals(banner.getStart_date()))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày bắt đầu không được nằm trong quá khứ."));
        }
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày kết thúc không được trước ngày bắt đầu."));
        }

        banner.setTitle(title.trim());
        banner.setDescription(description);
        banner.setLink(link);
        banner.setPosition(position != null ? position : 0);
        banner.setActive(active != null ? active : true);
        banner.setStart_date(startDate);
        banner.setEnd_date(endDate);

        // Thay đổi ở đây: Upload thẳng lên Cloudinary khi cập nhật ảnh mới
        if (imageFile != null && !imageFile.isEmpty()) {
            if (imageFile.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("message", "Dung lượng ảnh vượt quá giới hạn cho phép (tối đa 5MB)."));
            }
            String contentType = imageFile.getContentType();
            if (contentType != null && !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tệp tải lên không phải là định dạng hình ảnh hợp lệ."));
            }
            try {
                String cloudinaryUrl = uploadToCloudinary(imageFile);
                banner.setImage_url(cloudinaryUrl);
            } catch (IOException e) {
                return ResponseEntity.status(500).body(Map.of("message", "Lỗi upload lên Cloudinary: " + e.getMessage()));
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

    // 6. Bật / Tắt (Ẩn / Hiện) trạng thái banner nhanh
    @RequestMapping(value = "/{id}/toggle", method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> toggleBannerStatus(@PathVariable Integer id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner có ID: " + id));
        
        banner.setActive(!Boolean.TRUE.equals(banner.getActive()));
        Banner saved = bannerRepository.save(banner);
        return ResponseEntity.ok(saved);
    }
}