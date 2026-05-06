package com.pdks.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class EmployeeDto {

    @NotNull(message = "Şube seçilmeli")
    private UUID branchId;

    @NotBlank(message = "Ad zorunlu")
    private String firstName;

    @NotBlank(message = "Soyad zorunlu")
    private String lastName;

    private String email;
    private String phone;
    private String department;
    private String position;
    private LocalDate startDate;
}
