package com.yuksel.investmenttracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    // In-memory cache for exchange rates (in real implementation, this would come from external API)
    private final Map<String, BigDecimal> exchangeRates = new ConcurrentHashMap<>();

    {
        // Initialize with some default rates (these would be fetched from real APIs)
        exchangeRates.put("USD_TRY", BigDecimal.valueOf(31.5));
        exchangeRates.put("EUR_TRY", BigDecimal.valueOf(34.2));
        exchangeRates.put("GBP_TRY", BigDecimal.valueOf(39.8));
        exchangeRates.put("JPY_TRY", BigDecimal.valueOf(0.21));
        exchangeRates.put("TRY_TRY", BigDecimal.valueOf(1.0));
        
        // Reverse rates
        exchangeRates.put("TRY_USD", BigDecimal.valueOf(1.0).divide(BigDecimal.valueOf(31.5), 6, RoundingMode.HALF_UP));
        exchangeRates.put("TRY_EUR", BigDecimal.valueOf(1.0).divide(BigDecimal.valueOf(34.2), 6, RoundingMode.HALF_UP));
        exchangeRates.put("TRY_GBP", BigDecimal.valueOf(1.0).divide(BigDecimal.valueOf(39.8), 6, RoundingMode.HALF_UP));
        exchangeRates.put("TRY_JPY", BigDecimal.valueOf(1.0).divide(BigDecimal.valueOf(0.21), 6, RoundingMode.HALF_UP));
    }

    @Cacheable(value = "exchange-rates", key = "#fromCurrency + '_' + #toCurrency")
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        String key = fromCurrency + "_" + toCurrency;
        BigDecimal rate = exchangeRates.get(key);
        
        if (rate != null) {
            log.debug("Exchange rate {}: {}", key, rate);
            return rate;
        }

        // Try inverse rate
        String inverseKey = toCurrency + "_" + fromCurrency;
        BigDecimal inverseRate = exchangeRates.get(inverseKey);
        if (inverseRate != null) {
            rate = BigDecimal.ONE.divide(inverseRate, 6, RoundingMode.HALF_UP);
            exchangeRates.put(key, rate); // Cache the calculated rate
            log.debug("Calculated exchange rate {} from inverse: {}", key, rate);
            return rate;
        }

        // If no direct or inverse rate found, try cross-currency conversion via TRY
        if (!fromCurrency.equals("TRY") && !toCurrency.equals("TRY")) {
            BigDecimal fromToTRY = exchangeRates.get(fromCurrency + "_TRY");
            BigDecimal TRYToTarget = exchangeRates.get("TRY_" + toCurrency);
            
            if (fromToTRY != null && TRYToTarget != null) {
                rate = fromToTRY.multiply(TRYToTarget);
                exchangeRates.put(key, rate); // Cache the calculated rate
                log.debug("Cross-calculated exchange rate {} via TRY: {}", key, rate);
                return rate;
            }
        }

        log.warn("Exchange rate not found for {}, returning 1.0", key);
        return BigDecimal.ONE; // Default to 1:1 if rate not found
    }

    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        BigDecimal convertedAmount = amount.multiply(rate);
        
        log.debug("Converted {} {} to {} {} at rate {}", 
                amount, fromCurrency, convertedAmount, toCurrency, rate);
        
        return convertedAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public void updateExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        String key = fromCurrency + "_" + toCurrency;
        exchangeRates.put(key, rate);
        
        // Also store the inverse rate
        String inverseKey = toCurrency + "_" + fromCurrency;
        BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 6, RoundingMode.HALF_UP);
        exchangeRates.put(inverseKey, inverseRate);
        
        log.info("Updated exchange rate {}: {}", key, rate);
    }

    public void refreshExchangeRates() {
        // TODO: Implement integration with external exchange rate APIs
        // This would typically call services like:
        // - Fixer.io
        // - Open Exchange Rates
        // - Currency Layer
        // - Alpha Vantage
        log.info("Exchange rate refresh requested - would fetch from external APIs in production");
    }

    public String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) {
            return "0.00";
        }

        return switch (currency.toUpperCase()) {
            case "TRY" -> "₺" + String.format("%.2f", amount);
            case "USD" -> "$" + String.format("%.2f", amount);
            case "EUR" -> "€" + String.format("%.2f", amount);
            case "GBP" -> "£" + String.format("%.2f", amount);
            case "JPY" -> "¥" + String.format("%.0f", amount);
            default -> currency + " " + String.format("%.2f", amount);
        };
    }

    public boolean isSupportedCurrency(String currency) {
        return exchangeRates.containsKey(currency + "_TRY") || 
               exchangeRates.containsKey("TRY_" + currency) ||
               currency.equals("TRY");
    }
}