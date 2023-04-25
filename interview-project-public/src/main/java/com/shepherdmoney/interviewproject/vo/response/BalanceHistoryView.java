package com.shepherdmoney.interviewproject.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class BalanceHistoryView {
    private String creditCardNumber;
    private Instant date;
    private double balance;
}
