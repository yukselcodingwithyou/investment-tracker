package com.yuksel.investmenttracker.repository;

import com.yuksel.investmenttracker.domain.entity.PriceSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceSnapshotRepository extends MongoRepository<PriceSnapshot, String> {
    
    @Query(value = "{'assetId': ?0}", sort = "{'asOf': -1}")
    Optional<PriceSnapshot> findLatestByAssetId(String assetId);
    
    @Query(value = "{'assetId': ?0, 'asOf': {'$gte': ?1, '$lte': ?2}}", sort = "{'asOf': 1}")
    List<PriceSnapshot> findByAssetIdAndAsOfBetween(String assetId, LocalDateTime start, LocalDateTime end);
    
    @Query(value = "{'assetId': {'$in': ?0}}", sort = "{'asOf': -1}")
    List<PriceSnapshot> findLatestByAssetIds(List<String> assetIds);
}