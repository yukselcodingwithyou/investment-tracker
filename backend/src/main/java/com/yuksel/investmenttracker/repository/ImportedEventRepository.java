package com.yuksel.investmenttracker.repository;

import com.yuksel.investmenttracker.domain.entity.ImportedEvent;
import com.yuksel.investmenttracker.domain.enums.ImportedEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportedEventRepository extends MongoRepository<ImportedEvent, String> {
    List<ImportedEvent> findByUserIdAndAssetId(String userId, String assetId);
    Page<ImportedEvent> findByUserId(String userId, Pageable pageable);
    Page<ImportedEvent> findByUserIdAndType(String userId, ImportedEventType type, Pageable pageable);
}