package com.tkiet.qms.controller;

import com.tkiet.qms.entity.BonafideCertificate;
import com.tkiet.qms.entity.Student;
import com.tkiet.qms.entity.Token;
import com.tkiet.qms.repository.StudentRepository;
import com.tkiet.qms.service.BonafideService;
import com.tkiet.qms.service.TokenService;
import com.tkiet.qms.service.EmailService;
import com.tkiet.qms.service.PdfService;
import com.tkiet.qms.entity.TokenStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private EmailService emailService;

    @Autowired
    private PdfService pdfService;

    // ── GET /api/admin/applications  (PENDING only) ──
    @GetMapping("/applications")
    public List<Token> getPendingApplications() {
        return tokenService.getPendingTokens();
    }

    // FIX #4: NEW endpoint — GET /api/admin/applications/approved
    @GetMapping("/applications/approved")
    public List<Token> getApprovedApplications() {
        return tokenService.getApprovedTokens(); // Add this method in TokenService
    }

    // ── POST /api/admin/approve?tokenId=1 ──
    @PostMapping("/approve")
    public ResponseEntity<Map<String, String>> approveApplication(
            @RequestParam Long tokenId,
            @RequestBody Map<String, String> body) {

        // FIX #2: null-safe token lookup
        Token token = tokenService.getTokenById(tokenId);
        if (token == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", "Token not found with id: " + tokenId));
        }

        // Guard: only approve PENDING tokens
        if (token.getStatus() != TokenStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Token is already " + token.getStatus()));
        }

        Student student = token.getStudent();
        if (student == null) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Token has no associated student."));
        }

        String academicYear = body.getOrDefault("academicYear", token.getAcademicYear());

        // Ref number uses the token's DB id — globally unique, never resets.
        // token.getTokenNumber() is a daily counter that resets to 1 each day
        // and caused duplicate-key errors when multiple approvals happened across days.
        String refNumber = "TKIET/CSE/BON/" + academicYear + "/TK-"
                + String.format("%05d", token.getId());

        // 2. Generate PDF
        byte[] pdfBytes = pdfService.generateBonafidePdf(
                student.getName(),
                student.getRollNumber(),
                student.getClassName(),
                student.getDivision(),
                token.getPurpose(),
                academicYear,
                refNumber
        );

        // 3. Save BonafideCertificate record
        bonafideService.saveCertificate(token, refNumber, academicYear);

        // 4. Send Email
        emailService.sendCertificateEmail(student.getEmail(), student.getName(), pdfBytes, refNumber);

        // 5. Update Token status
        token.setStatus(TokenStatus.APPROVED);
        token.setServedAt(java.time.LocalDateTime.now());
        tokenService.saveToken(token);

        return ResponseEntity.ok(Map.of("message", "Application approved and certificate emailed."));
    }

    // ── POST /api/admin/reject?tokenId=1 ──
    @PostMapping("/reject")
    public ResponseEntity<Map<String, String>> rejectApplication(
            @RequestParam Long tokenId,
            @RequestBody Map<String, String> body) {

        // FIX #2: null-safe token lookup
        Token token = tokenService.getTokenById(tokenId);
        if (token == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", "Token not found with id: " + tokenId));
        }

        // Guard: only reject PENDING tokens
        if (token.getStatus() != TokenStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Token is already " + token.getStatus()));
        }

        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Rejection reason is required."));
        }

        token.setStatus(TokenStatus.REJECTED);
        token.setRejectionReason(reason);
        tokenService.saveToken(token);

        emailService.sendRejectionEmail(
                token.getStudent().getEmail(),
                token.getStudent().getName(),
                reason
        );

        return ResponseEntity.ok(Map.of("message", "Application rejected."));
    }

    // ── GET /api/admin/verify/{tokenId} ──
    @GetMapping("/verify/{tokenId}")
    public ResponseEntity<?> verifyToken(@PathVariable Long tokenId) {
        Token token = tokenService.getTokenById(tokenId);
        if (token == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Token not found"));
        }

        Student student = token.getStudent();
        List<Token> allTokens = tokenService.getStudentTokens(student.getId());

        // History excluding the current token
        List<Token> history = allTokens.stream()
                .filter(t -> !t.getId().equals(tokenId))
                .collect(Collectors.toList());

        long approvedCount = history.stream().filter(t -> t.getStatus() == com.tkiet.qms.entity.TokenStatus.APPROVED).count();
        long rejectedCount = history.stream().filter(t -> t.getStatus() == com.tkiet.qms.entity.TokenStatus.REJECTED).count();
        long pendingCount  = history.stream().filter(t -> t.getStatus() == com.tkiet.qms.entity.TokenStatus.PENDING).count();

        // Check if there's a duplicate pending application for same purpose
        boolean hasDuplicatePending = history.stream()
                .anyMatch(t -> t.getStatus() == com.tkiet.qms.entity.TokenStatus.PENDING
                        && t.getPurpose() != null
                        && t.getPurpose().equalsIgnoreCase(token.getPurpose()));

        // Check if same purpose was already approved
        boolean alreadyApprovedSamePurpose = history.stream()
                .anyMatch(t -> t.getStatus() == com.tkiet.qms.entity.TokenStatus.APPROVED
                        && t.getPurpose() != null
                        && t.getPurpose().equalsIgnoreCase(token.getPurpose())
                        && t.getAcademicYear() != null
                        && t.getAcademicYear().equals(token.getAcademicYear()));

        Map<String, Object> result = new HashMap<>();
        result.put("studentName",    student.getName());
        result.put("rollNumber",     student.getRollNumber());
        result.put("className",      student.getClassName());
        result.put("division",       student.getDivision());
        result.put("email",          student.getEmail());
        result.put("mobile",         student.getMobile());
        result.put("source", student.getSource() != null ? student.getSource() : "SELF_REGISTERED");
        result.put("totalHistory",   history.size());
        result.put("approvedCount",  approvedCount);
        result.put("rejectedCount",  rejectedCount);
        result.put("pendingCount",   pendingCount);
        result.put("hasDuplicatePending",        hasDuplicatePending);
        result.put("alreadyApprovedSamePurpose", alreadyApprovedSamePurpose);
        result.put("isFirstTimeApplicant",       history.isEmpty());
        result.put("registeredOn",   student.getCreatedAt());

        return ResponseEntity.ok(result);
    }

    // ── GET /api/admin/stats ──
    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return tokenService.getStats(java.time.LocalDate.now());
    }

    // ── GET /api/admin/students ──
    @GetMapping("/students")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // ── POST /api/admin/upload-students ──
    @PostMapping("/upload-students")
    public ResponseEntity<String> uploadStudents(@RequestParam("file") MultipartFile file) {
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

                String name       = getCellValue(row.getCell(0));
                String rollNumber = getCellValue(row.getCell(1));
                String className  = getCellValue(row.getCell(2));
                String division   = getCellValue(row.getCell(3));
                String email      = getCellValue(row.getCell(4));
                String mobile     = getCellValue(row.getCell(5));

                if (name.isEmpty() || rollNumber.isEmpty()) continue;

                Student existing = studentRepository.findByRollNumber(rollNumber);

                if (existing != null) {
                    // If student was self-registered, upgrade them to ADMIN_UPLOADED
                    // and overwrite their data with official college records
                    if (!"ADMIN_UPLOADED".equals(existing.getSource())) {
                        existing.setName(name);
                        existing.setClassName(className);
                        existing.setDivision(division);
                        existing.setEmail(email);
                        existing.setMobile(mobile);
                        existing.setSource("ADMIN_UPLOADED");
                        studentRepository.save(existing);
                        updatedCount++;
                    } else {
                        skippedCount++; // already official, skip
                    }
                    continue;
                }

                // New student — save as official (ADMIN_UPLOADED)
                Student student = new Student();
                student.setName(name);
                student.setRollNumber(rollNumber);
                student.setClassName(className);
                student.setDivision(division);
                student.setEmail(email);
                student.setMobile(mobile);
                student.setSource("ADMIN_UPLOADED");  // mark as verified official record
                studentRepository.save(student);
                savedCount++;
            }

            return ResponseEntity.ok(
                    savedCount + " students imported, "
                    + updatedCount + " upgraded to verified, "
                    + skippedCount + " already official (skipped).");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error reading file: " + e.getMessage());
        }
    }

    // ── GET /api/admin/download-students ──
    @GetMapping("/download-students")
    public void downloadStudents(HttpServletResponse response) throws IOException {
        List<Student> students = studentRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Name", "Roll Number", "Class", "Division", "Email", "Mobile", "Created At"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Student s : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getId());
                row.createCell(1).setCellValue(s.getName());
                row.createCell(2).setCellValue(s.getRollNumber());
                row.createCell(3).setCellValue(s.getClassName());
                row.createCell(4).setCellValue(s.getDivision());
                row.createCell(5).setCellValue(s.getEmail() != null ? s.getEmail() : "");
                row.createCell(6).setCellValue(s.getMobile() != null ? s.getMobile() : "");
                row.createCell(7).setCellValue(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=students.xlsx");
            workbook.write(response.getOutputStream());
        }
    }

    // ── Helper — read cell as String ──
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            default:      return "";
        }
    }
}