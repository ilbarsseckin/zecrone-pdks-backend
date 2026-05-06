package com.pdks.shift;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class ShiftDto {

    @NotNull
    private UUID branchId;

    @NotNull
    private UUID employeeId;

    private String name;

    @NotNull
    private LocalDate shiftDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private Shift.ShiftType type = Shift.ShiftType.MORNING;
}