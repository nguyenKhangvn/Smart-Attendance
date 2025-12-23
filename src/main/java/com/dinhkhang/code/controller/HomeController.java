package com.dinhkhang.code.controller;

import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    @Autowired
    private IUserService userService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        try {
            // Validate password match
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu và xác nhận mật khẩu không khớp!");
                return "redirect:/register";
            }

            // Validate role
            User.Role userRole;
            try {
                userRole = User.Role.valueOf(role);
                // Only allow STUDENT and TEACHER registration
                if (userRole != User.Role.STUDENT && userRole != User.Role.TEACHER) {
                    redirectAttributes.addFlashAttribute("error", "Vai trò không hợp lệ!");
                    return "redirect:/register";
                }
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Vai trò không hợp lệ!");
                return "redirect:/register";
            }

            // Create new user
            User user = new User();
            user.setFullName(fullName);
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);
            user.setRole(userRole);
            user.setIsActive(true);

            userService.createUser(user);

            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký thành công! Vui lòng đăng nhập để sử dụng hệ thống.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER"))) {
            return "redirect:/teacher/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT"))) {
            return "redirect:/student/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        return "redirect:/";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
