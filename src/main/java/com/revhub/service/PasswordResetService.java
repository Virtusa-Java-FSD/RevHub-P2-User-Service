package com.revhub.service;

import com.revhub.exception.ResourceNotFoundException;
import com.revhub.model.PasswordResetToken;
import com.revhub.model.User;
import com.revhub.repository.PasswordResetTokenRepository;
import com.revhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Transactional
    public void sendOtp(String email) {
        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email address"));
        // Delete any existing tokens for this email
        tokenRepository.deleteByEmail(email);
        // Generate OTP
        String otp = generateOtp();
        // Create and save token
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setOtp(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        token.setVerified(false);
        tokenRepository.save(token);
        // Send OTP via email
        emailService.sendOtpEmail(email, otp);
        log.info("OTP sent to email: {}", email);
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        // Find token by email and OTP
        PasswordResetToken token = tokenRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new RuntimeException("Invalid OTP. Please try again."));
        // Check if token is expired
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }
        // Check if already verified
        if (token.getVerified()) {
            throw new RuntimeException("OTP has already been used.");
        }
        // Mark as verified
        token.setVerified(true);
        tokenRepository.save(token);
        log.info("OTP verified successfully for email: {}", email);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        // Find verified token
        PasswordResetToken token = tokenRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No password reset request found. Please request OTP first."));
        // Check if token is verified
        if (!token.getVerified()) {
            throw new RuntimeException("OTP not verified. Please verify OTP first.");
        }
        // Check if token is expired
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }
        // Find user and update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // Delete the token after successful password reset
        tokenRepository.delete(token);
        log.info("Password reset successfully for email: {}", email);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        log.info("Cleaned up expired password reset tokens");
    }
}
