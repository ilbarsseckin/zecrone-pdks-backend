package com.pdks.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceRepository attendanceRepo;
    private final AttendanceService attendanceService;

    // POST /api/attendance/check-in
    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<AttendanceRecord> checkIn(
            @RequestParam UUID employeeId) {
        return ResponseEntity.ok(attendanceService.checkIn(employeeId));
    }

    // POST /api/attendance/check-out
    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<AttendanceRecord> checkOut(
            @RequestParam UUID employeeId) {
        return ResponseEntity.ok(attendanceService.checkOut(employeeId));
    }

    // GET /api/attendance/today?employeeId=...
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<AttendanceRecord> today(
            @RequestParam UUID employeeId) {
        return ResponseEntity.ok(attendanceService.getTodayRecord(employeeId));
    }

    // GET /api/attendance/daily?branchId=...&date=2025-01-15
    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<AttendanceRecord>> daily(
            @RequestParam UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(
            attendanceService.getDailyReport(branchId, date));
    }

    // GET /api/attendance/monthly?employeeId=...&year=2025&month=1
    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<AttendanceRecord>> monthly(
            @RequestParam UUID employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(
            attendanceService.getMonthlyReport(employeeId, year, month));
    }

    @PatchMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<AttendanceRecord> editRecord(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String userId) {

        AttendanceRecord rec = attendanceRepo.findById(id)
                .orElseThrow(() -> new com.pdks.common.BusinessException("Kayıt bulunamadı"));

        if (body.containsKey("checkIn") && !body.get("checkIn").isBlank())
            rec.setCheckIn(java.time.LocalDateTime.parse(
                    rec.getWorkDate() + "T" + body.get("checkIn") + ":00"));

        if (body.containsKey("checkOut") && !body.get("checkOut").isBlank())
            rec.setCheckOut(java.time.LocalDateTime.parse(
                    rec.getWorkDate() + "T" + body.get("checkOut") + ":00"));

        if (rec.getCheckIn() != null && rec.getCheckOut() != null) {
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    rec.getCheckIn(), rec.getCheckOut());
            rec.setWorkMinutes((int) minutes);
        }

        if (body.containsKey("status"))
            rec.setStatus(AttendanceRecord.Status.valueOf(body.get("status")));

        rec.setManuallyEdited(true);
        rec.setEditedBy(UUID.fromString(userId));
        rec.setEditedAt(java.time.LocalDateTime.now());
        if (body.containsKey("editNote")) rec.setEditNote(body.get("editNote"));

        return ResponseEntity.ok(attendanceRepo.save(rec));
    }
}
