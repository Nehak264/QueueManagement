package com.tkiet.qms.service;

import com.tkiet.qms.entity.TimeSlot;
import com.tkiet.qms.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class SlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    // returns all slots for a counter on today's date
    public List<TimeSlot> getAvailableSlots(Long counterId) {
        LocalDate today = LocalDate.now();
        return timeSlotRepository.findByCounterIdAndSlotDate(counterId, today);
    }

    // returns a single slot by id
    public TimeSlot getSlotById(Long slotId) {
        return timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
    }
}