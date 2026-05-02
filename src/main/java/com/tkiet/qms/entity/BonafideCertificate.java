package com.tkiet.qms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "bonafide_certificates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BonafideCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id", nullable = false)
    private Token token;

    @Column(name = "ref_number", unique = true)
    private String refNumber;      // TKIET/CSE/BON/2026/047

    @Column(name = "academic_year")
    private String academicYear;   // "2025 – 2026"

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}