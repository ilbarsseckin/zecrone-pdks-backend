package com.pdks.employee;

import com.pdks.mobile.MobileAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService   employeeService;
    private final MobileAuthService mobileAuthService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Employee>> list(
            @RequestParam(required = false) UUID branchId) {
        if (branchId != null)
            return ResponseEntity.ok(employeeService.findByBranch(branchId));
        return ResponseEntity.ok(employeeService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Employee> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Employee> create(@Valid @RequestBody EmployeeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(employeeService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Employee> update(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDto dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setStatus(
            @PathVariable UUID id,
            @RequestParam Employee.Status status) {
        employeeService.setStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    /**
     * Yönetici çalışan için mobil şifre belirler.
     * Çalışan ilk defa mobil uygulamaya giriş yaparken bu şifreyi kullanır.
     */
    @PostMapping("/{id}/set-mobile-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> setMobilePassword(
            @PathVariable UUID id,
            @RequestParam String password) {
        mobileAuthService.setInitialPassword(id, password);
        return ResponseEntity.ok(Map.of(
            "message", "Mobil şifre başarıyla belirlendi."
        ));
    }
}
