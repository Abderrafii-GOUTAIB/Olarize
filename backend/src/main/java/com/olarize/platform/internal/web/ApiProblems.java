package com.olarize.platform.internal.web;

import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ApiProblems {
    private ApiProblems() { }

    public static ProblemDetail create(HttpStatus status) {
        String detail = switch (status) {
            case BAD_REQUEST -> "The request is invalid.";
            case UNAUTHORIZED -> "Authentication is required.";
            case FORBIDDEN -> "This operation is not permitted.";
            case NOT_FOUND -> "The requested resource was not found.";
            case METHOD_NOT_ALLOWED -> "The HTTP method is not supported.";
            case UNSUPPORTED_MEDIA_TYPE -> "The content type is not supported.";
            case CONFLICT -> "The request conflicts with the current state.";
            default -> status.is5xxServerError() ? "An unexpected error occurred." : "The request could not be processed.";
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:olarize:problem:http-" + status.value()));
        problem.setProperty("code", "HTTP_" + status.value());
        problem.setProperty("requestId", MDC.get(RequestIdFilter.MDC_KEY));
        return problem;
    }
}
