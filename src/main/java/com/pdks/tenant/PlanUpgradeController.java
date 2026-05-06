package com.pdks.tenant;

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
@RequestMapping("/api/plan-upgrades")
@RequiredArgsConstructor
public class PlanUpgradeController {

    private final PlanUpgradeService upgradeService;

    // ── ADMIN endpoint'leri ──────────────────────────────────────────────────

    /** Plan yükseltme talebi oluştur */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanUpgradeRequest> request(
            @Valid @RequestBody PlanUpgradeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(upgradeService.requestUpgrade(dto));
    }

    /** Kendi talep geçmişi */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<PlanUpgradeRequest>> myRequests() {
        return ResponseEntity.ok(upgradeService.findMyRequests());
    }

    // ── SUPER_ADMIN endpoint'leri ────────────────────────────────────────────

    /** Bekleyen tüm talepler */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<PlanUpgradeRequest>> pending() {
        return ResponseEntity.ok(upgradeService.findAllPending());
    }

    /** Talebi onayla */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PlanUpgradeRequest> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal String reviewerId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(
            upgradeService.approve(id, UUID.fromString(reviewerId), note));
    }

    /** Talebi reddet */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PlanUpgradeRequest> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal String reviewerId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(
            upgradeService.reject(id, UUID.fromString(reviewerId), note));
    }
}
