package com.olarize.employment.infrastructure.in.web.controller;

import com.olarize.employment.infrastructure.in.web.dto.request.ExpiringContractsRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.ExpiringContractsResponseDto;
import com.olarize.employment.infrastructure.in.web.mapper.ExpiringContractsWebMapper;

import com.olarize.employment.application.port.in.ListExpiringContractsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/api/v1/companies/{companyId}/contracts/expiring")
class ExpiringContractsController {
    private static final Set<String> ALLOWED_QUERY_PARAMETERS = Set.of("fromDate", "toDate", "page", "pageSize");
    private final ListExpiringContractsUseCase listExpiringContracts;

    ExpiringContractsController(ListExpiringContractsUseCase listExpiringContracts) {
        this.listExpiringContracts = listExpiringContracts;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List contracts with recorded expiry in a date range",
            description = "Returns current contract versions whose HR-recorded end dates fall inside the inclusive date range. "
                    + "Results are company-scoped, ordered by end date and contract ID, and paginated (default 25, maximum 100). "
                    + "The separate missing-date count covers current company contracts without a recorded end date; it is not an expiry estimate. "
                    + "Requires a Keycloak access token with profile.read and current contract.read membership. Date interpretation uses Africa/Casablanca. "
                    + "This read-only endpoint sends no notices and makes no renewal or legal-validity decision.",
            responses = {
                @ApiResponse(responseCode = "200", description = "The requested page of current contract facts and missing-date coverage"),
                @ApiResponse(responseCode = "400", description = "Missing, invalid, duplicated or unknown filters; reversed or overlong date range; invalid page size"),
                @ApiResponse(responseCode = "401", description = "A valid Keycloak access token is required"),
                @ApiResponse(responseCode = "403", description = "Current contract.read membership in this company is required")})
    ResponseEntity<ExpiringContractsResponseDto> list(
            @Parameter(description = "Company whose current contracts are listed", example = "c0000000-0000-4000-8000-000000000001")
            @PathVariable UUID companyId,
            @Parameter(description = "First included end date, inclusive; use YYYY-MM-DD", example = "2027-01-01")
            @RequestParam String fromDate,
            @Parameter(description = "Last included end date, inclusive; at most 366 days after fromDate", example = "2027-12-31")
            @RequestParam String toDate,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100; defaults to 25", example = "25")
            @RequestParam(defaultValue = "25") int pageSize,
            HttpServletRequest request) {
        validateQuery(request);
        var input = new ExpiringContractsRequestDto(parseDate(fromDate), parseDate(toDate), page, pageSize);
        try {
            var result = listExpiringContracts.list(companyId, input.fromDate(), input.toDate(),
                    input.page(), input.pageSize());
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(ExpiringContractsWebMapper.response(result));
        } catch (AccessDeniedException denied) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static void validateQuery(HttpServletRequest request) {
        if (!ALLOWED_QUERY_PARAMETERS.containsAll(request.getParameterMap().keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        for (String name : ALLOWED_QUERY_PARAMETERS) {
            String[] values = request.getParameterValues(name);
            if (values != null && values.length != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
