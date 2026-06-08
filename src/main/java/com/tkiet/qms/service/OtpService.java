package com.tkiet.qms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    private static final long OTP_VALIDITY_MS = 5 * 60 * 1000;

    public void sendOtp(String email) {

        String otp = String.format("%06d",
                new Random().nextInt(900000) + 100000);

        otpStore.put(email, otp);
        otpExpiry.put(email,
                System.currentTimeMillis() + OTP_VALIDITY_MS);

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(email);
            message.setSubject("TKIET Bonafide Portal OTP");
            message.setText(
                    "Your OTP is: " + otp +
                            "\n\nValid for 5 minutes." +
                            "\nDo not share it with anyone."
            );

            mailSender.send(message);

            System.out.println("OTP sent to " + email);

        } catch (Exception e) {

            otpStore.remove(email);
            otpExpiry.remove(email);

            throw new RuntimeException(
                    "Failed to send OTP: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otp) {

        Long expiry = otpExpiry.get(email);

        if (expiry == null ||
                System.currentTimeMillis() > expiry) {

            otpStore.remove(email);
            otpExpiry.remove(email);

            return false;
        }

        if (otpStore.containsKey(email)
                && otpStore.get(email).equals(otp)) {

            otpStore.remove(email);
            otpExpiry.remove(email);

            return true;
        }

        return false;
    }
}