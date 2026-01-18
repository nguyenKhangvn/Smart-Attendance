package com.dinhkhang.code.service;

import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.repository.ClassRepository;
import com.dinhkhang.code.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClassService implements IClassService {

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ClassEntity createClass(ClassEntity classEntity, Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("User is not a teacher");
        }

        if (classRepository.existsByClassCode(classEntity.getClassCode())) {
            throw new RuntimeException("Class code already exists");
        }

        classEntity.setTeacher(teacher);
        return classRepository.save(classEntity);
    }

    @Override
    public ClassEntity updateClass(Long id, ClassEntity updatedClass) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        classEntity.setSubjectName(updatedClass.getSubjectName());
        classEntity.setDescription(updatedClass.getDescription());
        classEntity.setSemester(updatedClass.getSemester());
        classEntity.setScheduleInfo(updatedClass.getScheduleInfo());

        return classRepository.save(classEntity);
    }

    @Override
    public ClassEntity updateClass(Long id, ClassEntity updatedClass, Long teacherId) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // SECURITY: Verify ownership
        if (!classEntity.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Access denied: You don't own this class");
        }

        classEntity.setSubjectName(updatedClass.getSubjectName());
        classEntity.setDescription(updatedClass.getDescription());
        classEntity.setSemester(updatedClass.getSemester());
        classEntity.setScheduleInfo(updatedClass.getScheduleInfo());

        return classRepository.save(classEntity);
    }

    @Override
    public void addStudentToClass(Long classId, Long studentId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        if (student.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }

        if (classEntity.getStudents().contains(student)) {
            return; // Already exists
        }

        classEntity.getStudents().add(student);
        classRepository.save(classEntity);
    }

    // SỬA: Đổi Long thành String cho studentCode để hỗ trợ mã dạng "HE14002"
    public void addStudentToClass(Long classId, String studentCode) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        User student = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Student not found with code: " + studentCode));

        if (student.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }

        // Hibernate sẽ tải list students lên. Với lớp học nhỏ thì OK.
        if (classEntity.getStudents().contains(student)) {
            return; // Đã tồn tại thì thôi
        }

        classEntity.getStudents().add(student);
        classRepository.save(classEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassEntity getClassDetail(Long id) {
        ClassEntity classEntity = classRepository.findByIdWithTeacher(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // classEntity.getStudents().size(); // Đã được fetch eagerly trong
        // findByIdWithTeacher

        return classEntity;
    }

    @Override
    public void removeStudentFromClass(Long classId, Long studentId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        classEntity.getStudents().remove(student);
        classRepository.save(classEntity);
    }

    @Override
    public Optional<ClassEntity> findById(Long id) {
        return classRepository.findById(id);
    }

    @Override
    public List<ClassEntity> getClassesByTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        return classRepository.findByTeacherAndIsActive(teacher, true);
    }

    @Override
    public List<ClassEntity> getClassesByStudent(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return classRepository.findActiveClassesByStudent(student);
    }

    @Override
    public void deleteClass(Long id) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        classEntity.setIsActive(false);
        classRepository.save(classEntity);
    }

    @Override
    public void deleteClass(Long id, Long teacherId) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // SECURITY: Verify ownership
        if (!classEntity.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Access denied: You don't own this class");
        }

        classEntity.setIsActive(false);
        classRepository.save(classEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassEntity> getAllClasses() {
        return classRepository.findAllWithTeacherAndStudents();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassEntity> findByIdWithTeacher(Long id) {
        return classRepository.findByIdWithTeacher(id);
    }

    @Override
    public void activateClass(Long id) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        classEntity.setIsActive(true);
        classRepository.save(classEntity);
    }

    @Override
    public void deactivateClass(Long id) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        classEntity.setIsActive(false);
        classRepository.save(classEntity);
    }

    // SỬA LỖI PHÂN TRANG (Quan trọng nhất)
    @Override
    @Transactional(readOnly = true)
    public Page<ClassEntity> getAllClasses(Pageable pageable) {
        // Cần đảm bảo Repository có hàm này (Không fetch students)
        // Nếu chưa có, hãy vào ClassRepository thêm hàm bên dưới
        return classRepository.findAllWithTeacherOnly(pageable);
    }

    @Override
    public long countStudentsInClass(Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));
        return classEntity.getStudents() != null ? classEntity.getStudents().size() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassEntity> findAllWithTeacherOnly(Pageable pageable) {
        return classRepository.findAllWithTeacherOnly(pageable);
    }
}
