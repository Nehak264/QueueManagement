package com.tkiet.qms.repository;

import com.tkiet.qms.entity.BonafideCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BonafideCertificateRepository extends JpaRepository<BonafideCertificate, Long> {

    // check if a certificate was already generated for a token
    Optional<BonafideCertificate> findByTokenId(Long tokenId);
}