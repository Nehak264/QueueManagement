package com.tkiet.qms.service;

import com.tkiet.qms.entity.*;
import com.tkiet.qms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    // ── BOOK A TOKEN (Modified with OTP Generation) ──────
    public Token bookToken(String rollNumber, Long slotId, String purpose) {
        Student student = studentRepository.findByRollNumber(rollNumber);
        if (student == null) {
            throw new RuntimeException("Student not found: " + rollNumber);
        }

        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.getBookedCount() >= slot.getMaxCapacity()) {
            throw new RuntimeException("Slot is full");
        }

        if (!slot.isActive()) {
            throw new RuntimeException("Slot is not active");
        }

        int nextTokenNumber = tokenRepository.findMaxTokenNumberBySlot(slotId) + 1;

        // NEW: Generate a 6-digit OTP
        String generatedOtp = String.format("%06d", new Random().nextInt(999999));

        Token token = new Token();
        token.setTokenNumber(nextTokenNumber);
        token.setStudent(student);
        token.setSlot(slot);
        token.setPurpose(purpose);
        token.setStatus(TokenStatus.WAITING);
        token.setOtp(generatedOtp); // Saving the OTP
        token.setOtpVerified(false);

        slot.setBookedCount(slot.getBookedCount() + 1);
        timeSlotRepository.save(slot);

        return tokenRepository.save(token);
    }

    // ── VERIFY OTP & MARK DONE (New Admin Method) ─────────
    public Token verifyOtpAndDone(Long tokenId, String inputOtp) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (token.getStatus() != TokenStatus.WAITING) {
            throw new RuntimeException("Token is already " + token.getStatus());
        }

        // Check if the OTP matches
        if (token.getOtp() != null && token.getOtp().equals(inputOtp)) {
            token.setOtpVerified(true);
            token.setStatus(TokenStatus.DONE);
            token.setServedAt(LocalDateTime.now());
            return tokenRepository.save(token);
        } else {
            throw new RuntimeException("Invalid OTP. Verification failed.");
        }
    }

    // ── EXISTING METHODS ──────────────────────────────────
    public List<Token> getQueue(Long slotId) {
        return tokenRepository.findBySlotIdOrderByTokenNumber(slotId);
    }

    public List<Token> getStudentTokens(Long studentId) {
        return tokenRepository.findByStudentId(studentId);
    }

    public Token markDone(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(TokenStatus.DONE);
        token.setServedAt(LocalDateTime.now());
        return tokenRepository.save(token);
    }

    public Token skipToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(TokenStatus.SKIPPED);
        return tokenRepository.save(token);
    }

    public Token cancelToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (token.getStatus() != TokenStatus.WAITING) {
            throw new RuntimeException("Cannot cancel — token is already " + token.getStatus());
        }

        token.setStatus(TokenStatus.CANCELLED);
        TimeSlot slot = token.getSlot();
        slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
        timeSlotRepository.save(slot);

        return tokenRepository.save(token);
    }

    public java.util.Map<String, Long> getStats(java.time.LocalDate date) {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("waiting",  tokenRepository.countBySlotSlotDateAndStatus(date, TokenStatus.WAITING));
        stats.put("done",     tokenRepository.countBySlotSlotDateAndStatus(date, TokenStatus.DONE));
        stats.put("skipped",  tokenRepository.countBySlotSlotDateAndStatus(date, TokenStatus.SKIPPED));
        return stats;
    }
}