package com.olarize.identity.adapter.in.web;

import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.identity.application.port.in.CompanyContext;
import com.olarize.identity.domain.exception.CompanyAccessDenied;
import com.olarize.identity.domain.exception.CompanySelectionRequired;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class CompanyContextController {
    private final CompanyAccessUseCase access;
    CompanyContextController(CompanyAccessUseCase access) { this.access = access; }

    @GetMapping(value = "/api/v1/identity/memberships", produces = MediaType.APPLICATION_JSON_VALUE)
    List<CompanyContext> memberships(HttpServletRequest request) {
        validateQuery(request, false);
        return access.memberships();
    }

    @GetMapping(value = "/api/v1/identity/company-context", produces = MediaType.APPLICATION_JSON_VALUE)
    CompanyContext context(@Parameter(name = "companyId", description = "Optional company UUID from your active memberships. Omit it when you have one active membership; provide it when you have more than one.", schema = @Schema(type = "string", format = "uuid"))
                           @RequestParam(name = "companyId", required = false) String value,
                           HttpServletRequest request) {
        validateQuery(request, true);
        UUID requested = null;
        if (value != null) {
            try {
                requested = UUID.fromString(value);
                if (!requested.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException invalid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }
        try {
            return access.resolve(requested);
        } catch (CompanyAccessDenied denied) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (CompanySelectionRequired selection) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static void validateQuery(HttpServletRequest request, boolean allowsCompanyId) {
        var allowed = allowsCompanyId ? Set.of("companyId") : Set.<String>of();
        String[] companyIds = allowsCompanyId ? request.getParameterValues("companyId") : null;
        if (!allowed.containsAll(request.getParameterMap().keySet())
                || (companyIds != null && companyIds.length > 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
