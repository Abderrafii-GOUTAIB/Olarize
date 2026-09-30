package com.olarize.identity.infrastructure.in.web.mapper;

import com.olarize.identity.infrastructure.in.web.dto.response.CompanyContextResponseDto;
import com.olarize.identity.application.port.in.CompanyContext;

public final class CompanyContextWebMapper {
    private CompanyContextWebMapper() { }

    public static CompanyContextResponseDto response(CompanyContext context) {
        return new CompanyContextResponseDto(context.membershipId(), context.companyId(),
                context.companyName(), context.permissions(), context.version());
    }
}
