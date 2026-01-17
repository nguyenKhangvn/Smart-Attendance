package com.dinhkhang.code.controller;

import com.dinhkhang.code.entity.AttendanceRecord;
import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.service.IAttendanceService;
import com.dinhkhang.code.service.IClassService;
import com.dinhkhang.code.service.IUserService;
import com.dinhkhang.code.service.PaginationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @Autowired
    private PaginationService paginationService;

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

        ClassEntity classEntity = classService.findByIdWithTeacher(id)
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
    public String attendanceHistory(Model model, Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "checkedInAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Page<AttendanceRecord> recordsPage = attendanceService.getStudentAttendancePage(student.getId(), null,
                paginationService.createPageable(page, size, sortBy, sortDir));

        model.addAttribute("recordsPage", recordsPage);
        model.addAttribute("records", recordsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "student/attendance-history";
    }
}
