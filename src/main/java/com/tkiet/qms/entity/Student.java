package com.tkiet.qms.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@JsonPropertyOrder({"id", "name", "rollNumber", "className", "division", "email", "source", "createdAt"})
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "roll_number", unique = true, nullable = false)
    private String rollNumber;

    @Column(name = "class_name")
    private String className;

    private String division;

    private String email;

    private String mobile;

    /**
     * Source of this student record:
     * - ADMIN_UPLOADED  : came from official college Excel uploaded by admin → data is VERIFIED
     * - SELF_REGISTERED : student typed their own details on register.html → data is UNVERIFIED
     *
     * Admin should treat SELF_REGISTERED applications with extra scrutiny.
     * Ideally, only ADMIN_UPLOADED students should be able to apply.
     */
    @Column(name = "source", columnDefinition = "VARCHAR(30) DEFAULT 'SELF_REGISTERED'")
    private String source = "SELF_REGISTERED"; // default for safety — ADMIN_UPLOADED = official data

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}