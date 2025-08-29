package com.yuksel.investmenttracker.repository;

import com.yuksel.investmenttracker.domain.entity.AcquisitionLot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcquisitionLotRepository extends MongoRepository<AcquisitionLot, String> {
    List<AcquisitionLot> findByUserIdAndAssetId(String userId, String assetId);
    List<AcquisitionLot> findByUserId(String userId);
    Page<AcquisitionLot> findByUserId(String userId, Pageable pageable);
    Page<AcquisitionLot> findByUserIdAndAssetId(String userId, String assetId, Pageable pageable);
}