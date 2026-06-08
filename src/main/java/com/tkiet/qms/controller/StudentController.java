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

        Token token = new Token();
        token.setStudent(student);
        token.setPurpose(purpose);
        token.setAcademicYear(academicYear);
        token.setStatus(TokenStatus.PENDING);

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

        if (student.getRollNumber() == null ||
                !student.getRollNumber().matches("\\d{7}")) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Roll number must be exactly 7 digits."
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

        Student saved = studentRepository.save(student);

        return ResponseEntity.ok(saved);
    }
}