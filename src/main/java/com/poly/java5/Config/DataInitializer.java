package com.poly.java5.Config;

import com.poly.java5.Entity.Publisher;
import com.poly.java5.Repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PublisherRepository publisherRepository;

    @Override
    public void run(String... args) throws Exception {
        // MỤC TIÊU 2: Đảm bảo luôn có ít nhất 1 Nhà xuất bản mặc định trong hệ thống khi khởi động
        if (publisherRepository.count() == 0) {
            Publisher defaultPublisher = new Publisher();
            defaultPublisher.setName("Nhà Xuất Bản Mặc Định");
            // defaultPublisher.setAddress("Cần Thơ, Việt Nam"); // Mở comment nếu Entity có thuộc tính address
            // defaultPublisher.setPhone("0901234567");        // Mở comment nếu Entity có thuộc tính phone
            
            publisherRepository.save(defaultPublisher);
            log.info("✅ [DataInitializer] Đã khởi tạo Nhà xuất bản mặc định duy nhất thành công!");
        }
    }
}