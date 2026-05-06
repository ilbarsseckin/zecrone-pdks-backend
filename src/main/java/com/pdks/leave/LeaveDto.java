package com.pdks.leave;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class LeaveDto {

    @NotNull
    private UUID employeeId;

    @NotNull
    private Leave.LeaveType type;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private String description;

    /** WEB veya MOBILE — otomatik set edilir, frontend göndermez */
    private String requestedBy = "WEB";
}
