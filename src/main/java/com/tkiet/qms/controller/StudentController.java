package com.tkiet.qms.controller;

import com.tkiet.qms.entity.TimeSlot;
import com.tkiet.qms.entity.Token;
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

    // GET /api/student/slots?counterId=1
    // student sees available slots for a counter
    @GetMapping("/slots")
    public List<TimeSlot> getSlots(@RequestParam Long counterId) {
        return slotService.getAvailableSlots(counterId);
    }

    // POST /api/student/book
    // body: { "rollNumber": "2201234", "slotId": 1, "purpose": "Bank Account" }
    // student books a token
    @PostMapping("/book")
    public Token bookToken(@RequestBody Map<String, String> body) {
        return tokenService.bookToken(
                body.get("rollNumber"),
                Long.parseLong(body.get("slotId")),
                body.get("purpose")
        );
    }

    // GET /api/student/tokens?studentId=1
    // student sees their own past requests
    @GetMapping("/tokens")
    public List<Token> getMyTokens(@RequestParam Long studentId) {
        return tokenService.getStudentTokens(studentId);
    }

    // POST /api/student/cancel?tokenId=5
    // student cancels their token
    @PostMapping("/cancel")
    public Token cancelToken(@RequestParam Long tokenId) {
        return tokenService.cancelToken(tokenId);
    }
}
