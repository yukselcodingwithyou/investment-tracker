package com.yuksel.investmenttracker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    void getExchangeRate_SameCurrency_ShouldReturnOne() {
        // When
        BigDecimal rate = currencyService.getExchangeRate("USD", "USD");

        // Then
        assertThat(rate).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void getExchangeRate_USD_TRY_ShouldReturnPositiveRate() {
        // When
        BigDecimal rate = currencyService.getExchangeRate("USD", "TRY");

        // Then
        assertThat(rate).isPositive();
        assertThat(rate).isGreaterThan(BigDecimal.valueOf(20)); // USD/TRY should be > 20
    }

    @Test
    void convertCurrency_ZeroAmount_ShouldReturnZero() {
        // When
        BigDecimal result = currencyService.convertCurrency(BigDecimal.ZERO, "USD", "TRY");

        // Then
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void convertCurrency_USD_TRY_ShouldConvertCorrectly() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(100);

        // When
        BigDecimal result = currencyService.convertCurrency(amount, "USD", "TRY");

        // Then
        assertThat(result).isPositive();
        assertThat(result).isGreaterThan(amount); // USD to TRY should increase value
    }

    @Test
    void formatCurrency_TRY_ShouldUseCorrectSymbol() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(1234.56);

        // When
        String formatted = currencyService.formatCurrency(amount, "TRY");

        // Then
        assertThat(formatted).startsWith("₺");
        assertThat(formatted).contains("1234.56");
    }

    @Test
    void formatCurrency_USD_ShouldUseCorrectSymbol() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(1234.56);

        // When
        String formatted = currencyService.formatCurrency(amount, "USD");

        // Then
        assertThat(formatted).startsWith("$");
        assertThat(formatted).contains("1234.56");
    }

    @Test
    void isSupportedCurrency_TRY_ShouldReturnTrue() {
        // When
        boolean supported = currencyService.isSupportedCurrency("TRY");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedCurrency_USD_ShouldReturnTrue() {
        // When
        boolean supported = currencyService.isSupportedCurrency("USD");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void updateExchangeRate_ShouldUpdateRate() {
        // Given
        BigDecimal newRate = BigDecimal.valueOf(32.0);

        // When
        currencyService.updateExchangeRate("USD", "TRY", newRate);
        BigDecimal retrievedRate = currencyService.getExchangeRate("USD", "TRY");

        // Then
        assertThat(retrievedRate).isEqualTo(newRate);
    }
}