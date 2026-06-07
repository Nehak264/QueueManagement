package com.tkiet.qms.service;

import com.tkiet.qms.entity.*;
import com.tkiet.qms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FeePaymentService {

    @Autowired
    private FeePaymentRepository feePaymentRepository;

    @Autowired
    private TokenRepository tokenRepository;

    // Create payment entry at booking time (unpaid, no receipt number yet)
    public FeePayment createPayment(Token token, String feeType, Double amount, String paymentMode, String referenceNumber) {
        FeePayment payment = new FeePayment();
        payment.setToken(token);
        payment.setFeeType(feeType);
        payment.setAmount(amount != null ? amount : 0.0);
        payment.setPaymentMode(paymentMode != null ? paymentMode : "Cash");
        payment.setReferenceNumber(referenceNumber != null ? referenceNumber : "");
        payment.setReceiptNumber(null);
        payment.setPaidAt(null);
        return feePaymentRepository.save(payment);
    }

    // Finalize payment when token is DONE (assign receipt number, record paid timestamp)
    public FeePayment finalizePayment(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found: " + tokenId));

        if (token.getStatus() != TokenStatus.DONE) {
            throw new RuntimeException("Token must be DONE before finalizing fee payment");
        }

        FeePayment payment = feePaymentRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("Fee payment details not found for token: " + tokenId));

        // Generate receipt number if not already present
        if (payment.getReceiptNumber() == null) {
            String receiptNumber = "TKIET/CSE/FEE/" +
                    java.time.Year.now().getValue() + "/" +
                    String.format("%03d", token.getTokenNumber());
            payment.setReceiptNumber(receiptNumber);
            payment.setPaidAt(LocalDateTime.now());
            return feePaymentRepository.save(payment);
        }

        return payment;
    }

    // Retrieve payment by token ID
    public Optional<FeePayment> getPaymentByTokenId(Long tokenId) {
        return feePaymentRepository.findByTokenId(tokenId);
    }
}
