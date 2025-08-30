package com.yuksel.investmenttracker.repository;

import com.yuksel.investmenttracker.domain.entity.AuthToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends MongoRepository<AuthToken, String> {
    Optional<AuthToken> findByRefreshTokenHash(String refreshTokenHash);
    Optional<AuthToken> findByRefreshTokenHashAndExpiresAtAfter(String refreshTokenHash, LocalDateTime now);
    void deleteByUserId(String userId);
}