package com.olarize.employment.application.port.in;

import java.time.LocalDate;

public record EmployeeDetailResult(EmployeeRecord employee, LocalDate asOf, String timeZone) { }
