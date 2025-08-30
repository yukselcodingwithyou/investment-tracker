package com.yuksel.investmenttracker.repository;

import com.yuksel.investmenttracker.domain.entity.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
    
    Optional<PasswordResetToken> findByTokenAndUsedFalseAndExpiresAtAfter(String token, LocalDateTime now);
    
    void deleteByUserId(String userId);
    
    void deleteByExpiresAtBefore(LocalDateTime expiredBefore);
}