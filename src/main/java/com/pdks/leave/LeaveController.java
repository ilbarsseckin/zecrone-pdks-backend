package com.pdks.leave;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Leave>> list() {
        return ResponseEntity.ok(leaveService.findAll());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Leave>> pending() {
        return ResponseEntity.ok(leaveService.findPending());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<Leave>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(leaveService.findByEmployee(employeeId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<Leave> create(@Valid @RequestBody LeaveDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(leaveService.create(dto));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Leave> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal String reviewerId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(
            leaveService.approve(id, UUID.fromString(reviewerId), note));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Leave> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal String reviewerId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(
            leaveService.reject(id, UUID.fromString(reviewerId), note));
    }

    // ── Bakiye endpoint'leri ─────────────────────────────────────────────────

    /** Çalışanın yıllık izin bakiyesi */
    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<LeaveBalance> balance(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return ResponseEntity.ok(leaveService.getOrCreateBalance(employeeId, year));
    }

    /** Bakiye güncelle — sadece ADMIN */
    @PatchMapping("/balance/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setBalance(
            @PathVariable UUID employeeId,
            @RequestParam int year,
            @RequestParam int days) {
        leaveService.setEntitledDays(employeeId, year, days);
        return ResponseEntity.noContent().build();
    }
}
