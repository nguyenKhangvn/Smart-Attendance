package com.dinhkhang.code.controller;

import com.dinhkhang.code.entity.AttendanceRecord;
import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.service.IAttendanceService;
import com.dinhkhang.code.service.IClassService;
import com.dinhkhang.code.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private IClassService classService;

    @Autowired
    private IAttendanceService attendanceService;

    @Autowired
    private IUserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<ClassEntity> classes = classService.getClassesByStudent(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("classes", classes);

        return "student/dashboard";
    }

    @GetMapping("/classes")
    public String listClasses(Model model, Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<ClassEntity> classes = classService.getClassesByStudent(student.getId());

        model.addAttribute("classes", classes);

        return "student/classes";
    }

    @GetMapping("/classes/{id}")
    public String viewClass(@PathVariable Long id, Model model, Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        ClassEntity classEntity = classService.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        List<AttendanceRecord> attendanceRecords = attendanceService.getStudentAttendance(student.getId(), id);

        model.addAttribute("classEntity", classEntity);
        model.addAttribute("attendanceRecords", attendanceRecords);

        return "student/class-detail";
    }

    @GetMapping("/scan-qr")
    public String scanQR(Model model) {
        return "student/scan-qr";
    }

    @GetMapping("/attendance-history")
    public String attendanceHistory(Model model, Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<AttendanceRecord> records = attendanceService.getStudentAttendance(student.getId(), null);

        model.addAttribute("records", records);

        return "student/attendance-history";
    }
}


