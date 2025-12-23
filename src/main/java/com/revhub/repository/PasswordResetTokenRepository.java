package com.revhub.repository;
import com.revhub.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByEmailAndOtp(String email, String otp);
    Optional<PasswordResetToken> findByEmail(String email);
    List<PasswordResetToken> findByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
    void deleteByExpiryDateBefore(LocalDateTime date);
}
