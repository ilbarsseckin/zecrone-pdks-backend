package com.pdks.overtime;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class OvertimeDto {

    @NotNull
    private UUID employeeId;

    @NotNull
    private UUID branchId;

    @NotNull
    private LocalDate workDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private Overtime.OvertimeType type = Overtime.OvertimeType.WEEKDAY;
    private String description;
}