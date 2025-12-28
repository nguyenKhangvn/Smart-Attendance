package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.StudentImportDTO;
import com.dinhkhang.code.entity.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ExcelImportService {

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<StudentImportDTO> parseExcelFile(MultipartFile file) throws IOException {
        List<StudentImportDTO> students = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                StudentImportDTO student = new StudentImportDTO();

                // Column 0: Student Code
                Cell codeCell = row.getCell(0);
                if (codeCell != null) {
                    student.setStudentCode(getCellValueAsString(codeCell));
                }

                // Column 1: Full Name
                Cell nameCell = row.getCell(1);
                if (nameCell != null) {
                    student.setFullName(getCellValueAsString(nameCell));
                }

                // Column 2: Email
                Cell emailCell = row.getCell(2);
                if (emailCell != null) {
                    student.setEmail(getCellValueAsString(emailCell));
                }

                // Column 3: Phone Number (optional)
                Cell phoneCell = row.getCell(3);
                if (phoneCell != null) {
                    student.setPhoneNumber(getCellValueAsString(phoneCell));
                }

                // Column 4: Username
                Cell usernameCell = row.getCell(4);
                if (usernameCell != null) {
                    student.setUsername(getCellValueAsString(usernameCell));
                }

                // Column 5: Password
                Cell passwordCell = row.getCell(5);
                if (passwordCell != null) {
                    student.setPassword(getCellValueAsString(passwordCell));
                }

                // Validate required fields
                if (student.getStudentCode() != null && !student.getStudentCode().isEmpty() &&
                        student.getFullName() != null && !student.getFullName().isEmpty() &&
                        student.getEmail() != null && !student.getEmail().isEmpty() &&
                        student.getUsername() != null && !student.getUsername().isEmpty()) {
                    students.add(student);
                }
            }
        }

        return students;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    public List<User> importStudents(List<StudentImportDTO> studentDTOs) {
        List<User> createdUsers = new ArrayList<>();

        for (StudentImportDTO dto : studentDTOs) {
            try {
                // Kiểm tra email đã tồn tại chưa
                if (userService.findByUsername(dto.getUsername()).isPresent()) {
                    System.out.println("Username already exists, skipping: " + dto.getUsername());
                    continue;
                }

                User user = new User();
                user.setStudentCode(dto.getStudentCode());
                user.setFullName(dto.getFullName());
                user.setEmail(dto.getEmail());
                user.setDateOfBirth(dto.getDateOfBirth()); // THÊM MỚI
                user.setPhoneNumber(dto.getPhoneNumber());
                user.setUsername(dto.getUsername());

                // Set default password = 123456
                String password = dto.getPassword();
                if (password == null || password.isEmpty()) {
                    password = "123456";
                }
                user.setPassword(password);
                user.setRole(User.Role.STUDENT);
                user.setIsActive(true);

                User created = userService.createUser(user);
                createdUsers.add(created);
            } catch (Exception e) {
                System.err.println("Error importing student: " + dto.getEmail() + " - " + e.getMessage());
            }
        }

        return createdUsers;
    }
}
