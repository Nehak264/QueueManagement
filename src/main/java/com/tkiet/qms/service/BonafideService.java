package com.tkiet.qms.service;

import com.tkiet.qms.entity.*;
import com.tkiet.qms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BonafideService {

    @Autowired
    private BonafideCertificateRepository certRepository;

    @Autowired
    private TokenRepository tokenRepository;

    public BonafideCertificate saveCertificate(Token token, String refNumber, String academicYear) {
        BonafideCertificate cert = new BonafideCertificate();
        cert.setToken(token);
        cert.setRefNumber(refNumber);
        cert.setAcademicYear(academicYear);
        cert.setGeneratedAt(java.time.LocalDateTime.now());
        return certRepository.save(cert);
    }

    public BonafideCertificate getCertificate(Long tokenId) {
        return certRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));
    }
}