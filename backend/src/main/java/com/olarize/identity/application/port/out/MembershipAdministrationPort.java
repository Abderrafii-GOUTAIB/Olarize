package com.olarize.identity.application.port.out;

import com.olarize.identity.application.model.MembershipChangePreviewClaims;
import com.olarize.identity.domain.model.Membership;
import java.util.List;
import java.util.UUID;

public interface MembershipAdministrationPort {
    List<Membership> list(String actorSubject, UUID companyId);
    Membership findCommittedRetry(String actorSubject, MembershipChangePreviewClaims preview, UUID idempotencyKey);
    Membership change(String actorSubject, MembershipChangePreviewClaims preview, UUID idempotencyKey, String correlationId);
}
