package com.dinhkhang.code.mapper;

import com.dinhkhang.code.dto.AttendanceRecordDTO;
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
                entity.getStudent().getEmail()
        );

        // Map AttendanceRecord to AttendanceRecordDTO
        return new AttendanceRecordDTO(
                entity.getId(),
                entity.getCheckedInAt(),
                entity.getStatus().name(),
                entity.getDistanceMeters(),
                entity.getFailReason(),
                entity.getStudentLatitude(),
                entity.getStudentLongitude(),
                entity.getDeviceUid(),
                studentDTO
        );
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
