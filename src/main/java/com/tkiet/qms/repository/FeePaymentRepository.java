package com.tkiet.qms.repository;

import com.tkiet.qms.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    // Find the fee payment record associated with a specific token
    Optional<FeePayment> findByTokenId(Long tokenId);
}
