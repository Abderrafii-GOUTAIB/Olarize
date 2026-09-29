package com.olarize.identity.adapter.in.web;

import com.olarize.identity.application.port.in.MembershipAdministrationUseCase;
import com.olarize.identity.domain.exception.CompanyAccessDenied;
import com.olarize.identity.domain.exception.IdentityProviderUnavailable;
import com.olarize.identity.domain.exception.InvalidMembershipPreview;
import com.olarize.identity.domain.exception.MembershipChangeRejected;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/companies/{companyId}")
class MembershipAdministrationController {
    private final MembershipAdministrationUseCase administration;

    MembershipAdministrationController(MembershipAdministrationUseCase administration) {
        this.administration = administration;
    }

    @GetMapping(value = "/memberships", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List company memberships",
            description = "Lists each member's userId, current status, fixed role bundle, explicit permissions and version for one company. userId is the member's Keycloak user ID and can be copied into a preview request. Requires a valid Keycloak token with profile.read and an active identity.admin membership in that company. The company ID selects scope but does not grant access. Identity-administrator entries are visible but cannot be changed through this API.")
    ResponseEntity<List<ManagedMembershipResponse>> list(@PathVariable UUID companyId) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(administration.list(companyId).stream().map(ManagedMembershipResponse::from).toList());
        } catch (CompanyAccessDenied denied) {
            throw status(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping(value = "/membership-change-previews", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Preview a membership change",
            description = "Previews an exact grant, role change, reactivation or revocation. Copy userId from the membership row to choose the member. Grants and reactivations verify that Keycloak user ID. The signed preview contains the user, company, action, permissions, current versions and reason, expires after 30 minutes, and does not write membership state. Only HR_ADMIN, MANAGER, EMPLOYEE and PAYROLL_OPERATOR bundles are assignable; identity.admin cannot be granted or edited.")
    ResponseEntity<MembershipPreviewResponse> preview(@PathVariable UUID companyId,
            @Valid @RequestBody MembershipChangePreviewRequest request) {
        try {
            var preview = administration.preview(companyId, request.getUserId(), request.getRoleBundle(), request.getReason());
            return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                    .body(MembershipPreviewResponse.from(preview));
        } catch (CompanyAccessDenied denied) {
            throw status(HttpStatus.FORBIDDEN);
        } catch (IdentityProviderUnavailable unavailable) {
            throw status(HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MembershipChangeRejected rejected) {
            throw status(HttpStatus.CONFLICT);
        }
    }

    @PostMapping(value = "/membership-changes/confirm", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirm a membership change",
            description = "Confirms the exact, unexpired signed preview by submitting it with the expected target version, a UUID idempotency key and confirm=true. The active identity administrator and target versions are checked again. Membership change and before/after audit commit atomically. Repeating the same key and request returns its recorded result; stale, expired or conflicting retries return 409. Revocation and permission reduction take effect on the target's next request.")
    ResponseEntity<ManagedMembershipResponse> confirm(@PathVariable UUID companyId,
            @Valid @RequestBody ConfirmMembershipChangeRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        if (!Boolean.TRUE.equals(request.getConfirm())) throw status(HttpStatus.BAD_REQUEST);
        String correlationId = response.getHeader("X-Request-ID");
        try {
            var changed = administration.confirm(companyId, request.getPreviewToken(), request.getExpectedTargetVersion(),
                    request.getIdempotencyKey(), correlationId);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ManagedMembershipResponse.from(changed));
        } catch (CompanyAccessDenied denied) {
            throw status(HttpStatus.FORBIDDEN);
        } catch (IdentityProviderUnavailable unavailable) {
            throw status(HttpStatus.SERVICE_UNAVAILABLE);
        } catch (InvalidMembershipPreview | MembershipChangeRejected conflict) {
            throw status(HttpStatus.CONFLICT);
        }
    }

    private static ResponseStatusException status(HttpStatus status) {
        return new ResponseStatusException(status);
    }
}
