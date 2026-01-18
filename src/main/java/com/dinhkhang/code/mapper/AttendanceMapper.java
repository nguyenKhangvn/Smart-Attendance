package com.dinhkhang.code.mapper;

import com.dinhkhang.code.dto.AttendanceRecordDTO;
import com.dinhkhang.code.dto.ClassEntityDTO;
import com.dinhkhang.code.dto.ClassSessionDTO;
import com.dinhkhang.code.dto.StudentDTO;
import com.dinhkhang.code.entity.AttendanceRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AttendanceMapper {

    public AttendanceRecordDTO toDTO(AttendanceRecord entity) {
        if (entity == null) {
            return null;
        }

        // Map Student to StudentDTO
        StudentDTO studentDTO = new StudentDTO(
                entity.getStudent().getId(),
                entity.getStudent().getStudentCode(),
                entity.getStudent().getFullName(),
                entity.getStudent().getEmail());

        // Map ClassSession to ClassSessionDTO (chỉ cần id/classEntity cho Thymeleaf)
        ClassSessionDTO sessionDTO = null;
        if (entity.getClassSession() != null) {
            sessionDTO = new ClassSessionDTO();
            sessionDTO.setId(entity.getClassSession().getId());
            if (entity.getClassSession().getClassEntity() != null) {
                sessionDTO.setClassEntity(new ClassEntityDTO(entity.getClassSession().getClassEntity().getId()));
            }
        }

        AttendanceRecordDTO dto = new AttendanceRecordDTO(
                entity.getId(),
                entity.getCheckedInAt(),
                entity.getStatus().name(),
                entity.getDistanceMeters(),
                entity.getFailReason(),
                entity.getStudentLatitude(),
                entity.getStudentLongitude(),
                entity.getDeviceUid(),
                studentDTO);
        dto.setSession(sessionDTO);
        return dto;
    }

    public List<AttendanceRecordDTO> toDTOList(List<AttendanceRecord> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
