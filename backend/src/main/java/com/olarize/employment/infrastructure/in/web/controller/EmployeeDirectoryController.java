package com.olarize.employment.infrastructure.in.web.controller;

import com.olarize.employment.application.exception.EmployeeNotFound;
import com.olarize.employment.application.port.in.EmployeeDirectoryUseCase;
import com.olarize.employment.domain.model.EmployeeDirectoryStatus;
import com.olarize.employment.infrastructure.in.web.dto.request.EmployeeDirectoryRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.EmployeeDetailResponseDto;
import com.olarize.employment.infrastructure.in.web.dto.response.EmployeeDirectoryResponseDto;
import com.olarize.employment.infrastructure.in.web.mapper.EmployeeDirectoryWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/employees")
@SecurityRequirement(name = "keycloak", scopes = "profile.read")
class EmployeeDirectoryController {
    private final EmployeeDirectoryUseCase directory;

    EmployeeDirectoryController(EmployeeDirectoryUseCase directory) { this.directory = directory; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search company employees",
            description = "Searches literal text in employee names or references, preserving separate records for duplicate names. "
                    + "Requires current employee.read membership and profile.read scope. Defaults to CURRENT; select UPCOMING, ENDED or ALL explicitly. "
                    + "Status uses employment episode dates inclusive of the last employed day, independently of contract expiry. "
                    + "asOf defaults to today in Africa/Casablanca and is returned explicitly. Results are ordered by reference then employee ID; "
                    + "page defaults to 0, pageSize to 25 (maximum 100). An authorized no-match result has empty items; database errors remain errors.",
            responses = {@ApiResponse(responseCode = "200", description = "Company-scoped employee page"),
                @ApiResponse(responseCode = "400", description = "Invalid filter, date or pagination"),
                @ApiResponse(responseCode = "401", description = "Valid Keycloak access token required"),
                @ApiResponse(responseCode = "403", description = "Current employee.read permission required"),
                @ApiResponse(responseCode = "500", description = "Directory unavailable; use requestId to investigate and retry")})
    ResponseEntity<EmployeeDirectoryResponseDto> list(@PathVariable UUID companyId,
            @Parameter(description = "Literal partial name or reference; maximum 200 characters", example = "Ahmed")
            @RequestParam(required = false) String search,
            @Parameter(description = "CURRENT, UPCOMING, ENDED or ALL", example = "CURRENT")
            @RequestParam(defaultValue = "CURRENT") EmployeeDirectoryStatus status,
            @Parameter(description = "ISO date used to derive status; defaults to Casablanca today", example = "2026-09-30")
            @RequestParam(required = false) String asOf,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int pageSize,
            HttpServletRequest request) {
        validateQuery(request, Set.of("search", "status", "asOf", "page", "pageSize"));
        try {
            var input = new EmployeeDirectoryRequestDto(search, status, date(asOf), page, pageSize);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(EmployeeDirectoryWebMapper.response(
                    directory.list(companyId, input.asOf(), EmployeeDirectoryWebMapper.filter(input))));
        } catch (AccessDeniedException denied) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/{employeeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Inspect an employee record",
            description = "Returns an identified company's employee record, work contact facts and the episode selected as of the supplied date. "
                    + "Requires current employee.read permission and profile.read scope. Missing or foreign-company IDs return the same scoped 404. "
                    + "asOf defaults to Africa/Casablanca today. Does not expose private contact, national identity, salary or document fields.",
            responses = {@ApiResponse(responseCode = "200", description = "Employee work facts and derived employment status"),
                @ApiResponse(responseCode = "400", description = "Invalid employee ID or date"),
                @ApiResponse(responseCode = "401", description = "Valid Keycloak access token required"),
                @ApiResponse(responseCode = "403", description = "Current employee.read permission required"),
                @ApiResponse(responseCode = "404", description = "Employee unavailable in selected company"),
                @ApiResponse(responseCode = "500", description = "Employee detail unavailable; retry using requestId")})
    ResponseEntity<EmployeeDetailResponseDto> detail(@PathVariable UUID companyId,
            @Parameter(description = "Employee ID copied from the directory", example = "c0000000-0000-4000-8000-000000000080")
            @PathVariable UUID employeeId, @RequestParam(required = false) String asOf, HttpServletRequest request) {
        validateQuery(request, Set.of("asOf"));
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(EmployeeDirectoryWebMapper.response(
                    directory.detail(companyId, employeeId, date(asOf))));
        } catch (AccessDeniedException denied) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (EmployeeNotFound missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static LocalDate date(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException invalid) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    }

    private static void validateQuery(HttpServletRequest request, Set<String> allowed) {
        if (!allowed.containsAll(request.getParameterMap().keySet())
                || request.getParameterMap().values().stream().anyMatch(values -> values.length != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
