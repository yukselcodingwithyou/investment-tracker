package com.yuksel.investmenttracker.repository;

import com.yuksel.investmenttracker.domain.entity.NotificationSubscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSubscriptionRepository extends MongoRepository<NotificationSubscription, String> {
    
    List<NotificationSubscription> findByUserIdAndEnabled(String userId, boolean enabled);
    
    Optional<NotificationSubscription> findByUserIdAndDeviceToken(String userId, String deviceToken);
    
    List<NotificationSubscription> findByEnabledTrue();
    
    void deleteByUserIdAndDeviceToken(String userId, String deviceToken);
}