package com.dinhkhang.code.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.repository.UserRepository;

/**
 * Service để quản lý user mặc định khi không sử dụng authentication
 * Sử dụng cho môi trường local của giảng viên
 */
@Component
public class DefaultUserProvider {

    @Autowired
    private UserRepository userRepository;

    /**
     * Lấy giảng viên mặc định (giảng viên đầu tiên trong hệ thống)
     * Nếu không có giảng viên nào, tạo một giảng viên mới
     */
    public User getDefaultTeacher() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.Role.TEACHER)
                .findFirst()
                .orElseGet(() -> createDefaultTeacher());
    }

    /**
     * Tạo giảng viên mặc định nếu chưa có
     */
    private User createDefaultTeacher() {
        User teacher = new User();
        teacher.setUsername("teacher");
        teacher.setPassword("teacher123"); // Password sẽ được mã hóa tự động
        teacher.setFullName("Giảng Viên Mặc Định");
        teacher.setEmail("teacher@localhost");
        teacher.setRole(User.Role.TEACHER);
        teacher.setIsActive(true);
        return userRepository.save(teacher);
    }

    /**
     * Lấy học sinh mặc định (học sinh đầu tiên trong hệ thống)
     */
    public User getDefaultStudent() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.Role.STUDENT)
                .findFirst()
                .orElse(null);
    }
}
