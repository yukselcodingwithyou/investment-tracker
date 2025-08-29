package com.yuksel.investmenttracker.repository;

import com.yuksel.investmenttracker.domain.entity.Asset;
import com.yuksel.investmenttracker.domain.enums.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends MongoRepository<Asset, String> {
    Optional<Asset> findBySymbol(String symbol);
    
    Page<Asset> findByType(AssetType type, Pageable pageable);
    
    @Query("{'$or': [{'symbol': {'$regex': ?0, '$options': 'i'}}, {'name': {'$regex': ?0, '$options': 'i'}}]}")
    Page<Asset> findBySymbolOrNameContainingIgnoreCase(String search, Pageable pageable);
    
    @Query("{'type': ?0, '$or': [{'symbol': {'$regex': ?1, '$options': 'i'}}, {'name': {'$regex': ?1, '$options': 'i'}}]}")
    Page<Asset> findByTypeAndSymbolOrNameContainingIgnoreCase(AssetType type, String search, Pageable pageable);
}