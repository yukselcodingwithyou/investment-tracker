package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.AcquisitionLot;
import com.yuksel.investmenttracker.domain.entity.Asset;
import com.yuksel.investmenttracker.domain.enums.AssetType;
import com.yuksel.investmenttracker.dto.response.PortfolioAnalyticsResponse;
import com.yuksel.investmenttracker.dto.response.PortfolioSummaryResponse;
import com.yuksel.investmenttracker.repository.AcquisitionLotRepository;
import com.yuksel.investmenttracker.repository.AssetRepository;
import com.yuksel.investmenttracker.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private AcquisitionLotRepository acquisitionLotRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PriceService priceService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PortfolioService portfolioService;

    private final String TEST_USER_ID = "test-user-123";
    private final String TEST_ASSET_ID = "test-asset-456";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        UserPrincipal userPrincipal = new UserPrincipal(
                TEST_USER_ID,
                "Test User",
                "test@example.com",
                "hashedPassword",
                Collections.emptyList(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
    }

    @Test
    void getPortfolioSummary_WithEmptyPortfolio_ShouldReturnEmptyResponse() {
        // Given
        when(acquisitionLotRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

        // When
        PortfolioSummaryResponse result = portfolioService.getPortfolioSummary();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalValueTRY()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo("NEUTRAL");
        assertThat(result.getTotalUnrealizedPLTRY()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getPortfolioSummary_WithAcquisitions_ShouldCalculateCorrectly() {
        // Given
        List<AcquisitionLot> acquisitions = createMockAcquisitions();
        when(acquisitionLotRepository.findByUserId(TEST_USER_ID)).thenReturn(acquisitions);
        when(priceService.getCurrentPrice(anyString(), anyString())).thenReturn(BigDecimal.valueOf(110.0));

        // When
        PortfolioSummaryResponse result = portfolioService.getPortfolioSummary();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalValueTRY()).isPositive();
        assertThat(result.getStatus()).isIn("UP", "DOWN", "NEUTRAL");
        verify(priceService, atLeastOnce()).getCurrentPrice(anyString(), eq("TRY"));
    }

    @Test
    void getPortfolioAnalytics_ShouldReturnComprehensiveData() {
        // Given
        List<AcquisitionLot> acquisitions = createMockAcquisitions();
        when(acquisitionLotRepository.findByUserId(TEST_USER_ID)).thenReturn(acquisitions);
        when(priceService.getCurrentPrice(anyString(), anyString())).thenReturn(BigDecimal.valueOf(105.0));
        when(assetRepository.findById(anyString())).thenReturn(Optional.of(createMockAsset()));

        // When
        PortfolioAnalyticsResponse result = portfolioService.getPortfolioAnalytics("30D");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPortfolioHistory()).isNotEmpty();
        assertThat(result.getAssetAllocation()).isNotEmpty();
        assertThat(result.getTopMovers()).isNotEmpty();
        assertThat(result.getVolatility()).isNotNull();
        assertThat(result.getSharpeRatio()).isNotNull();
        assertThat(result.getMaxDrawdown()).isNotNull();
    }

    @Test
    void getAssetAllocation_ShouldGroupByAssetType() {
        // Given
        List<AcquisitionLot> acquisitions = createMockAcquisitions();
        when(acquisitionLotRepository.findByUserId(TEST_USER_ID)).thenReturn(acquisitions);
        when(priceService.getCurrentPrice(anyString(), anyString())).thenReturn(BigDecimal.valueOf(100.0));
        when(assetRepository.findById(anyString())).thenReturn(Optional.of(createMockAsset()));

        // When
        var result = portfolioService.getAssetAllocation();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getAssetType()).isNotNull();
        assertThat(result.get(0).getValue()).isPositive();
        assertThat(result.get(0).getPercentage()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    private List<AcquisitionLot> createMockAcquisitions() {
        AcquisitionLot acquisition1 = new AcquisitionLot();
        acquisition1.setId("acq1");
        acquisition1.setUserId(TEST_USER_ID);
        acquisition1.setAssetId(TEST_ASSET_ID);
        acquisition1.setQuantity(BigDecimal.valueOf(10));
        acquisition1.setUnitPrice(BigDecimal.valueOf(100));
        acquisition1.setCurrency("USD");
        acquisition1.setFee(BigDecimal.valueOf(5));
        acquisition1.setAcquisitionDate(LocalDate.now().minusDays(30));
        acquisition1.setCreatedAt(LocalDateTime.now());
        acquisition1.setUpdatedAt(LocalDateTime.now());

        return Arrays.asList(acquisition1);
    }

    private Asset createMockAsset() {
        Asset asset = new Asset();
        asset.setId(TEST_ASSET_ID);
        asset.setSymbol("AAPL");
        asset.setName("Apple Inc.");
        asset.setType(AssetType.EQUITY);
        asset.setCurrency("USD");
        return asset;
    }
}