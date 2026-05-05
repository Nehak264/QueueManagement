package com.tkiet.qms.service;

import com.tkiet.qms.entity.*;
import com.tkiet.qms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    // ── BOOK A TOKEN ──────────────────────────────────────
    public Token bookToken(String rollNumber, Long slotId, String purpose) {

        // 1. find the student by roll number
        Student student = studentRepository.findByRollNumber(rollNumber);
        if (student == null) {
            throw new RuntimeException("Student not found: " + rollNumber);
        }

        // 2. find the slot
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        // 3. check if slot is full
        if (slot.getBookedCount() >= slot.getMaxCapacity()) {
            throw new RuntimeException("Slot is full");
        }

        // 4. check if slot is active
        if (!slot.isActive()) {
            throw new RuntimeException("Slot is not active");
        }

        // 5. generate next token number for this slot
        //    findMaxTokenNumberBySlot returns 0 if no tokens yet
        //    so first token = 0 + 1 = 1
        int nextTokenNumber = tokenRepository.findMaxTokenNumberBySlot(slotId) + 1;

        // 6. create the token
        Token token = new Token();
        token.setTokenNumber(nextTokenNumber);
        token.setStudent(student);
        token.setSlot(slot);
        token.setPurpose(purpose);
        token.setStatus(TokenStatus.WAITING);
        token.setOtpVerified(false);

        // 7. increase booked count on the slot
        slot.setBookedCount(slot.getBookedCount() + 1);
        timeSlotRepository.save(slot);

        // 8. save and return the token
        return tokenRepository.save(token);
    }

    // ── GET QUEUE FOR A SLOT (admin view) ─────────────────
    public List<Token> getQueue(Long slotId) {
        return tokenRepository.findBySlotIdOrderByTokenNumber(slotId);
    }

    // ── GET STUDENT'S OWN TOKENS (my requests) ────────────
    public List<Token> getStudentTokens(Long studentId) {
        return tokenRepository.findByStudentId(studentId);
    }

    // ── MARK DONE (admin) ─────────────────────────────────
    public Token markDone(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(TokenStatus.DONE);
        token.setServedAt(LocalDateTime.now());  // record when it was done
        return tokenRepository.save(token);
    }

    // ── SKIP STUDENT (admin) ──────────────────────────────
    public Token skipToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(TokenStatus.SKIPPED);
        return tokenRepository.save(token);
    }

    // ── CANCEL TOKEN (student cancels) ────────────────────
    public Token cancelToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        // can only cancel if still waiting
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new RuntimeException("Cannot cancel — token is already " + token.getStatus());
        }

        token.setStatus(TokenStatus.CANCELLED);

        // decrease booked count on slot
        TimeSlot slot = token.getSlot();
        slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
        timeSlotRepository.save(slot);

        return tokenRepository.save(token);
    }

    // ── ADMIN STATS ───────────────────────────────────────
    public java.util.Map<String, Long> getStats(java.time.LocalDate date) {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("waiting",  tokenRepository.countBySlotSlotDateAndStatus(date, TokenStatus.WAITING));
        stats.put("done",     tokenRepository.countBySlotSlotDateAndStatus(date, TokenStatus.DONE));
        stats.put("skipped",  tokenRepository.countBySlotSlotDateAndStatus(date, TokenStatus.SKIPPED));
        return stats;
    }
    // generates a 4-digit OTP and saves to token
    public Token sendOtp(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        // generate random 4-digit OTP
        String otp = String.format("%04d", (int)(Math.random() * 9000) + 1000);
        token.setOtp(otp);
        token.setOtpVerified(false);

        System.out.println("OTP for token #" + token.getTokenNumber() + " : " + otp);

        return tokenRepository.save(token);
    }

    // verifies OTP entered by admin
    public Token verifyOtp(Long tokenId, String otp) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (token.getOtp() == null) {
            throw new RuntimeException("OTP not generated yet. Click Send OTP first.");
        }

        if (!token.getOtp().equals(otp)) {
            throw new RuntimeException("Wrong OTP. Please try again.");
        }

        token.setOtpVerified(true);
        return tokenRepository.save(token);
    }
}