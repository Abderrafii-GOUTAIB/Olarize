package com.olarize.employment.infrastructure.in.web.controller;

import com.olarize.employment.application.exception.EmployeeAccountLinkConflict;
import com.olarize.employment.application.exception.EmployeeAccountLinkUnavailable;
import com.olarize.employment.application.port.in.EmployeeAccountLinkUseCase;
import com.olarize.employment.infrastructure.in.web.dto.request.ChangeEmployeeAccountLinkRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.EmployeeAccountLinkResponseDto;
import com.olarize.employment.infrastructure.in.web.dto.response.MyEmployeeLinkResponseDto;
import com.olarize.employment.infrastructure.in.web.mapper.EmployeeAccountLinkWebMapper;
import com.olarize.identity.application.port.in.EmployeeLinkIdentityUseCase.AccountVerificationUnavailable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SecurityRequirement(name = "keycloak", scopes = "profile.read")
@RequestMapping("/api/v1/companies/{companyId}")
class EmployeeAccountLinkController {
    private final EmployeeAccountLinkUseCase links;
    EmployeeAccountLinkController(EmployeeAccountLinkUseCase links) { this.links = links; }

    @GetMapping(value = "/employee-account-links/{membershipId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Inspect a company membership's employee link",
            description = "Requires profile.read and current identity.link.admin. Returns IDs, state and current link version only; an existing membership without a link is UNLINKED/version0. No employee HR facts or private verification evidence are exposed.",
            responses = {@ApiResponse(responseCode = "200", description = "Current link metadata or UNLINKED"),
                @ApiResponse(responseCode = "401", description = "Authentication required"),
                @ApiResponse(responseCode = "403", description = "Dedicated company link-administration permission required"),
                @ApiResponse(responseCode = "404", description = "Membership unavailable in this company")})
    ResponseEntity<EmployeeAccountLinkResponseDto> read(@PathVariable UUID companyId, @PathVariable UUID membershipId) {
        return execute(() -> EmployeeAccountLinkWebMapper.response(links.read(companyId, membershipId)));
    }

    @PostMapping(value = "/employee-account-links/{membershipId}/changes", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirm an employee account link, replacement or revocation",
            description = "Requires profile.read and current identity.link.admin; self-linking is forbidden. LINK/RELINK requires the employee ID and a verified active membership; REVOKE omits employeeId and retains its prior target in history. Submit current expectedVersion (0 when unlinked), expectedMembershipVersion, reason, verificationEvidenceReference, UUID idempotencyKey and confirm=true. Keycloak subject verification and final membership/version checks prevent name/email inference. Link and immutable before/after audit commit atomically; exact retries return the original result. No HR/account permissions are granted.",
            responses = {@ApiResponse(responseCode = "200", description = "Confirmed link change or original retry result"),
                @ApiResponse(responseCode = "400", description = "Invalid unknown or unconfirmed input"),
                @ApiResponse(responseCode = "401", description = "Authentication required"),
                @ApiResponse(responseCode = "403", description = "Dedicated permission required; self linking prohibited"),
                @ApiResponse(responseCode = "404", description = "Membership/account or employee unavailable in the selected company"),
                @ApiResponse(responseCode = "409", description = "Stale versions, invalid transition, duplicate active link or conflicting retry"),
                @ApiResponse(responseCode = "503", description = "Identity verification service unavailable; no change committed")})
    ResponseEntity<EmployeeAccountLinkResponseDto> change(@PathVariable UUID companyId, @PathVariable UUID membershipId,
            @Valid @RequestBody ChangeEmployeeAccountLinkRequestDto request, HttpServletResponse response) {
        return execute(() -> EmployeeAccountLinkWebMapper.response(links.change(companyId, membershipId,
                EmployeeAccountLinkWebMapper.change(request), request.idempotencyKey(), response.getHeader("X-Request-ID"))));
    }

    @GetMapping(value = "/my-employee-link", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Resolve my current verified employee link",
            description = "Requires profile.read and an active company membership. Resolves only the authenticated membership's current active link and returns company/employee IDs, never HR fields. No subject, membership or employee argument can override identity. Revoked links/memberships take effect on the next request; a replacement resolves only the new target.",
            responses = {@ApiResponse(responseCode = "200", description = "Own current employee ID only"),
                @ApiResponse(responseCode = "401", description = "Authentication required"),
                @ApiResponse(responseCode = "403", description = "Active company membership required"),
                @ApiResponse(responseCode = "404", description = "No current verified employee link")})
    ResponseEntity<MyEmployeeLinkResponseDto> own(@PathVariable UUID companyId) {
        return execute(() -> EmployeeAccountLinkWebMapper.response(links.own(companyId)));
    }

    private static <T> ResponseEntity<T> execute(Supplier<T> operation) {
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(operation.get()); }
        catch (EmployeeAccountLinkUnavailable missing) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        catch (EmployeeAccountLinkConflict | ObjectOptimisticLockingFailureException conflict) { throw new ResponseStatusException(HttpStatus.CONFLICT); }
        catch (AccessDeniedException denied) { throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
        catch (AccountVerificationUnavailable unavailable) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE); }
        catch (IllegalArgumentException invalid) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
        catch (DataAccessException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql) {
                    if ("23505".equals(sql.getSQLState()) || "40P01".equals(sql.getSQLState())) throw new ResponseStatusException(HttpStatus.CONFLICT);
                    if ("42501".equals(sql.getSQLState())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
            throw failure;
        }
    }
}
