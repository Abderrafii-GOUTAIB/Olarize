package com.olarize.employment.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record ContractExpiryWindow(LocalDate fromDate, LocalDate toDate) {
    public ContractExpiryWindow {
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)
                || ChronoUnit.DAYS.between(fromDate, toDate) >= 366) {
            throw new IllegalArgumentException("Invalid contract expiry date window");
        }
    }
}
