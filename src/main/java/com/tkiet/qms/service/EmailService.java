package com.tkiet.qms.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendCertificateEmail(String toEmail, String studentName, byte[] pdfBytes, String refNumber) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Bonafide Certificate - TKIET Warananangar");
            helper.setText("Dear " + studentName + ",\n\nPlease find attached your bonafide certificate (Ref: " + refNumber + ").\n\nThis is a digitally generated document.");

            helper.addAttachment("Bonafide_Certificate_" + studentName.replace(" ", "_") + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public void sendRejectionEmail(String toEmail, String studentName, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false);

            helper.setTo(toEmail);
            helper.setSubject("Application Status Update - TKIET Warananangar");
            helper.setText("Dear " + studentName + ",\n\nYour application for a bonafide certificate has been rejected.\nReason: " + reason + "\n\nPlease correct the details and apply again.");

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
