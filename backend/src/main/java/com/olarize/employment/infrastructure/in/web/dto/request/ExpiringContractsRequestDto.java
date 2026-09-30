package com.olarize.employment.infrastructure.in.web.dto.request;

import java.time.LocalDate;

public record ExpiringContractsRequestDto(LocalDate fromDate, LocalDate toDate, int page, int pageSize) { }
