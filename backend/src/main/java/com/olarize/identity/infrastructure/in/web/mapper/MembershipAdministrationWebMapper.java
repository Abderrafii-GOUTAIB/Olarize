package com.olarize.identity.infrastructure.in.web.mapper;

import com.olarize.identity.infrastructure.in.web.dto.response.ManagedMembershipResponseDto;
import com.olarize.identity.infrastructure.in.web.dto.response.MembershipPreviewResponseDto;
import com.olarize.identity.application.port.in.MembershipAdministrationUseCase;
import com.olarize.identity.domain.model.Membership;

public final class MembershipAdministrationWebMapper {
    private MembershipAdministrationWebMapper() { }

    public static ManagedMembershipResponseDto response(Membership member) {
        return new ManagedMembershipResponseDto(member.id(), member.subject(), member.companyId(), member.status(),
                member.roleBundle(), member.permissions(), member.version());
    }

    public static MembershipPreviewResponseDto response(MembershipAdministrationUseCase.Preview preview) {
        return new MembershipPreviewResponseDto(preview.token(), preview.previewId(), preview.companyId(), preview.targetSubject(),
                preview.action(), preview.roleBundle(), preview.permissions(), preview.actorVersion(), preview.targetVersion(), preview.expiresAt());
    }
}
