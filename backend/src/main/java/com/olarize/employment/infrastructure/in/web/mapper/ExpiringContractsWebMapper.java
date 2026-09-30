package com.olarize.employment.infrastructure.in.web.mapper;

import com.olarize.employment.infrastructure.in.web.dto.response.ExpiringContractResponseDto;
import com.olarize.employment.infrastructure.in.web.dto.response.ExpiringContractsResponseDto;

import com.olarize.employment.application.port.in.ExpiringContractsResult;
import com.olarize.employment.infrastructure.in.web.dto.response.ExpiringContractsPaginationResponseDto;

public final class ExpiringContractsWebMapper {
    private ExpiringContractsWebMapper() { }

    public static ExpiringContractsResponseDto response(ExpiringContractsResult result) {
        var items = result.items().stream().map(item -> new ExpiringContractResponseDto(item.contractId(),
                item.contractVersion(), item.employeeId(), item.employeeName(), item.employeeReference(),
                item.startDate(), item.endDate())).toList();
        return new ExpiringContractsResponseDto(result.companyId(), result.fromDate(), result.toDate(),
                result.asOf(), result.timeZone(), items,
                new ExpiringContractsPaginationResponseDto(result.page(), result.pageSize(),
                        result.totalItems(), result.totalPages()), result.currentContractsWithoutRecordedEndDate());
    }
}
