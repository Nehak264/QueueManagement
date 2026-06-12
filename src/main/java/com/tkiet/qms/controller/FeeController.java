package com.tkiet.qms.controller;

import com.tkiet.qms.entity.FeeRecord;
import com.tkiet.qms.entity.Student;
import com.tkiet.qms.repository.FeeRepository;
import com.tkiet.qms.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class FeeController {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private StudentRepository studentRepository;

    // ── GET /api/student/fees?rollNumber=2201234 ──
    // Student calls this to see all their semester fee records
    @GetMapping("/api/student/fees")
    public ResponseEntity<?> getStudentFees(@RequestParam String rollNumber) {
        Student student = studentRepository.findByRollNumber(rollNumber);
        if (student == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Student not found."));
        }
        List<FeeRecord> records = feeRepository.findByStudentIdOrderBySemesterAsc(student.getId());
        return ResponseEntity.ok(records);
    }

    // ── POST /api/admin/upload-fees ──
    // Admin uploads an Excel file with fee data
    // Expected Excel columns (row 1 = header, skip it):
    // A: Roll Number | B: Academic Year | C: Semester | D: Total Fees | E: Minimum Due | F: Amount Paid
    @PostMapping("/api/admin/upload-fees")
    public ResponseEntity<String> uploadFees(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Uploaded file is empty.");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int savedCount   = 0;
            int updatedCount = 0;
            int skippedCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String rollNumber    = getCellValue(row.getCell(0));
                String academicYear  = getCellValue(row.getCell(1));
                String semesterStr   = getCellValue(row.getCell(2));
                String totalStr      = getCellValue(row.getCell(3));
                String minDueStr     = getCellValue(row.getCell(4));
                String amountPaidStr = getCellValue(row.getCell(5));

                if (rollNumber.isEmpty() || semesterStr.isEmpty()) continue;

                Student student = studentRepository.findByRollNumber(rollNumber);
                if (student == null) {
                    skippedCount++;
                    continue; // skip unknown roll numbers
                }

                int semester;
                double totalFees, minimumDue, amountPaid;
                try {
                    semester    = Integer.parseInt(semesterStr);
                    totalFees   = Double.parseDouble(totalStr);
                    minimumDue  = Double.parseDouble(minDueStr);
                    amountPaid  = Double.parseDouble(amountPaidStr);
                } catch (NumberFormatException e) {
                    skippedCount++;
                    continue;
                }

                // If record already exists for this student+semester+year, update it
                FeeRecord existing = feeRepository.findByStudentIdAndSemesterAndAcademicYear(
                        student.getId(), semester, academicYear);

                if (existing != null) {
                    existing.setTotalFees(totalFees);
                    existing.setMinimumDue(minimumDue);
                    existing.setAmountPaid(amountPaid);
                    feeRepository.save(existing);
                    updatedCount++;
                } else {
                    FeeRecord record = new FeeRecord();
                    record.setStudent(student);
                    record.setAcademicYear(academicYear);
                    record.setSemester(semester);
                    record.setTotalFees(totalFees);
                    record.setMinimumDue(minimumDue);
                    record.setAmountPaid(amountPaid);
                    feeRepository.save(record);
                    savedCount++;
                }
            }

            return ResponseEntity.ok(savedCount + " records added, "
                    + updatedCount + " updated, "
                    + skippedCount + " skipped (unknown roll number or bad data).");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error reading file: " + e.getMessage());
        }
    }

    // ── Helper ──
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            default:      return "";
        }
    }
}