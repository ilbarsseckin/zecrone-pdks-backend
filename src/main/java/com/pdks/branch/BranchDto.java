package com.pdks.branch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalTime;

@Data
public class BranchDto {

    @NotBlank(message = "Şube adı zorunlu")
    private String name;

    private String city;
    private String address;
    private String phone;

    private LocalTime workStart = LocalTime.of(8, 0);
    private LocalTime workEnd   = LocalTime.of(17, 0);
    private Integer lateTolerance = 5;
    private String timezone = "Europe/Istanbul";
}
