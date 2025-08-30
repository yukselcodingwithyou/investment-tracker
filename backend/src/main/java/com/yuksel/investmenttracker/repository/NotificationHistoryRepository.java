package com.yuksel.investmenttracker.repository;

import com.yuksel.investmenttracker.domain.entity.NotificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationHistoryRepository extends MongoRepository<NotificationHistory, String> {
    
    Page<NotificationHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    List<NotificationHistory> findByStatusAndCreatedAtBefore(String status, LocalDateTime before);
    
    long countByUserIdAndType(String userId, String type);
}