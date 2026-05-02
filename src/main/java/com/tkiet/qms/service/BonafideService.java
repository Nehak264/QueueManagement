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

    // generate a certificate for a token
    public BonafideCertificate generateCertificate(Long tokenId, String academicYear) {

        // 1. find the token
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        // 2. check if certificate already exists for this token
        if (certRepository.findByTokenId(tokenId).isPresent()) {
            throw new RuntimeException("Certificate already generated for this token");
        }

        // 3. check token is done before generating cert
        if (token.getStatus() != TokenStatus.DONE) {
            throw new RuntimeException("Token must be DONE before generating certificate");
        }

        // 4. generate ref number: TKIET/CSE/BON/2026/047
        String refNumber = "TKIET/CSE/BON/" +
                java.time.Year.now().getValue() + "/" +
                String.format("%03d", token.getTokenNumber());

        // 5. create and save certificate
        BonafideCertificate cert = new BonafideCertificate();
        cert.setToken(token);
        cert.setRefNumber(refNumber);
        cert.setAcademicYear(academicYear);

        return certRepository.save(cert);
    }

    // get certificate by token id
    public BonafideCertificate getCertificate(Long tokenId) {
        return certRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));
    }
}