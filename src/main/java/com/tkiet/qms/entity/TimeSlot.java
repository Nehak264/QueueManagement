package com.tkiet.qms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "time_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many slots belong to one counter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counter_id", nullable = false)
    private Counter counter;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;    // 2026-05-02

    @Column(name = "start_time")
    private LocalTime startTime;   // 09:00

    @Column(name = "end_time")
    private LocalTime endTime;     // 11:00

    @Column(name = "max_capacity")
    private int maxCapacity;       // 20

    @Column(name = "booked_count")
    private int bookedCount;       // increases as students book

    @Column(name = "is_active")
    private boolean isActive;      // admin can disable a slot
}
