package com.tkiet.qms.repository;

import com.tkiet.qms.entity.Token;
import com.tkiet.qms.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface TokenRepository extends JpaRepository<Token, Long> {

    // get all tokens for a slot in order — used in admin queue view
    List<Token> findBySlotIdOrderByTokenNumber(Long slotId);

    // get all tokens for a student — used in "My Requests" section
    List<Token> findByStudentId(Long studentId);

    // get the highest token number in a slot
    // so we can generate the next one (max + 1)
    @Query("SELECT COALESCE(MAX(t.tokenNumber), 0) FROM Token t WHERE t.slot.id = :slotId")
    int findMaxTokenNumberBySlot(@Param("slotId") Long slotId);

    // count tokens by status for today — used in admin dashboard stats
    long countBySlotSlotDateAndStatus(LocalDate date, TokenStatus status);
}
