package com.tkiet.qms.controller;

import com.tkiet.qms.entity.BonafideCertificate;
import com.tkiet.qms.entity.FeePayment;
import com.tkiet.qms.entity.Student;
import com.tkiet.qms.entity.Token;
import com.tkiet.qms.repository.StudentRepository;
import com.tkiet.qms.service.BonafideService;
import com.tkiet.qms.service.FeePaymentService;
import com.tkiet.qms.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private BonafideService bonafideService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FeePaymentService feePaymentService;

    // ── GET /api/admin/queue?slotId=1 ──
    @GetMapping("/queue")
    public List<Token> getQueue(@RequestParam Long slotId) {
        return tokenService.getQueue(slotId);
    }

    // ── POST /api/admin/done?tokenId=5 ──
    @PostMapping("/done")
    public Token markDone(@RequestParam Long tokenId) {
        return tokenService.markDone(tokenId);
    }

    // ── POST /api/admin/skip?tokenId=5 ──
    @PostMapping("/skip")
    public Token skipToken(@RequestParam Long tokenId) {
        return tokenService.skipToken(tokenId);
    }

    // ── GET /api/admin/stats ──
    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return tokenService.getStats(LocalDate.now());
    }

    // ── POST /api/admin/certificate ──
    @PostMapping("/certificate")
    public BonafideCertificate generateCertificate(@RequestBody Map<String, String> body) {
        return bonafideService.generateCertificate(
                Long.parseLong(body.get("tokenId")),
                body.get("academicYear")
        );
    }

    // ── GET /api/admin/certificate?tokenId=5 ──
    @GetMapping("/certificate")
    public BonafideCertificate getCertificate(@RequestParam Long tokenId) {
        return bonafideService.getCertificate(tokenId);
    }

    // ── GET /api/admin/fee-payment?tokenId=5 ──
    @GetMapping("/fee-payment")
    public FeePayment getFeePayment(@RequestParam Long tokenId) {
        return feePaymentService.getPaymentByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("Fee payment details not found for token: " + tokenId));
    }

    // ── POST /api/admin/receipt ──
    @PostMapping("/receipt")
    public FeePayment generateReceipt(@RequestBody Map<String, String> body) {
        return feePaymentService.finalizePayment(
                Long.parseLong(body.get("tokenId"))
        );
    }

    // ── POST /api/admin/otp/send?tokenId=1 ──
    @PostMapping("/otp/send")
    public Token sendOtp(@RequestParam Long tokenId) {
        return tokenService.sendOtp(tokenId);
    }

    // ── POST /api/admin/otp/verify?tokenId=1&otp=1234 ──
    @PostMapping("/otp/verify")
    public Token verifyOtp(@RequestParam Long tokenId, @RequestParam String otp) {
        return tokenService.verifyOtp(tokenId, otp);
    }

    // ── GET /api/admin/students ──
    @GetMapping("/students")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // ── POST /api/admin/upload-students ──
    @PostMapping("/upload-students")
    public String uploadStudents(@RequestParam("file") MultipartFile file) {
        try {
            InputStream is = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            int savedCount   = 0;
            int skippedCount = 0;

            // loop rows — skip row 0 (header)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name       = getCellValue(row.getCell(0)); // Column A
                String rollNumber = getCellValue(row.getCell(1)); // Column B
                String className  = getCellValue(row.getCell(2)); // Column C
                String division   = getCellValue(row.getCell(3)); // Column D
                String email      = getCellValue(row.getCell(4)); // Column E

                // skip empty rows
                if (name.isEmpty() || rollNumber.isEmpty()) continue;

                // skip duplicate roll numbers
                if (studentRepository.findByRollNumber(rollNumber) != null) {
                    skippedCount++;
                    continue;
                }

                // save to database
                Student student = new Student();
                student.setName(name);
                student.setRollNumber(rollNumber);
                student.setClassName(className);
                student.setDivision(division);
                student.setEmail(email);
                studentRepository.save(student);
                savedCount++;
            }

            workbook.close();
            return savedCount + " students imported! " + skippedCount + " skipped (already exist).";

        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    // ── GET /api/admin/download-students ──
    @GetMapping("/download-students")
    public void downloadStudents(HttpServletResponse response) throws IOException {

        List<Student> students = studentRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // header row
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Name", "Roll Number", "Class", "Division", "Email", "Created At"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }


        // data rows — specify exact order
        int rowNum = 1;
        for (Student s : students) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(s.getId());
            row.createCell(1).setCellValue(s.getName());        // Name
            row.createCell(2).setCellValue(s.getRollNumber());  // Roll Number
            row.createCell(3).setCellValue(s.getClassName());   // Class
            row.createCell(4).setCellValue(s.getDivision());    // Division
            row.createCell(5).setCellValue(s.getEmail() != null ? s.getEmail() : "");
            row.createCell(6).setCellValue(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
        }
        // auto size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // send as download
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=students.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ── helper — read cell as String ──
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            default:      return "";
        }
    }
}