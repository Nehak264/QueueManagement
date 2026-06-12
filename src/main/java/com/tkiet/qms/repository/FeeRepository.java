package com.tkiet.qms.repository;

import com.tkiet.qms.entity.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeRepository extends JpaRepository<FeeRecord, Long> {

    // All fee records for a student (all semesters)
    List<FeeRecord> findByStudentIdOrderBySemesterAsc(Long studentId);

    // Fee record for a specific student + semester + year
    FeeRecord findByStudentIdAndSemesterAndAcademicYear(Long studentId, int semester, String academicYear);
}