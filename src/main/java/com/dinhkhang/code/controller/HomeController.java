package com.dinhkhang.code.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.dinhkhang.code.service.IUserService;

@Controller
public class HomeController {

    @Autowired
    private IUserService userService;

    @GetMapping("/")
    public String index() {
        // Chuyển hướng trực tiếp đến trang giảng viên vì không cần đăng nhập
        return "redirect:/teacher/dashboard";
    }

    // Các chức năng đăng ký và đăng nhập đã bị vô hiệu hóa
    // vì ứng dụng chạy local trên máy giảng viên

    @GetMapping("/dashboard")
    public String dashboard() {
        // Chuyển hướng mặc định đến trang giảng viên
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
