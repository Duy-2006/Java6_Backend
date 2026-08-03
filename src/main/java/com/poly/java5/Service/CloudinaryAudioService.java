package com.poly.java5.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Dịch vụ tải file audio MP3 từ URL bên ngoài (FPT.AI) lên Cloudinary.
 * Sau khi upload thành công, trả về URL vĩnh viễn của Cloudinary để lưu vào DB.
 */
@Service
@RequiredArgsConstructor
public class CloudinaryAudioService {

    private final Cloudinary cloudinary;

    /**
     * Tải file MP3 từ URL của FPT.AI lên Cloudinary.
     *
     * @param fptAudioUrl URL tạm thời từ FPT.AI (dạng https://...mp3)
     * @param publicId    Tên định danh file trên Cloudinary (VD:
     *                    "audiobooks/chapter_5_1716900000000")
     * @return URL vĩnh viễn Cloudinary (https://res.cloudinary.com/...)
     * @throws Exception nếu download hoặc upload thất bại
     */
    public String downloadAndUploadToCloudinary(String fptAudioUrl, String publicId) throws Exception {
        System.out.println("☁️ [Cloudinary] Bắt đầu tải file từ: " + fptAudioUrl);

        // 1. Tải stream MP3 từ FPT.AI về
        HttpURLConnection conn = (HttpURLConnection) new URL(fptAudioUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);
        conn.connect();

        if (conn.getResponseCode() != 200) {
            throw new Exception("FPT.AI trả về HTTP " + conn.getResponseCode() + " — Chưa sẵn sàng để tải.");
        }

        // 2. Đọc toàn bộ dữ liệu MP3 vào byte[]
        byte[] audioBytes;
        try (InputStream in = conn.getInputStream()) {
            audioBytes = in.readAllBytes();
        }

        if (audioBytes == null || audioBytes.length == 0) {
            throw new Exception("File MP3 tải về rỗng (0 bytes).");
        }
        System.out.printf("☁️ [Cloudinary] Đã tải về %.1f KB, đang upload lên Cloudinary...%n",
                audioBytes.length / 1024.0);

        // 3. Upload lên Cloudinary với resource_type=video (Cloudinary xử lý audio qua
        // video)
        Map<?, ?> result = cloudinary.uploader().upload(
                audioBytes,
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "resource_type", "video", // Cloudinary dùng "video" cho cả audio
                        "overwrite", true,
                        "folder", "", // public_id đã bao gồm folder
                        "format", "mp3"));

        String secureUrl = (String) result.get("secure_url");
        if (secureUrl == null || secureUrl.isBlank()) {
            throw new Exception("Cloudinary không trả về URL sau khi upload.");
        }

        System.out.println("✅ [Cloudinary] Upload thành công: " + secureUrl);
        return secureUrl;
    }
}
