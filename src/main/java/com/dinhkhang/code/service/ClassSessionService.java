package com.dinhkhang.code.service;

import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.ClassSession;
import com.dinhkhang.code.repository.ClassRepository;
import com.dinhkhang.code.repository.ClassSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ClassSessionService implements IClassSessionService {

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private ClassRepository classRepository;

    public ClassSession createSession(ClassSession session, Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        session.setClassEntity(classEntity);
        return classSessionRepository.save(session);
    }

    public ClassSession updateSession(Long id, ClassSession updatedSession) {
        ClassSession session = classSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setSessionName(updatedSession.getSessionName());
        session.setSessionDate(updatedSession.getSessionDate());
        session.setDurationMinutes(updatedSession.getDurationMinutes());
        session.setNotes(updatedSession.getNotes());
        session.setStatus(updatedSession.getStatus());

        return classSessionRepository.save(session);
    }

    public ClassSession startSession(Long id) {
        ClassSession session = classSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(ClassSession.SessionStatus.IN_PROGRESS);
        return classSessionRepository.save(session);
    }

    public ClassSession completeSession(Long id) {
        ClassSession session = classSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(ClassSession.SessionStatus.COMPLETED);
        return classSessionRepository.save(session);
    }

    public List<ClassSession> getSessionsByClass(Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        return classSessionRepository.findByClassEntityOrderBySessionDateDesc(classEntity);
    }

    public ClassSession findById(Long id) {
        return classSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    public ClassSession findByIdWithClassEntity(Long id) {
        return classSessionRepository.findByIdWithClassEntity(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    public void deleteSession(Long id) {
        classSessionRepository.deleteById(id);
    }
}
