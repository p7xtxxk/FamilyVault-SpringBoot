package com.familyvault.service;

import com.familyvault.model.OtpCode;
import com.familyvault.repository.OtpCodeRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class OtpService {

    private final OtpCodeRepository otpRepo;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String smtpUser;

    @Value("${app.app-name}")
    private String appName;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_REQUESTS_PER_HOUR = 5;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeRepository otpRepo, JavaMailSender mailSender) {
        this.otpRepo = otpRepo;
        this.mailSender = mailSender;
    }

    public String generateOtp() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public boolean checkRateLimit(String email) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long count = otpRepo.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        return count < MAX_OTP_REQUESTS_PER_HOUR;
    }

    public void storeOtp(String email, String otp) {
        otpRepo.deleteAllByEmail(email);
        Instant now = Instant.now();
        OtpCode code = new OtpCode(email, otp, now, now.plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        otpRepo.save(code);
    }

    public boolean verifyOtp(String email, String otp) {
        var record = otpRepo.findByEmailAndOtpAndExpiresAtAfter(email, otp, Instant.now());
        if (record.isPresent()) {
            otpRepo.deleteAllByEmail(email);
            return true;
        }
        return false;
    }

    public boolean sendOtpEmail(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(smtpUser);
            helper.setTo(email);
            helper.setSubject("Your " + appName + " Login Code");

            String htmlBody = """
                <html>
                <body style="font-family: Georgia, serif; background: #f9f6f0; padding: 40px;">
                  <div style="max-width: 480px; margin: 0 auto; background: #fff; border-radius: 12px;
                              padding: 40px; border: 1px solid #e8e0d0; box-shadow: 0 4px 20px rgba(0,0,0,0.06);">
                    <h2 style="color: #2c1810; font-size: 24px; margin-bottom: 8px;"> %s</h2>
                    <p style="color: #6b5c4e; font-size: 15px; margin-bottom: 28px;">
                      Your one time login code is:
                    </p>
                    <div style="background: #2c1810; color: #f5efe6; font-size: 36px; font-weight: bold;
                                letter-spacing: 12px; text-align: center; padding: 20px; border-radius: 8px;
                                margin-bottom: 24px;">
                      %s
                    </div>
                    <p style="color: #9e8e80; font-size: 13px;">
                      code expires in <strong>%d minutes</strong>.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(appName, otp, OTP_EXPIRY_MINUTES);

            helper.setText(htmlBody, true);
            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            System.err.println("[EMAIL ERROR] " + e.getMessage());
            return false;
        }
    }
}
