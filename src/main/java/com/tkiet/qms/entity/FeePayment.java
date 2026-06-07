package com.tkiet.qms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id", nullable = false)
    private Token token;

    @Column(name = "fee_type", nullable = false)
    private String feeType;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "payment_mode", nullable = false)
    private String paymentMode;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "receipt_number", unique = true)
    private String receiptNumber;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
