package com.olarize.employment.infrastructure.in.web.mapper;

import com.olarize.employment.application.port.in.EmployeeDetailResult;
import com.olarize.employment.application.port.in.EmployeeDirectoryResult;
import com.olarize.employment.application.port.in.EmployeeRecord;
import com.olarize.employment.domain.model.EmployeeDirectoryFilter;
import com.olarize.employment.infrastructure.in.web.dto.request.EmployeeDirectoryRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.EmployeeDetailResponseDto;
import com.olarize.employment.infrastructure.in.web.dto.response.EmployeeDirectoryPaginationResponseDto;
import com.olarize.employment.infrastructure.in.web.dto.response.EmployeeDirectoryResponseDto;
import com.olarize.employment.infrastructure.in.web.dto.response.EmployeeSummaryResponseDto;

public final class EmployeeDirectoryWebMapper {
    private EmployeeDirectoryWebMapper() { }

    public static EmployeeDirectoryFilter filter(EmployeeDirectoryRequestDto input) {
        return new EmployeeDirectoryFilter(input.search(), input.status(), input.page(), input.pageSize());
    }

    public static EmployeeDirectoryResponseDto response(EmployeeDirectoryResult result) {
        return new EmployeeDirectoryResponseDto(result.companyId(), result.asOf(), result.timeZone(),
                result.records().items().stream().map(EmployeeDirectoryWebMapper::summary).toList(),
                new EmployeeDirectoryPaginationResponseDto(result.page(), result.pageSize(),
                        result.records().totalItems(), result.totalPages()));
    }

    private static EmployeeSummaryResponseDto summary(EmployeeRecord record) {
        return new EmployeeSummaryResponseDto(record.employeeId(), record.reference(), record.name(), record.jobTitle(),
                record.department(), record.site(), record.status(), record.episodeId(), record.startDate(),
                record.lastEmployedDay(), record.version(), record.episodeVersion());
    }

    public static EmployeeDetailResponseDto response(EmployeeDetailResult result) {
        var record = result.employee();
        return new EmployeeDetailResponseDto(record.employeeId(), record.companyId(), record.reference(), record.name(),
                record.jobTitle(), record.department(), record.site(), record.workEmail(), record.workPhone(),
                record.version(), record.recordedAt(), record.episodeId(), record.startDate(), record.lastEmployedDay(),
                record.episodeVersion(), record.status(), result.asOf(), result.timeZone());
    }
}
