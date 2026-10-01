package com.olarize.employment.infrastructure.in.web.controller;

import com.olarize.employment.application.port.in.CorrectEmployeeUseCase;
import com.olarize.employment.application.exception.EmployeeCorrectionConflict;
import com.olarize.employment.application.exception.EmployeeNotFound;
import com.olarize.employment.infrastructure.in.web.dto.request.CorrectEmployeeRequestDto;
import com.olarize.employment.infrastructure.in.web.dto.response.CorrectedEmployeeResponseDto;
import com.olarize.employment.infrastructure.in.web.mapper.EmployeeCorrectionWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/employees/{employeeId}/episodes/{episodeId}/corrections")
class EmployeeCorrectionController {
    private final CorrectEmployeeUseCase corrections;
    EmployeeCorrectionController(CorrectEmployeeUseCase corrections) { this.corrections = corrections; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Correct employee work facts with a reason and current versions",
            description = "Requires Keycloak profile.read and current company employee.write. HR explicitly confirms the full minimum work-field replacement for stable employee and episode IDs. Immutable source versions and restricted audit are committed atomically. Dated job facts are selected by directory asOf; name/contact facts are current administrative corrections. Start-date corrections preserve contract dates and downstream snapshots. Same-key retries return the original outcome; stale or duplicate writes conflict. Department/site are labels, never linked references. No account, salary, private identity or last-day changes.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Correction committed with source versions, or original retry outcome",
                        content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                                implementation = CorrectedEmployeeResponseDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid, unknown or unconfirmed input"),
                @ApiResponse(responseCode = "401", description = "Authentication required"),
                @ApiResponse(responseCode = "403", description = "Current company employee.write permission required"),
                @ApiResponse(responseCode = "404", description = "Employee or episode unavailable in selected company"),
                @ApiResponse(responseCode = "409", description = "Stale versions, duplicate reference, conflicting retry or dependent factual intervals")})
    ResponseEntity<?> correct(@PathVariable UUID companyId,
            @PathVariable UUID employeeId, @PathVariable UUID episodeId,
            @Valid @RequestBody CorrectEmployeeRequestDto request, HttpServletResponse response) {
        try {
            var result = corrections.correct(companyId, employeeId, episodeId, request.expectedVersion(),
                    request.expectedEpisodeVersion(), EmployeeCorrectionWebMapper.correction(request),
                    request.idempotencyKey(), response.getHeader("X-Request-ID"));
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(EmployeeCorrectionWebMapper.response(result));
        } catch (org.springframework.security.access.AccessDeniedException denied) { throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
        catch (EmployeeNotFound missing) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        catch (EmployeeCorrectionConflict conflict) {
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The correction conflicts with current facts.");
            problem.setType(java.net.URI.create("urn:olarize:problem:http-409"));
            problem.setProperty("code", "HTTP_409");
            problem.setProperty("requestId", response.getHeader("X-Request-ID"));
            problem.setProperty("dependentFacts", conflict.dependentFacts());
            return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(CacheControl.noStore()).body(problem);
        } catch (ObjectOptimisticLockingFailureException stale) { throw new ResponseStatusException(HttpStatus.CONFLICT); }
        catch (DataIntegrityViolationException failure) {
            if (isCorrectionConstraint(failure)) throw new ResponseStatusException(HttpStatus.CONFLICT);
            throw failure;
        } catch (IllegalArgumentException invalid) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    }

    private static boolean isCorrectionConstraint(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                // Only named uniqueness failures are expected conflicts. Messages are never returned/logged.
                String message = sql.getMessage();
                return message != null && (message.contains("employment_employee_reference_unique")
                        || message.contains("employee_version_unique") || message.contains("employee_correction_key_unique"));
            }
        }
        return false;
    }
}
