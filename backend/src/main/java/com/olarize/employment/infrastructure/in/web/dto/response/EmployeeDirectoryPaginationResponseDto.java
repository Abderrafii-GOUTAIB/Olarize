package com.olarize.employment.infrastructure.in.web.dto.response;

public record EmployeeDirectoryPaginationResponseDto(int page, int pageSize, long totalItems, int totalPages) { }
