package com.olarize.employment.infrastructure.out.persistence.mapper;

import com.olarize.employment.application.port.in.EmployeeAccountLinkUseCase.Result;
import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeAccountLinkJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeAccountLinkAuditJpaEntity;

public final class EmployeeAccountLinkPersistenceMapper {
    private EmployeeAccountLinkPersistenceMapper() { }
    public static Result result(EmployeeAccountLinkJpaEntity link) {
        return new Result(link.getCompanyId(), link.getMembershipId(), link.getEmployeeId(), link.getStatus(), link.getVersion(), link.getRecordedAt());
    }
    public static Result result(EmployeeAccountLinkAuditJpaEntity audit) {
        return new Result(audit.getCompanyId(), audit.getMembershipId(), audit.getEmployeeId(), audit.getStatus(), audit.getVersion(), audit.getRecordedAt());
    }
}
