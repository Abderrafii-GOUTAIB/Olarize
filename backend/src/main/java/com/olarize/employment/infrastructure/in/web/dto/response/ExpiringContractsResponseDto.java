package com.olarize.employment.infrastructure.in.web.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpiringContractsResponseDto(UUID companyId, LocalDate fromDate, LocalDate toDate,
        LocalDate asOf, String timeZone, List<ExpiringContractResponseDto> items,
        ExpiringContractsPaginationResponseDto pagination, long currentContractsWithoutRecordedEndDate) {
    public ExpiringContractsResponseDto { items = List.copyOf(items); }

}
