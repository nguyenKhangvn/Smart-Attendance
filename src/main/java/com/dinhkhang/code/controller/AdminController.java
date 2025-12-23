package com.dinhkhang.code.controller;

import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.service.IClassService;
import com.dinhkhang.code.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IClassService classService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> teachers = userService.getUsersByRole(User.Role.TEACHER);
        List<User> students = userService.getUsersByRole(User.Role.STUDENT);

        // Count active classes using service method
        long classCount = classService.getAllClasses().stream()
                .filter(ClassEntity::getIsActive)
                .count();

        model.addAttribute("teachers", teachers);
        model.addAttribute("students", students);
        model.addAttribute("teacherCount", teachers.size());
        model.addAttribute("studentCount", students.size());
        model.addAttribute("classCount", classCount);

        return "admin/dashboard";
    }

    // ==================== TEACHERS CRUD ====================

    @GetMapping("/teachers")
    public String listTeachers(Model model) {
        List<User> teachers = userService.getUsersByRole(User.Role.TEACHER);
        model.addAttribute("teachers", teachers);
        return "admin/teachers";
    }

    @GetMapping("/teachers/create")
    public String showCreateTeacherForm(Model model) {
        model.addAttribute("teacher", new User());
        return "admin/teacher-form";
    }

    @PostMapping("/teachers/create")
    public String createTeacher(@ModelAttribute User teacher, RedirectAttributes redirectAttributes) {
        try {
            teacher.setRole(User.Role.TEACHER);
            teacher.setIsActive(true);
            userService.createUser(teacher);
            redirectAttributes.addFlashAttribute("success", "Tạo giáo viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @GetMapping("/teachers/edit/{id}")
    public String showEditTeacherForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));

            if (teacher.getRole() != User.Role.TEACHER) {
                redirectAttributes.addFlashAttribute("error", "User này không phải giáo viên!");
                return "redirect:/admin/teachers";
            }

            model.addAttribute("teacher", teacher);
            return "admin/teacher-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/teachers";
        }
    }

    @PostMapping("/teachers/update/{id}")
    public String updateTeacher(@PathVariable Long id, @ModelAttribute User updatedTeacher,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, updatedTeacher);
            redirectAttributes.addFlashAttribute("success", "Cập nhật giáo viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @PostMapping("/teachers/delete/{id}")
    public String deleteTeacher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Xóa giáo viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @PostMapping("/teachers/toggle-status/{id}")
    public String toggleTeacherStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));

            if (teacher.getIsActive()) {
                userService.deactivateUser(id);
                redirectAttributes.addFlashAttribute("success", "Đã vô hiệu hóa giáo viên!");
            } else {
                teacher.setIsActive(true);
                userService.updateUser(id, teacher);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt giáo viên!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    // ==================== STUDENTS CRUD ====================

    @GetMapping("/students")
    public String listStudents(Model model) {
        List<User> students = userService.getUsersByRole(User.Role.STUDENT);
        model.addAttribute("students", students);
        return "admin/students";
    }

    @GetMapping("/students/create")
    public String showCreateStudentForm(Model model) {
        model.addAttribute("student", new User());
        return "admin/student-form";
    }

    @PostMapping("/students/create")
    public String createStudent(@ModelAttribute User student, RedirectAttributes redirectAttributes) {
        try {
            student.setRole(User.Role.STUDENT);
            student.setIsActive(true);
            userService.createUser(student);
            redirectAttributes.addFlashAttribute("success", "Tạo học sinh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/students/edit/{id}")
    public String showEditStudentForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

            if (student.getRole() != User.Role.STUDENT) {
                redirectAttributes.addFlashAttribute("error", "User này không phải học sinh!");
                return "redirect:/admin/students";
            }

            model.addAttribute("student", student);
            return "admin/student-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/students";
        }
    }

    @PostMapping("/students/update/{id}")
    public String updateStudent(@PathVariable Long id, @ModelAttribute User updatedStudent,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, updatedStudent);
            redirectAttributes.addFlashAttribute("success", "Cập nhật học sinh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Xóa học sinh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/toggle-status/{id}")
    public String toggleStudentStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

            if (student.getIsActive()) {
                userService.deactivateUser(id);
                redirectAttributes.addFlashAttribute("success", "Đã vô hiệu hóa học sinh!");
            } else {
                student.setIsActive(true);
                userService.updateUser(id, student);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt học sinh!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    // ==================== CLASSES CRUD ====================

    @GetMapping("/classes")
    public String listClasses(Model model) {
        // Get all classes directly from service
        List<ClassEntity> allClasses = classService.getAllClasses();

        model.addAttribute("classes", allClasses);
        return "admin/classes";
    }

    @GetMapping("/classes/create")
    public String showCreateClassForm(Model model) {
        List<User> teachers = userService.getUsersByRole(User.Role.TEACHER);
        model.addAttribute("classEntity", new ClassEntity());
        model.addAttribute("teachers", teachers);
        return "admin/class-form";
    }

    @PostMapping("/classes/create")
    public String createClass(@ModelAttribute ClassEntity classEntity,
            @RequestParam Long teacherId,
            RedirectAttributes redirectAttributes) {
        try {
            classService.createClass(classEntity, teacherId);
            redirectAttributes.addFlashAttribute("success", "Tạo lớp học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @GetMapping("/classes/edit/{id}")
    public String showEditClassForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = classService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

            List<User> teachers = userService.getUsersByRole(User.Role.TEACHER);

            model.addAttribute("classEntity", classEntity);
            model.addAttribute("teachers", teachers);
            return "admin/class-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/classes";
        }
    }

    @PostMapping("/classes/update/{id}")
    public String updateClass(@PathVariable Long id,
            @ModelAttribute ClassEntity updatedClass,
            RedirectAttributes redirectAttributes) {
        try {
            classService.updateClass(id, updatedClass);
            redirectAttributes.addFlashAttribute("success", "Cập nhật lớp học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/classes/delete/{id}")
    public String deleteClass(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            classService.deleteClass(id);
            redirectAttributes.addFlashAttribute("success", "Xóa lớp học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/classes/toggle-status/{id}")
    public String toggleClassStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = classService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

            classEntity.setIsActive(!classEntity.getIsActive());
            classService.updateClass(id, classEntity);

            String message = classEntity.getIsActive() ? "Đã kích hoạt lớp học!" : "Đã tạm dừng lớp học!";
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @GetMapping("/classes/{id}")
    public String viewClassDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = classService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

            model.addAttribute("classEntity", classEntity);
            model.addAttribute("students", classEntity.getStudents());
            return "admin/class-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/classes";
        }
    }
}
