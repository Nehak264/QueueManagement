package com.tkiet.qms.controller;

import com.tkiet.qms.entity.Student;
import com.tkiet.qms.entity.TimeSlot;
import com.tkiet.qms.entity.Token;
import com.tkiet.qms.repository.StudentRepository;
import com.tkiet.qms.service.SlotService;
import com.tkiet.qms.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private SlotService slotService;

    @Autowired
    private StudentRepository studentRepository;  // ← was missing

    // GET /api/student/slots?counterId=1
    @GetMapping("/slots")
    public List<TimeSlot> getSlots(@RequestParam Long counterId) {
        return slotService.getAvailableSlots(counterId);
    }

    // POST /api/student/book
    @PostMapping("/book")
    public Token bookToken(@RequestBody Map<String, String> body) {
        String rollNumber = body.get("rollNumber");
        Long slotId = Long.parseLong(body.get("slotId"));
        String purpose = body.get("purpose");
        
        Double amount = null;
        if (body.get("amount") != null && !body.get("amount").trim().isEmpty()) {
            try {
                amount = Double.parseDouble(body.get("amount"));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        
        String paymentMode = body.get("paymentMode");
        String referenceNumber = body.get("referenceNumber");
        
        return tokenService.bookToken(
                rollNumber,
                slotId,
                purpose,
                amount,
                paymentMode,
                referenceNumber
        );
    }

    // GET /api/student/tokens?studentId=1
    @GetMapping("/tokens")
    public List<Token> getMyTokens(@RequestParam Long studentId) {
        return tokenService.getStudentTokens(studentId);
    }

    // POST /api/student/cancel?tokenId=5
    @PostMapping("/cancel")
    public Token cancelToken(@RequestParam Long tokenId) {
        return tokenService.cancelToken(tokenId);
    }

    // POST /api/student/register
    @PostMapping("/register")
    public Student registerStudent(@RequestBody Student student) {

        // check if roll number already exists
        Student existing = studentRepository.findByRollNumber(student.getRollNumber());
        if (existing != null) {
            throw new RuntimeException("Student with this roll number already exists!");
        }

        return studentRepository.save(student);
    }
    // GET /api/student/tokens/byRoll?rollNumber=2201234
    @GetMapping("/tokens/byRoll")
    public List<Token> getTokensByRoll(@RequestParam String rollNumber) {
        Student student = studentRepository.findByRollNumber(rollNumber);
        if (student == null) return new java.util.ArrayList<>();
        return tokenService.getStudentTokens(student.getId());
    }
}