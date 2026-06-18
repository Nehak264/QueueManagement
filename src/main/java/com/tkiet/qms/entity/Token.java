package com.tkiet.qms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private int tokenNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Student student;

    private String purpose;

    /**
     * Queue priority score assigned at submission time.
     * Lower = processed first by admin.
     * 1 = Passport/Visa (urgent), 2 = Scholarship, 3 = Bank Account, 4 = Other
     * NULL for legacy tokens — treated as lowest priority (4) in admin display.
     */
    @Column(name = "priority_score")
    private Integer priorityScore;
    
    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    private String otp;

    @Column(name = "otp_verified")
    private boolean otpVerified;

    @Column(name = "extra_note")
    private String extraNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "served_at")
    private LocalDateTime servedAt;
}