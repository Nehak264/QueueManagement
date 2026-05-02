package com.tkiet.qms.repository;

import com.tkiet.qms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // finds a student by their roll number
    // Spring generates: SELECT * FROM students WHERE roll_number = ?
    Student findByRollNumber(String rollNumber);
}