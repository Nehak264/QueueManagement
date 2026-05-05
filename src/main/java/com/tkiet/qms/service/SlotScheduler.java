package com.tkiet.qms.service;

import com.tkiet.qms.entity.Counter;
import com.tkiet.qms.entity.TimeSlot;
import com.tkiet.qms.repository.CounterRepository;
import com.tkiet.qms.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

    @Component
    public class SlotScheduler {

        @Autowired
        private TimeSlotRepository timeSlotRepository;

        @Autowired
        private CounterRepository counterRepository;

        // runs every day at 8:00 AM automatically
        @Scheduled(cron = "0 0 8 * * *")
        public void createDailySlots() {

            LocalDate today = LocalDate.now();

            // check if slots already exist for today — don't create duplicates
            List<TimeSlot> existing = timeSlotRepository.findBySlotDate(today);
            if (!existing.isEmpty()) {
                System.out.println("Slots already exist for " + today);
                return;
            }

            // get all counters from DB
            List<Counter> counters = counterRepository.findAll();

            for (Counter counter : counters) {
                if (counter.getName().equals("Bonafide")) {
                    // Bonafide — 2 slots
                    createSlot(counter, today, "09:00", "11:00", 20);
                    createSlot(counter, today, "14:00", "16:00", 20);

                } else if (counter.getName().equals("Fees Counter")) {
                    // Fees — 2 slots
                    createSlot(counter, today, "11:00", "13:00", 25);
                    createSlot(counter, today, "13:00", "14:00", 25);
                }
            }

            System.out.println("✅ Daily slots created for " + today);
        }

        // also run on app startup — so slots exist immediately when Spring Boot starts
        @Scheduled(initialDelay = 3000, fixedDelay = Long.MAX_VALUE)
        public void createSlotsOnStartup() {
            createDailySlots();
        }

        private void createSlot(Counter counter, LocalDate date, String start, String end, int capacity) {
            TimeSlot slot = new TimeSlot();
            slot.setCounter(counter);
            slot.setSlotDate(date);
            slot.setStartTime(LocalTime.parse(start));
            slot.setEndTime(LocalTime.parse(end));
            slot.setMaxCapacity(capacity);
            slot.setBookedCount(0);
            slot.setActive(true);
            timeSlotRepository.save(slot);
        }
    }

