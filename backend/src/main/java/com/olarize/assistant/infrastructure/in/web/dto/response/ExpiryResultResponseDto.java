package com.olarize.assistant.infrastructure.in.web.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpiryResultResponseDto(UUID companyId, LocalDate fromDate, LocalDate toDate, LocalDate asOf,
        String timeZone, List<ExpiryContractResponseDto> items, int page, int pageSize,
        long totalItems, int totalPages, long currentContractsWithoutRecordedEndDate) {
    public ExpiryResultResponseDto { items = List.copyOf(items); }
}
