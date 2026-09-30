package com.olarize.platform.internal.web;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problem = ApiProblems.create(status);
        if (ex instanceof MethodArgumentNotValidException validation) {
            problem.setProperty("errors", validation.getBindingResult().getFieldErrors().stream()
                    .map(error -> Map.of("field", error.getField(), "code",
                            error.getCode() == null ? "Invalid" : error.getCode())).toList());
        } else if (ex instanceof HttpMessageNotReadableException unreadable) {
            String field = "request";
            Throwable cause = unreadable;
            while (cause != null) {
                if (cause instanceof tools.jackson.databind.DatabindException mapping
                        && !mapping.getPath().isEmpty()) {
                    String path = mapping.getPathReference();
                    java.util.regex.Matcher property = java.util.regex.Pattern
                            .compile("\\[\\\"([A-Za-z][A-Za-z0-9]*)\\\"\\]$").matcher(path);
                    if (property.find()) field = property.group(1);
                    break;
                }
                cause = cause.getCause();
            }
            problem.setProperty("errors", java.util.List.of(Map.of("field", field, "code", "Invalid")));
        }
        return new ResponseEntity<>(problem, headers, statusCode);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        // Default logging avoids exception messages, which may contain HR data or SQL.
        LOG.error("Unhandled request failure type={}", exception.getClass().getName());
        LOG.debug("Local diagnostic exception detail", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiProblems.create(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
