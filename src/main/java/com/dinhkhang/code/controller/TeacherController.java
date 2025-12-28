package com.dinhkhang.code.controller;

import com.dinhkhang.code.dto.StudentImportDTO;
import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.ClassSession;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.service.ExcelImportService;
import com.dinhkhang.code.service.IClassService;
import com.dinhkhang.code.service.IClassSessionService;
import com.dinhkhang.code.service.IQRService;
import com.dinhkhang.code.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private IClassService classService;

    @Autowired
    private IClassSessionService classSessionService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IQRService qrService;

    @Autowired
    private ExcelImportService excelImportService;

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model, Authentication authentication) {
        User teacher = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        List<ClassEntity> classes = classService.getClassesByTeacher(teacher.getId());

        // Calculate statistics
        int totalStudents = classes.stream()
                .mapToInt(c -> c.getStudents().size())
                .sum();

        // Count today's sessions
        long todaySessions = classes.stream()
                .flatMap(c -> classSessionService.getSessionsByClass(c.getId()).stream())
                .filter(s -> s.getSessionDate().toLocalDate().equals(java.time.LocalDate.now()))
                .count();

        model.addAttribute("teacher", teacher);
        model.addAttribute("classes", classes);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("todaySessions", todaySessions);

        return "teacher/dashboard";
    }

    @GetMapping("/classes")
    public String listClasses(Model model, Authentication authentication) {
        User teacher = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        List<ClassEntity> classes = classService.getClassesByTeacher(teacher.getId());

        model.addAttribute("classes", classes);

        return "teacher/classes";
    }

    @PostMapping("/classes/create")
    public String createClass(
            @ModelAttribute("classEntity") ClassEntity classEntity,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User teacher = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));

            classService.createClass(classEntity, teacher.getId());

            redirectAttributes.addFlashAttribute("success", "Tạo lớp học thành công");
            return "redirect:/teacher/classes";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("classEntity", classEntity);
            return "redirect:/teacher/classes/create";
        }
    }



    @GetMapping("/classes/{id}")
    public String viewClass(@PathVariable Long id, Model model) {

        ClassEntity classEntity = classService.getClassDetail(id);

        model.addAttribute("classEntity", classEntity);
        model.addAttribute("students", classEntity.getStudents());
        model.addAttribute("sessions", classSessionService.getSessionsByClass(id));

        return "teacher/class-detail";
    }



    @GetMapping("/classes/{id}/edit")
    public String editClassForm(@PathVariable Long id, Model model) {
        ClassEntity classEntity = classService.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        model.addAttribute("classEntity", classEntity);

        return "teacher/class-form";
    }

    @PostMapping("/classes/{id}/edit")
    public String editClass(@PathVariable Long id, @ModelAttribute ClassEntity classEntity,
                           Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            // Use the secure method with ownership validation
            classService.updateClass(id, classEntity, teacher.getId());

            redirectAttributes.addFlashAttribute("success", "Cập nhật lớp học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/classes/" + id;
    }

    @GetMapping("/sessions/{id}/attendance")
    public String viewAttendance(@PathVariable Long id, Model model) {
        ClassSession session = classSessionService.findById(id);

        model.addAttribute("session", session);
        model.addAttribute("classEntity", session.getClassEntity());

        return "teacher/attendance";
    }

    @GetMapping("/sessions/{id}/qr")
    public String generateQR(@PathVariable Long id, Model model) {
        ClassSession session = classSessionService.findById(id);

        model.addAttribute("session", session);

        return "teacher/qr-scanner";
    }

    // Import students from Excel
    @PostMapping("/classes/{classId}/import-students")
    public String importStudents(@PathVariable Long classId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file Excel!");
                return "redirect:/teacher/classes/" + classId;
            }

            // Parse Excel file
            List<StudentImportDTO> studentDTOs = excelImportService.parseExcelFile(file);

            if (studentDTOs.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "File Excel không có dữ liệu hợp lệ!");
                return "redirect:/teacher/classes/" + classId;
            }

            // Import students
            List<User> importedStudents = excelImportService.importStudents(studentDTOs);

            // Add students to class
            for (User student : importedStudents) {
                try {
                    classService.addStudentToClass(classId, student.getId());
                } catch (Exception e) {
                    System.err.println("Error adding student to class: " + e.getMessage());
                }
            }

            redirectAttributes.addFlashAttribute("success",
                    "Đã import thành công " + importedStudents.size() + " học sinh!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi import file: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/teacher/classes/" + classId;
    }

    // Add single student to class
    @PostMapping("/classes/{classId}/add-student")
    public String addStudentToClass(@PathVariable Long classId,
            @RequestParam String studentCode,
            RedirectAttributes redirectAttributes) {
        try {
            List<User> students = userService.searchStudents(studentCode);

            if (students.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy học sinh với mã: " + studentCode);
                return "redirect:/teacher/classes/" + classId;
            }

            User student = students.get(0);
            classService.addStudentToClass(classId, student.getId());

            redirectAttributes.addFlashAttribute("success", "Đã thêm học sinh: " + student.getFullName());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/teacher/classes/" + classId;
    }

    // Remove student from class
    @PostMapping("/classes/{classId}/remove-student/{studentId}")
    public String removeStudentFromClass(@PathVariable Long classId,
            @PathVariable Long studentId,
            RedirectAttributes redirectAttributes) {
        try {
            classService.removeStudentFromClass(classId, studentId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa học sinh khỏi lớp!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/teacher/classes/" + classId;
    }

    // ==================== SESSIONS MANAGEMENT ====================

    @GetMapping("/sessions/create")
    public String showCreateSessionForm(@RequestParam Long classId, Model model) {
        ClassEntity classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        model.addAttribute("classEntity", classEntity);
        return "teacher/session-form";
    }

    @PostMapping("/sessions/create")
    public String createSession(@RequestParam Long classId,
            @RequestParam String sessionName,
            @RequestParam String sessionDate,
            @RequestParam String sessionTime,
            @RequestParam Integer durationMinutes,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "10") Integer allowedMinutesBefore,
            @RequestParam(defaultValue = "15") Integer allowedMinutesAfter,
            RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = classService.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

            // Parse date and time
            java.time.LocalDateTime sessionDateTime = java.time.LocalDateTime.parse(
                    sessionDate + "T" + sessionTime);

            ClassSession session = new ClassSession();
            session.setSessionName(sessionName);
            session.setSessionDate(sessionDateTime);
            session.setDurationMinutes(durationMinutes);
            session.setNotes(notes);
            session.setClassEntity(classEntity);

            classSessionService.createSession(session, classId);

            redirectAttributes.addFlashAttribute("success",
                    "Đã tạo buổi học thành công! Hãy tạo QR code để sinh viên điểm danh.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo buổi học: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/teacher/classes/" + classId;
    }
}
