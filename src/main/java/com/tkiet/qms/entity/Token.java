package com.tkiet.qms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_number", nullable = false)
    private int tokenNumber;       // #47, #48 — generated in service layer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimeSlot slot;

    private String purpose;        // "Bank Account Opening", "Passport" etc.

    @Enumerated(EnumType.STRING)   // stores "WAITING" not 0,1,2 in DB
    private TokenStatus status;

    private String otp;            // 4-digit OTP for verification

    @Column(name = "otp_verified")
    private boolean otpVerified;

    @Column(name = "extra_note")
    private String extraNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "served_at")
    private LocalDateTime servedAt;    // set when status → DONE
}
