package com.olarize.employment.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpiringContractsResult(UUID companyId, LocalDate fromDate, LocalDate toDate,
        LocalDate asOf, String timeZone, List<ExpiringContract> items,
        int page, int pageSize, long totalItems, int totalPages,
        long currentContractsWithoutRecordedEndDate) {
    public ExpiringContractsResult { items = List.copyOf(items); }
}
