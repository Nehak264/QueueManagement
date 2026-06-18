package com.tkiet.qms.repository;

import com.tkiet.qms.entity.Token;
import com.tkiet.qms.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface TokenRepository extends JpaRepository<Token, Long> {

    // Get all tokens for a student — used in "My Requests" section
    List<Token> findByStudentId(Long studentId);

    // Get all tokens with a specific status (e.g. PENDING, APPROVED)
    List<Token> findByStatus(TokenStatus status);

    /**
     * Priority-sorted pending queue for admin dashboard.
     * Sorted by priorityScore ASC (lower = more urgent) then createdAt ASC (earlier = first).
     * Tie-breaking: if two students have same urgency, whoever submitted first appears first.
     */
    @Query("SELECT t FROM Token t WHERE t.status = com.tkiet.qms.entity.TokenStatus.PENDING ORDER BY t.priorityScore ASC NULLS LAST, t.createdAt ASC NULLS LAST")
    List<Token> findPendingTokensSorted();

    // Count by status — used in stats
    long countByStatus(TokenStatus status);

    // Count by status and servedAt range — used for daily approved/rejected stats
    long countByStatusAndServedAtBetween(TokenStatus status, LocalDateTime start, LocalDateTime end);

    // Count tokens created today for sequential daily token numbering
    @Query("SELECT COUNT(t) FROM Token t WHERE DATE(t.createdAt) = CURRENT_DATE")
    int countTokensCreatedToday();
}