package com.tkiet.qms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "counters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Counter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;          // "Bonafide", "Fees Counter", "Library"

    @Column(name = "open_time")
    private LocalTime openTime;   // 09:00

    @Column(name = "close_time")
    private LocalTime closeTime;  // 17:00

    @Column(name = "max_per_slot")
    private int maxPerSlot;       // 20
}