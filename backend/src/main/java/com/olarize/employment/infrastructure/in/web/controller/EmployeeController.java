package com.olarize.employment.infrastructure.in.web.controller;

import com.olarize.employment.infrastructure.in.web.dto.request.CreateEmployeeRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.CreatedEmployeeResponseDto;
import com.olarize.employment.infrastructure.in.web.mapper.EmployeeWebMapper;

import com.olarize.employment.application.port.in.CreateEmployeeUseCase;
import com.olarize.employment.application.exception.EmployeeCreationConflict;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/companies/{companyId}/employees")
class EmployeeController {
    private final CreateEmployeeUseCase create;

    EmployeeController(CreateEmployeeUseCase create) { this.create = create; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a minimum employee record",
            description = "HR confirms a supplied name, company-unique reference and employment start date. Requires a valid Keycloak token with profile.read and current employee.write permission in the selected company. Creates one employee, first episode and atomic audit; returns stable IDs, dates, status as of Africa/Casablanca today and version 1. An equal idempotency retry returns the same result. This records HR facts only: it does not create a login, contract or payroll eligibility.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Employee, episode and audit committed"),
                @ApiResponse(responseCode = "400", description = "Invalid, unknown or unconfirmed fields"),
                @ApiResponse(responseCode = "401", description = "Keycloak authentication required"),
                @ApiResponse(responseCode = "403", description = "Current company HR permission required"),
                @ApiResponse(responseCode = "409", description = "Duplicate company reference or conflicting retry")})
    ResponseEntity<CreatedEmployeeResponseDto> create(@PathVariable UUID companyId,
            @Valid @RequestBody CreateEmployeeRequestDto request, HttpServletResponse response) {
        try {
            var created = create.create(companyId, EmployeeWebMapper.facts(request),
                    request.idempotencyKey(), response.getHeader("X-Request-ID"));
            return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                    .body(EmployeeWebMapper.response(created));
        } catch (AccessDeniedException denied) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (EmployeeCreationConflict conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (DataIntegrityViolationException violation) {
            String message = violation.getMostSpecificCause().getMessage();
            if (message != null && message.contains("employment_employee_reference_unique")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
            throw violation;
        }
    }
}
