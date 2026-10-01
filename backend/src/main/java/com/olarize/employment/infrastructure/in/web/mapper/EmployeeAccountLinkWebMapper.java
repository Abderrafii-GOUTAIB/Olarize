package com.olarize.employment.infrastructure.in.web.mapper;

import com.olarize.employment.application.port.in.EmployeeAccountLinkUseCase.Result;
import com.olarize.employment.application.port.in.EmployeeAccountLinkUseCase.OwnLink;
import com.olarize.employment.domain.model.EmployeeAccountLinkChange;
import com.olarize.employment.infrastructure.in.web.dto.request.ChangeEmployeeAccountLinkRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.EmployeeAccountLinkResponseDto;
import com.olarize.employment.infrastructure.in.web.dto.response.MyEmployeeLinkResponseDto;

public final class EmployeeAccountLinkWebMapper {
    private EmployeeAccountLinkWebMapper() { }
    public static EmployeeAccountLinkChange change(ChangeEmployeeAccountLinkRequestDto r) {
        return new EmployeeAccountLinkChange(r.action(), r.employeeId(), r.expectedVersion(), r.expectedMembershipVersion(),
                r.reason(), r.verificationEvidenceReference());
    }
    public static EmployeeAccountLinkResponseDto response(Result r) {
        return new EmployeeAccountLinkResponseDto(r.companyId(), r.membershipId(), r.employeeId(), r.status(), r.version(), r.recordedAt());
    }
    public static MyEmployeeLinkResponseDto response(OwnLink r) {
        return new MyEmployeeLinkResponseDto(r.companyId(), r.employeeId());
    }
}
