package com.tkiet.qms.controller;

import com.tkiet.qms.entity.Student;
import com.tkiet.qms.entity.Token;
import com.tkiet.qms.entity.TokenStatus;
import com.tkiet.qms.repository.StudentRepository;
import com.tkiet.qms.service.OtpService;
import com.tkiet.qms.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private StudentRepository studentRepository;

    // ── SEND OTP TO EMAIL ───────────────────────────────────────
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required."));
        }

        otpService.sendOtp(email);

        return ResponseEntity.ok(
                Map.of("message", "OTP sent successfully to email."));
    }

    // ── SEND LOGIN OTP ──────────────────────────────────────────
    @PostMapping("/send-login-otp")
    public ResponseEntity<Map<String, String>> sendLoginOtp(
            @RequestBody Map<String, String> body) {

        String rollNumber = body.get("rollNumber");

        if (rollNumber == null || rollNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Roll number is required."));
        }

        Student student = studentRepository.findByRollNumber(rollNumber);

        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("message",
                            "Student not found. Please register first."));
        }

        otpService.sendOtp(student.getEmail());

        return ResponseEntity.ok(
                Map.of("message",
                        "OTP sent to registered email."));
    }

    // ── VERIFY OTP ──────────────────────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "verified", false,
                            "message", "Email and OTP are required."
                    ));
        }

        boolean verified = otpService.verifyOtp(email, otp);

        return ResponseEntity.ok(
                Map.of("verified", verified));
    }

    // ── VERIFY LOGIN OTP ────────────────────────────────────────
    @PostMapping("/verify-login-otp")
    public ResponseEntity<Map<String, Object>> verifyLoginOtp(
            @RequestBody Map<String, String> body) {

        String rollNumber = body.get("rollNumber");
        String otp = body.get("otp");

        if (rollNumber == null || otp == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "verified", false,
                            "message",
                            "Roll number and OTP are required."
                    ));
        }

        Student student = studentRepository.findByRollNumber(rollNumber);

        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of(
                            "verified", false,
                            "message", "Student not found."
                    ));
        }

        boolean verified =
                otpService.verifyOtp(student.getEmail(), otp);

        if (verified) {
            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "name", student.getName(),
                    "rollNumber", student.getRollNumber(),
                    "className", student.getClassName(),
                    "division", student.getDivision(),
                    "email", student.getEmail()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "verified", false,
                "message", "Invalid OTP."
        ));
    }

    // ── APPLY FOR BONAFIDE ──────────────────────────────────────
    @PostMapping("/apply")
    public ResponseEntity<?> apply(
            @RequestBody Map<String, String> body) {

        // ── DATE/TIME CONSTRAINT (backend layer — cannot be bypassed) ──
        // 🚧 TESTING MODE: constraints commented out — re-enable before production
        // LocalDateTime now = LocalDateTime.now();
        // DayOfWeek day = now.getDayOfWeek();
        // int hour = now.getHour();

        // if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
        //     return ResponseEntity.badRequest()
        //             .body(Map.of("message",
        //                     "Applications can only be submitted on weekdays (Monday–Friday). The office is closed on weekends."));
        // }
        // if (hour < 9 || hour >= 17) {
        //     return ResponseEntity.badRequest()
        //             .body(Map.of("message",
        //                     "Applications can only be submitted between 9:00 AM and 5:00 PM college hours."));
        // }

        // ── REQUIRED FIELD VALIDATION ──
        String rollNumber = body.get("rollNumber");
        String purpose = body.get("purpose");
        String academicYear = body.get("academicYear");

        if (rollNumber == null ||
                purpose == null ||
                academicYear == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "rollNumber, purpose and academicYear are required."
                    ));
        }

        Student student =
                studentRepository.findByRollNumber(rollNumber);

        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", "Student not found."));
        }

        boolean alreadyPending =
                tokenService.getStudentTokens(student.getId())
                        .stream()
                        .anyMatch(t ->
                                t.getPurpose().equals(purpose)
                                        && t.getAcademicYear().equals(academicYear)
                                        && t.getStatus() == TokenStatus.PENDING);

        if (alreadyPending) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "You already have a pending application for this purpose."
                    ));
        }

        // ── PRIORITY SCORE (urgency-based queue ordering) ──
        int priorityScore = switch (purpose) {
            case "Passport Application", "Visa Application" -> 1;  // Most urgent — government docs
            case "Scholarship Form"                          -> 2;  // External deadline
            case "Bank Account Opening"                     -> 3;  // Important but flexible
            default                                         -> 4;  // Other
        };

        Token token = new Token();
        token.setStudent(student);
        token.setPurpose(purpose);
        token.setAcademicYear(academicYear);
        token.setStatus(TokenStatus.PENDING);
        token.setPriorityScore(priorityScore);

        int dailyCount = tokenService.getTodayTokenCount();
        token.setTokenNumber(dailyCount + 1);

        Token saved = tokenService.saveToken(token);

        return ResponseEntity.ok(saved);
    }

    // ── GET APPLICATIONS ────────────────────────────────────────
    @GetMapping("/applications")
    public ResponseEntity<?> getApplications(
            @RequestParam String rollNumber) {

        if (rollNumber == null || rollNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Roll number is required."));
        }

        Student student =
                studentRepository.findByRollNumber(rollNumber);

        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("message",
                            "Student not found."));
        }

        List<Token> tokens =
                tokenService.getStudentTokens(student.getId());

        return ResponseEntity.ok(tokens);
    }

    // ── REGISTER STUDENT ────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> registerStudent(
            @RequestBody Student student) {

        if (student.getName() == null ||
                student.getName().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Name is required."));
        }

        // Format: 23UGCS22047 → 2-digit year + "UG" + 2-letter dept + 5-digit sequence
        if (student.getRollNumber() == null ||
                !student.getRollNumber().matches("\\d{2}UG[A-Z]{2}\\d{5}")) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid roll number format. Expected format: 23UGCS22047 (year + UG + dept + 5-digit no.)"
                    ));
        }

        if (student.getMobile() == null ||
                !student.getMobile().matches("\\d{10}")) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Mobile number must be 10 digits."
                    ));
        }

        if (student.getEmail() == null ||
                !student.getEmail().matches(
                        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid email address."
                    ));
        }

        Student existing =
                studentRepository.findByRollNumber(
                        student.getRollNumber());

        if (existing != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Student with this roll number already exists!"
                    ));
        }

        // Always stamp SELF_REGISTERED — frontend never sends this field,
        // and Jackson can bypass the Java field initializer on deserialization.
        student.setSource("SELF_REGISTERED");

        Student saved = studentRepository.save(student);

        return ResponseEntity.ok(saved);
    }
}