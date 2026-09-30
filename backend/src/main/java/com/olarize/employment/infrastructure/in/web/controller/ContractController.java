package com.olarize.employment.infrastructure.in.web.controller;

import com.olarize.employment.infrastructure.in.web.dto.request.CorrectContractRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.request.CreateContractRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.ContractResponseDto;
import com.olarize.employment.infrastructure.in.web.mapper.ContractWebMapper;

import com.olarize.employment.application.port.in.CreateContractUseCase;
import com.olarize.employment.application.port.in.CorrectContractUseCase;
import com.olarize.employment.application.exception.ContractFactsConflict;
import com.olarize.employment.application.exception.ContractFactsNotFound;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/contracts")
class ContractController {
    private final CreateContractUseCase create;
    private final CorrectContractUseCase correct;

    ContractController(CreateContractUseCase create, CorrectContractUseCase correct) {
        this.create = create;
        this.correct = correct;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Record contract facts for an employment episode",
            description = "HR confirms supplied contract type, dates and source reference for the selected employee episode. Requires a valid Keycloak access token with profile.read and current contract.write membership in the company. Contract facts are stored as UNREVIEWED; no legal validity, renewal, compensation, employment status or login decision is made. An omitted end date is NOT_RECORDED, not a guessed expiry. Same-key retries return the original version.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Contract facts and version 1 recorded atomically with audit evidence"),
                @ApiResponse(responseCode = "400", description = "Invalid, unknown or unconfirmed fields"),
                @ApiResponse(responseCode = "401", description = "Keycloak authentication required"),
                @ApiResponse(responseCode = "403", description = "Current company contract.write permission required"),
                @ApiResponse(responseCode = "404", description = "Employee or episode is unavailable in this company"),
                @ApiResponse(responseCode = "409", description = "Conflicting retry")})
    ResponseEntity<ContractResponseDto> create(
            @Parameter(description = "Requested company scope; access is verified from the current membership", example = "c0000000-0000-4000-8000-000000000001") @PathVariable UUID companyId,
            @Parameter(description = "Employee record that owns the employment episode", example = "c0000000-0000-4000-8000-000000000070") @PathVariable UUID employeeId,
            @Parameter(description = "Selected employment period; this API does not alter episode dates", example = "c0000000-0000-4000-8000-000000000071") @PathVariable UUID episodeId,
            @Valid @RequestBody CreateContractRequestDto request, HttpServletResponse response) {
        try {
            var saved = create.create(companyId, employeeId, episodeId, ContractWebMapper.facts(request),
                    request.idempotencyKey(), response.getHeader("X-Request-ID"));
            return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                    .body(ContractWebMapper.response(saved));
        } catch (AccessDeniedException denied) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (ContractFactsConflict conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (DataIntegrityViolationException invalidReference) {
            if (isEpisodeReferenceFailure(invalidReference)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            throw invalidReference;
        }
    }

    @PostMapping(path = "/{contractId}/corrections", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Correct contract facts without losing the prior version",
            description = "HR submits the full corrected facts, the current expectedVersion, a reason and an evidence reference. Requires current contract.write membership and HR confirmation. A correction appends a new UNREVIEWED version; a stale version conflicts. It does not renew or assess legal validity and never changes employment or login state. Same-key retries return the version originally created by that correction.",
            responses = {
                @ApiResponse(responseCode = "200", description = "A new contract facts version and audit evidence were committed"),
                @ApiResponse(responseCode = "400", description = "Invalid, unknown or unconfirmed fields"),
                @ApiResponse(responseCode = "401", description = "Keycloak authentication required"),
                @ApiResponse(responseCode = "403", description = "Current company contract.write permission required"),
                @ApiResponse(responseCode = "404", description = "Contract is unavailable in this company or episode"),
                @ApiResponse(responseCode = "409", description = "Stale expectedVersion or conflicting retry")})
    ResponseEntity<ContractResponseDto> correct(
            @Parameter(description = "Requested company scope", example = "c0000000-0000-4000-8000-000000000001") @PathVariable UUID companyId,
            @Parameter(description = "Employee record that owns the episode", example = "c0000000-0000-4000-8000-000000000070") @PathVariable UUID employeeId,
            @Parameter(description = "Selected employment period", example = "c0000000-0000-4000-8000-000000000071") @PathVariable UUID episodeId,
            @Parameter(description = "Contract fact record to correct", example = "c0000000-0000-4000-8000-000000000073") @PathVariable UUID contractId,
            @Valid @RequestBody CorrectContractRequestDto request, HttpServletResponse response) {
        try {
            var saved = correct.correct(companyId, employeeId, episodeId, contractId,
                    request.expectedVersion(), ContractWebMapper.facts(request), request.reason(),
                    request.evidenceReference(), request.idempotencyKey(), response.getHeader("X-Request-ID"));
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ContractWebMapper.response(saved));
        } catch (AccessDeniedException denied) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (ContractFactsNotFound missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (ContractFactsConflict conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean isEpisodeReferenceFailure(DataIntegrityViolationException failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause.getMessage() != null && (cause.getMessage().contains("employment_contract_employee_fk")
                    || cause.getMessage().contains("employment_contract_episode_fk"))) return true;
            cause = cause.getCause();
        }
        return false;
    }
}
