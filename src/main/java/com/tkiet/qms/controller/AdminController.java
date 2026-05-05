package com.tkiet.qms.controller;

import com.tkiet.qms.entity.BonafideCertificate;
import com.tkiet.qms.entity.Token;
import com.tkiet.qms.service.BonafideService;
import com.tkiet.qms.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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

    // GET /api/admin/queue?slotId=1
    // admin sees full queue for a slot
    @GetMapping("/queue")
    public List<Token> getQueue(@RequestParam Long slotId) {
        return tokenService.getQueue(slotId);
    }

    // POST /api/admin/done?tokenId=5
    // admin marks a token as done
    @PostMapping("/done")
    public Token markDone(@RequestParam Long tokenId) {
        return tokenService.markDone(tokenId);
    }

    // POST /api/admin/skip?tokenId=5
    // admin skips a student
    @PostMapping("/skip")
    public Token skipToken(@RequestParam Long tokenId) {
        return tokenService.skipToken(tokenId);
    }

    // GET /api/admin/stats
    // admin dashboard — waiting, done, skipped counts for today
    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return tokenService.getStats(LocalDate.now());
    }

    // POST /api/admin/certificate
    // body: { "tokenId": "5", "academicYear": "2025-2026" }
    // admin generates bonafide certificate
    @PostMapping("/certificate")
    public BonafideCertificate generateCertificate(@RequestBody Map<String, String> body) {
        return bonafideService.generateCertificate(
                Long.parseLong(body.get("tokenId")),
                body.get("academicYear")
        );
    }

    // GET /api/admin/certificate?tokenId=5
    // admin fetches an existing certificate
    @GetMapping("/certificate")
    public BonafideCertificate getCertificate(@RequestParam Long tokenId) {
        return bonafideService.getCertificate(tokenId);
    }
    // POST /api/admin/otp/send?tokenId=1
// generates OTP and saves to token
    @PostMapping("/otp/send")
    public Token sendOtp(@RequestParam Long tokenId) {
        return tokenService.sendOtp(tokenId);
    }

    // POST /api/admin/otp/verify?tokenId=1&otp=1234
// verifies OTP entered by admin
    @PostMapping("/otp/verify")
    public Token verifyOtp(@RequestParam Long tokenId, @RequestParam String otp) {
        return tokenService.verifyOtp(tokenId, otp);
    }
}