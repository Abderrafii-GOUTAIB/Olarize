package com.olarize.identity.application.port.out;

import com.olarize.identity.application.model.MembershipChangePreviewClaims;

public interface MembershipPreviewTokenPort {
    String issue(MembershipChangePreviewClaims claims);
    MembershipChangePreviewClaims verify(String token);
}
