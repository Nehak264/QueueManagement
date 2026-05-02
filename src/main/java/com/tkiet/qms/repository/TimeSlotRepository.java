package com.tkiet.qms.repository;

import com.tkiet.qms.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    // finds all slots for a specific counter on a specific date
    // used in student portal to show available slots
    List<TimeSlot> findByCounterIdAndSlotDate(Long counterId, LocalDate date);
}
