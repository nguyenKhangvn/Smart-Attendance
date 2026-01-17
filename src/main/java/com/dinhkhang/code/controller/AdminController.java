package com.dinhkhang.code.controller;

import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.service.IClassService;
import com.dinhkhang.code.service.IUserService;
import com.dinhkhang.code.service.PaginationService;
import com.dinhkhang.code.service.ExcelImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IClassService classService;

    @Autowired
    private PaginationService paginationService;

    @Autowired
    private ExcelImportService excelImportService;

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
    @Transactional(readOnly = true)
    public String listTeachers(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<User> teacherPage = userService.getUsersByRole(User.Role.TEACHER,
                paginationService.createPageable(page, size, sortBy, sortDir));
        model.addAttribute("teacherPage", teacherPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
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
                userService.activateUser(id);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt giáo viên!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    // ==================== STUDENTS CRUD ====================

    @GetMapping("/students")
    @Transactional(readOnly = true)
    public String listStudents(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<User> studentPage = userService.getUsersByRole(User.Role.STUDENT,
                paginationService.createPageable(page, size, sortBy, sortDir));
        model.addAttribute("studentPage", studentPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
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
                userService.activateUser(id);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt học sinh!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/import-excel")
    public String importStudentsFromExcel(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file Excel!");
                return "redirect:/admin/students";
            }

            Map<String, Object> result = excelImportService.importStudentsFromExcel(file);
            @SuppressWarnings("unchecked")
            List<User> students = (List<User>) result.get("students");
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");
            int successCount = (int) result.get("successCount");
            int errorCount = (int) result.get("errorCount");

            // Save valid students
            int savedCount = 0;
            List<String> saveErrors = new ArrayList<>();
            for (User student : students) {
                try {
                    userService.createUser(student);
                    savedCount++;
                } catch (Exception e) {
                    saveErrors.add("Mã SV " + student.getStudentCode() + ": " + e.getMessage());
                }
            }

            // Build result message
            StringBuilder message = new StringBuilder();
            message.append("Import hoàn tất! ");
            message.append("Thành công: ").append(savedCount).append(", ");
            message.append("Lỗi: ").append(errorCount + saveErrors.size());

            if (!errors.isEmpty() || !saveErrors.isEmpty()) {
                message.append("\n\nChi tiết lỗi:\n");
                errors.forEach(err -> message.append("- ").append(err).append("\n"));
                saveErrors.forEach(err -> message.append("- ").append(err).append("\n"));
            }

            if (savedCount > 0) {
                redirectAttributes.addFlashAttribute("success", message.toString());
            } else {
                redirectAttributes.addFlashAttribute("error", message.toString());
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi đọc file Excel: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    // ==================== CLASSES CRUD ====================

    @GetMapping("/classes")
    @Transactional(readOnly = true)
    public String listClasses(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<ClassEntity> classPage = classService
                .getAllClasses(paginationService.createPageable(page, size, sortBy, sortDir));
        model.addAttribute("classPage", classPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
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
            ClassEntity classEntity = classService.findByIdWithTeacher(id)
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

            if (classEntity.getIsActive()) {
                classService.deactivateClass(id);
                redirectAttributes.addFlashAttribute("success", "Đã tạm dừng lớp học!");
            } else {
                classService.activateClass(id);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt lớp học!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @GetMapping("/classes/detail/{id}")
    public String viewClassDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = classService.findByIdWithTeacher(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

            // Get all active students for adding to class
            List<User> allStudents = userService.getUsersByRole(User.Role.STUDENT)
                    .stream()
                    .filter(User::getIsActive)
                    .toList();

            // Filter out students already in the class
            List<User> availableStudents = allStudents.stream()
                    .filter(student -> !classEntity.getStudents().contains(student))
                    .toList();

            model.addAttribute("classEntity", classEntity);
            model.addAttribute("students", classEntity.getStudents());
            model.addAttribute("availableStudents", availableStudents);
            return "admin/class-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/classes";
        }
    }

    @PostMapping("/classes/{classId}/add-student")
    public String addStudentToClass(@PathVariable Long classId, @RequestParam Long studentId,
            RedirectAttributes redirectAttributes) {
        try {
            classService.addStudentToClass(classId, studentId);
            redirectAttributes.addFlashAttribute("success", "Đã thêm học sinh vào lớp!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/classes/" + classId;
    }

    @PostMapping("/classes/{classId}/remove-student/{studentId}")
    public String removeStudentFromClass(@PathVariable Long classId, @PathVariable Long studentId,
            RedirectAttributes redirectAttributes) {
        try {
            classService.removeStudentFromClass(classId, studentId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa học sinh khỏi lớp!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/classes/" + classId;
    }

    @PostMapping("/classes/{classId}/import-students")
    public String importStudentsToClass(@PathVariable Long classId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file Excel!");
                return "redirect:/admin/classes/detail/" + classId;
            }

            ClassEntity classEntity = classService.findByIdWithTeacher(classId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

            Map<String, Object> result = excelImportService.importStudentCodesFromExcel(file);
            @SuppressWarnings("unchecked")
            List<String> studentCodes = (List<String>) result.get("studentCodes");
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");

            int addedCount = 0;
            List<String> addErrors = new ArrayList<>();

            for (String studentCode : studentCodes) {
                try {
                    User student = userService.findByStudentCode(studentCode)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên mã " + studentCode));

                    // Check if student already in class
                    if (classEntity.getStudents().contains(student)) {
                        addErrors.add("Mã SV " + studentCode + ": Đã có trong lớp");
                        continue;
                    }

                    classService.addStudentToClass(classId, student.getId());
                    addedCount++;
                } catch (Exception e) {
                    addErrors.add("Mã SV " + studentCode + ": " + e.getMessage());
                }
            }

            // Build result message
            StringBuilder message = new StringBuilder();
            message.append("Import hoàn tất! ");
            message.append("Đã thêm: ").append(addedCount).append(", ");
            message.append("Lỗi: ").append(errors.size() + addErrors.size());

            if (!errors.isEmpty() || !addErrors.isEmpty()) {
                message.append("\n\nChi tiết lỗi:\n");
                errors.forEach(err -> message.append("- ").append(err).append("\n"));
                addErrors.forEach(err -> message.append("- ").append(err).append("\n"));
            }

            if (addedCount > 0) {
                redirectAttributes.addFlashAttribute("success", message.toString());
            } else {
                redirectAttributes.addFlashAttribute("error", message.toString());
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi đọc file Excel: " + e.getMessage());
        }
        return "redirect:/admin/classes/detail/" + classId;
    }
}
