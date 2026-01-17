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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Import students from Excel file
     * 
     * @param file Excel file with columns: StudentCode, FullName, Username, Email,
     *             PhoneNumber, Password
     * @return Map with keys: students (List<User>), errors (List<String>),
     *         successCount (int), errorCount (int)
     */
    public Map<String, Object> importStudentsFromExcel(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<User> students = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                try {
                    User student = new User();

                    // Column 0: Student Code
                    String studentCode = getCellValueAsString(row.getCell(0));
                    if (studentCode.isEmpty()) {
                        errors.add("Dòng " + (i + 1) + ": Thiếu mã sinh viên");
                        errorCount++;
                        continue;
                    }
                    student.setStudentCode(studentCode);

                    // Column 1: Full Name
                    String fullName = getCellValueAsString(row.getCell(1));
                    if (fullName.isEmpty()) {
                        errors.add("Dòng " + (i + 1) + ": Thiếu họ tên");
                        errorCount++;
                        continue;
                    }
                    student.setFullName(fullName);

                    // Column 2: Username (Updated from column 4 to 2 based on user file format)
                    String username = getCellValueAsString(row.getCell(2));
                    if (username.isEmpty()) {
                        errors.add("Dòng " + (i + 1) + ": Thiếu username");
                        errorCount++;
                        continue;
                    }

                    // Check if username exists
                    if (userService.findByUsername(username).isPresent()) {
                        errors.add("Dòng " + (i + 1) + ": Username '" + username + "' đã tồn tại");
                        errorCount++;
                        continue;
                    }
                    student.setUsername(username);

                    // Column 3: Email (Updated from column 2 to 3 based on user file format)
                    String email = getCellValueAsString(row.getCell(3));
                    if (email.isEmpty()) {
                        errors.add("Dòng " + (i + 1) + ": Thiếu email");
                        errorCount++;
                        continue;
                    }
                    student.setEmail(email);

                    // Column 4: Phone Number (Optional, Updated from column 3 to 4)
                    String phone = getCellValueAsString(row.getCell(4));
                    student.setPhoneNumber(phone);

                    // Column 5: Password (optional, default: 123456)
                    String password = getCellValueAsString(row.getCell(5));
                    if (password.isEmpty()) {
                        password = "123456";
                    }
                    student.setPassword(password);

                    student.setRole(User.Role.STUDENT);
                    student.setIsActive(true);

                    students.add(student);
                    successCount++;

                } catch (Exception e) {
                    errors.add("Dòng " + (i + 1) + ": " + e.getMessage());
                    errorCount++;
                }
            }
        } catch (IOException e) {
            errors.add("Lỗi đọc file: " + e.getMessage());
            errorCount++;
        }

        result.put("students", students);
        result.put("errors", errors);
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        return result;
    }

    /**
     * Import student codes from Excel file for adding to class
     * 
     * @param file Excel file with one column: StudentCode
     * @return Map with keys: studentCodes (List<String>), errors (List<String>)
     */
    public Map<String, Object> importStudentCodesFromExcel(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<String> studentCodes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                try {
                    // Column 0: Student Code
                    String studentCode = getCellValueAsString(row.getCell(0));
                    if (studentCode.isEmpty()) {
                        errors.add("Dòng " + (i + 1) + ": Thiếu mã sinh viên");
                        continue;
                    }
                    studentCodes.add(studentCode);

                } catch (Exception e) {
                    errors.add("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add("Lỗi đọc file: " + e.getMessage());
        }

        result.put("studentCodes", studentCodes);
        result.put("errors", errors);
        return result;
    }
}
