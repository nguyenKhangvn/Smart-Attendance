package com.dinhkhang.code.service;

import com.dinhkhang.code.entity.ClassSession;

import java.util.List;

public interface IClassSessionService {
    ClassSession createSession(ClassSession session, Long classId);

    ClassSession updateSession(Long id, ClassSession updatedSession);

    ClassSession startSession(Long id);

    ClassSession completeSession(Long id);

    List<ClassSession> getSessionsByClass(Long classId);

    ClassSession findById(Long id);

    ClassSession findByIdWithClassEntity(Long id);

    void deleteSession(Long id);
}
