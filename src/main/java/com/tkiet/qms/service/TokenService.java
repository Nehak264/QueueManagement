package com.tkiet.qms.service;

import com.tkiet.qms.entity.Token;
import com.tkiet.qms.entity.TokenStatus;
import com.tkiet.qms.repository.TokenRepository;
import com.tkiet.qms.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private StudentRepository studentRepository;

    public Token saveToken(Token token) {
        return tokenRepository.save(token);
    }

    public Token getTokenById(Long tokenId) {
        return tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));
    }

    /**
     * Returns PENDING tokens sorted by priority score (ASC) then submission time (ASC).
     * Admin always sees the most urgent applications first.
     */
    public List<Token> getPendingTokens() {
        return tokenRepository.findPendingTokensSorted();
    }

    // FIX #4 — for Admin "Approved Archive" tab
    public List<Token> getApprovedTokens() {
        return tokenRepository.findByStatus(TokenStatus.APPROVED);
    }

    public List<Token> getStudentTokens(Long studentId) {
        return tokenRepository.findByStudentId(studentId);
    }

    public Map<String, Long> getStats(LocalDate date) {
        Map<String, Long> stats = new HashMap<>();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay   = date.atTime(23, 59, 59);

        stats.put("pending",       tokenRepository.countByStatus(TokenStatus.PENDING));
        stats.put("approvedToday", tokenRepository.countByStatusAndServedAtBetween(TokenStatus.APPROVED, startOfDay, endOfDay));
        stats.put("rejectedToday", tokenRepository.countByStatusAndServedAtBetween(TokenStatus.REJECTED, startOfDay, endOfDay));
        return stats;
    }

    // FIX #1 — sequential daily token numbering (no collisions)
    public int getTodayTokenCount() {
        return tokenRepository.countTokensCreatedToday();
    }
}