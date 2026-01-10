package com.dinhkhang.code.config;

import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
 * Component tự động seed dữ liệu ban đầu khi khởi động ứng dụng
 * Đảm bảo luôn có tài khoản admin để đăng nhập
 */
@Component
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Tự động chạy sau khi Spring khởi tạo bean
     * Tạo admin nếu chưa tồn tại
     */
    @PostConstruct
    @Transactional
    public void seedData() {
        seedAdminAccount();
    }

    /**
     * Tạo tài khoản admin mặc định
     * Username: admin
     * Password: 123456
     */
    private void seedAdminAccount() {
        try {
            // Kiểm tra xem đã có admin chưa
            if (userRepository.existsByRole(User.Role.ADMIN)) {
                logger.info(" Admin account already exists. Skipping seed.");
                return;
            }

            // Tạo admin mới
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456")); // Mã hóa password bằng BCrypt
            admin.setFullName("Quản Trị Viên Hệ Thống");
            admin.setEmail("admin@smartattendance.com");
            admin.setRole(User.Role.ADMIN);
            admin.setIsActive(true);
            admin.setIsDeleted(false);

            userRepository.save(admin);
            logger.info(" Admin account created successfully!");
            logger.info("   Username: admin");
            logger.info("   Password: 123456");
            logger.info("   Email: admin@smartattendance.com");

        } catch (Exception e) {
            logger.error(" Failed to seed admin account: " + e.getMessage(), e);
        }
    }
}
