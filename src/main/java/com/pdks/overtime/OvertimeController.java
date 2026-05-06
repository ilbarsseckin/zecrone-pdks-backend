package com.pdks.overtime;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Overtime>> pending() {
        return ResponseEntity.ok(overtimeService.findPending());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<Overtime>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(overtimeService.findByEmployee(employeeId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Overtime>> list(
            @RequestParam UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(overtimeService.findByBranchAndRange(branchId, from, to));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Overtime> create(@Valid @RequestBody OvertimeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(overtimeService.create(dto));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Overtime> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(overtimeService.approve(id, UUID.fromString(userId)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Overtime> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(overtimeService.reject(id));
    }
}