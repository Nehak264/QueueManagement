package com.tkiet.qms.repository;

import com.tkiet.qms.entity.Counter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterRepository extends JpaRepository<Counter, Long> {
    // no extra methods needed
    // JpaRepository already gives us: findAll(), findById(), save(), delete()
}
