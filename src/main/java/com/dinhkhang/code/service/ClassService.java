package com.dinhkhang.code.service;

import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.repository.ClassRepository;
import com.dinhkhang.code.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClassService implements IClassService {

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

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

    public ClassEntity updateClass(Long id, ClassEntity updatedClass) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        classEntity.setSubjectName(updatedClass.getSubjectName());
        classEntity.setDescription(updatedClass.getDescription());
        classEntity.setSemester(updatedClass.getSemester());
        classEntity.setScheduleInfo(updatedClass.getScheduleInfo());

        return classRepository.save(classEntity);
    }

    public void addStudentToClass(Long classId, Long studentId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }

        classEntity.getStudents().add(student);
        classRepository.save(classEntity);
    }

    public void removeStudentFromClass(Long classId, Long studentId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        classEntity.getStudents().remove(student);
        classRepository.save(classEntity);
    }

    public Optional<ClassEntity> findById(Long id) {
        return classRepository.findById(id);
    }

    public List<ClassEntity> getClassesByTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        return classRepository.findByTeacherAndIsActive(teacher, true);
    }

    public List<ClassEntity> getClassesByStudent(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return classRepository.findActiveClassesByStudent(student);
    }

    public void deleteClass(Long id) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        classEntity.setIsActive(false);
        classRepository.save(classEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassEntity> getAllClasses() {
        return classRepository.findAll();
    }
}
