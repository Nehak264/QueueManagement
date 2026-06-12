package com.tkiet.qms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_records")
public class FeeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private String academicYear;   // e.g. "2025-2026"
    private int semester;          // 1 to 8

    private double totalFees;      // Total fees for this semester
    private double minimumDue;     // Minimum amount to pay this semester
    private double amountPaid;     // How much student has paid so far
    // balance = totalFees - amountPaid (calculated, not stored)

    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public double getTotalFees() { return totalFees; }
    public void setTotalFees(double totalFees) { this.totalFees = totalFees; }

    public double getMinimumDue() { return minimumDue; }
    public void setMinimumDue(double minimumDue) { this.minimumDue = minimumDue; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public double getBalance() { return totalFees - amountPaid; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}