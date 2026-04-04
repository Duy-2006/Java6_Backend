package com.poly.java5.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository) {
        return args -> {

            // Kiểm tra admin đã tồn tại chưa
            if (userRepository.findByUsername("admin") == null) {

                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword("123456"); // mật khẩu test
                admin.setEmail("admin@gmail.com");
                admin.setFullName("Administrator");
                admin.setPhone("0123456789");
                admin.setRole(UserRole.ADMIN);
                admin.setActive(true);

                userRepository.save(admin);

                System.out.println("✅ ADMIN account created!");
            }
        };
    }
}