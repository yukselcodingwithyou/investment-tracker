package com.yuksel.investmenttracker.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PortfolioHistoryResponse {
    private LocalDate date;
    private BigDecimal value;
    private BigDecimal change;
    private BigDecimal changePercent;
}