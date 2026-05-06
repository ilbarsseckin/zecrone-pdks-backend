package com.pdks.report;

import com.pdks.attendance.AttendanceRecord;
import com.pdks.attendance.AttendanceRepository;
import com.pdks.employee.Employee;
import com.pdks.employee.EmployeeRepository;
import com.pdks.leave.Leave;
import com.pdks.leave.LeaveRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AttendanceRepository attendanceRepo;
    private final EmployeeRepository   employeeRepo;
    private final LeaveRepository      leaveRepo;

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private static final String CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void writeHeader(Sheet sheet, CellStyle style, String... cols) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, 5500);
        }
    }

    private String statusLabel(AttendanceRecord.Status s) {
        return switch (s) {
            case PRESENT  -> "Geldi";
            case LATE     -> "Geç Geldi";
            case ABSENT   -> "Gelmedi";
            case HALF_DAY -> "Yarım Gün";
            case ON_LEAVE -> "İzinli";
        };
    }

    private String leaveTypeLabel(Leave.LeaveType t) {
        return switch (t) {
            case ANNUAL      -> "Yıllık";
            case SICK        -> "Hastalık";
            case MATERNITY   -> "Doğum";
            case PATERNITY   -> "Babalık";
            case MARRIAGE    -> "Evlilik";
            case BEREAVEMENT -> "Vefat";
            case UNPAID      -> "Ücretsiz";
            case OTHER       -> "Diğer";
        };
    }

    private String leaveStatusLabel(Leave.LeaveStatus s) {
        return switch (s) {
            case PENDING  -> "Bekliyor";
            case APPROVED -> "Onaylandı";
            case REJECTED -> "Reddedildi";
        };
    }

    // ── 1. Yoklama Raporu ─────────────────────────────────────────────────────

    @GetMapping("/attendance/excel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> attendanceExcel(
            @RequestParam UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws Exception {

        var records   = attendanceRepo.findAllByBranchIdAndWorkDateBetween(branchId, from, to);
        var employees = employeeRepo.findAll();
        Map<UUID, Employee> empMap = employees.stream()
            .collect(Collectors.toMap(Employee::getId, e -> e));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Yoklama Raporu");
            CellStyle hs = headerStyle(wb);
            writeHeader(sheet, hs,
                "Ad Soyad", "Departman", "Tarih",
                "Giriş", "Çıkış", "Çalışma (saat)", "Durum");

            int rowNum = 1;
            for (var rec : records) {
                Employee emp = empMap.get(rec.getEmployeeId());
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(emp != null ? emp.getFullName() : "-");
                row.createCell(1).setCellValue(emp != null && emp.getDepartment() != null
                    ? emp.getDepartment() : "-");
                row.createCell(2).setCellValue(rec.getWorkDate().toString());
                row.createCell(3).setCellValue(rec.getCheckIn() != null
                    ? rec.getCheckIn().toLocalTime().withSecond(0).toString() : "-");
                row.createCell(4).setCellValue(rec.getCheckOut() != null
                    ? rec.getCheckOut().toLocalTime().withSecond(0).toString() : "-");
                row.createCell(5).setCellValue(
                    rec.getWorkMinutes() != null
                        ? String.format("%.1f", rec.getWorkMinutes() / 60.0) : "0");
                row.createCell(6).setCellValue(statusLabel(rec.getStatus()));
            }

            return buildResponse(wb,
                "yoklama_" + from + "_" + to + ".xlsx");
        }
    }

    // ── 2. İzin Raporu ────────────────────────────────────────────────────────

    @GetMapping("/leaves/excel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> leavesExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws Exception {

        List<Leave> leaves = leaveRepo.findAll();

        // Tarih filtresi
        if (from != null) {
            leaves = leaves.stream()
                .filter(l -> !l.getStartDate().isBefore(from))
                .collect(Collectors.toList());
        }
        if (to != null) {
            leaves = leaves.stream()
                .filter(l -> !l.getEndDate().isAfter(to))
                .collect(Collectors.toList());
        }

        var employees = employeeRepo.findAll();
        Map<UUID, Employee> empMap = employees.stream()
            .collect(Collectors.toMap(Employee::getId, e -> e));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("İzin Raporu");
            CellStyle hs = headerStyle(wb);
            writeHeader(sheet, hs,
                "Ad Soyad", "Departman", "İzin Türü",
                "Başlangıç", "Bitiş", "Gün", "Durum", "Kaynak");

            int rowNum = 1;
            for (var leave : leaves) {
                Employee emp = empMap.get(leave.getEmployeeId());
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(emp != null ? emp.getFullName() : "-");
                row.createCell(1).setCellValue(emp != null && emp.getDepartment() != null
                    ? emp.getDepartment() : "-");
                row.createCell(2).setCellValue(leaveTypeLabel(leave.getType()));
                row.createCell(3).setCellValue(leave.getStartDate().toString());
                row.createCell(4).setCellValue(leave.getEndDate().toString());
                row.createCell(5).setCellValue(leave.getTotalDays() != null
                    ? leave.getTotalDays() : 0);
                row.createCell(6).setCellValue(leaveStatusLabel(leave.getStatus()));
                row.createCell(7).setCellValue(
                    "MOBILE".equals(leave.getRequestedBy()) ? "Mobil" : "Web");
            }

            return buildResponse(wb, "izinler.xlsx");
        }
    }

    // ── 3. Aylık Devam Özet Raporu ────────────────────────────────────────────

    @GetMapping("/monthly-summary/excel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> monthlySummaryExcel(
            @RequestParam UUID branchId,
            @RequestParam int year,
            @RequestParam int month) throws Exception {

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        int workDays   = countWorkDays(from, to);

        var records   = attendanceRepo.findAllByBranchIdAndWorkDateBetween(branchId, from, to);
        var employees = employeeRepo.findAllByBranchId(branchId);

        // Personel başına istatistik hesapla
        Map<UUID, long[]> stats = new HashMap<>(); // [present, late, absent, totalMin]
        for (Employee emp : employees) {
            stats.put(emp.getId(), new long[]{0, 0, 0, 0});
        }
        for (var rec : records) {
            long[] s = stats.getOrDefault(rec.getEmployeeId(), new long[4]);
            switch (rec.getStatus()) {
                case PRESENT  -> s[0]++;
                case LATE     -> { s[1]++; s[0]++; }
                case ABSENT   -> s[2]++;
                default       -> {}
            }
            if (rec.getWorkMinutes() != null) s[3] += rec.getWorkMinutes();
            stats.put(rec.getEmployeeId(), s);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(from.getMonth().toString() + " " + year);
            CellStyle hs = headerStyle(wb);
            writeHeader(sheet, hs,
                "Ad Soyad", "Departman",
                "Geldi", "Geç", "Gelmedi", "Devam %",
                "Toplam Çalışma (saat)");

            int rowNum = 1;
            for (Employee emp : employees) {
                long[] s = stats.getOrDefault(emp.getId(), new long[4]);
                double pct = workDays > 0 ? (s[0] * 100.0 / workDays) : 0;

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(emp.getFullName());
                row.createCell(1).setCellValue(
                    emp.getDepartment() != null ? emp.getDepartment() : "-");
                row.createCell(2).setCellValue(s[0]);
                row.createCell(3).setCellValue(s[1]);
                row.createCell(4).setCellValue(workDays - s[0]);
                row.createCell(5).setCellValue(String.format("%.1f%%", pct));
                row.createCell(6).setCellValue(String.format("%.1f", s[3] / 60.0));
            }

            return buildResponse(wb,
                "ozet_" + year + "_" + String.format("%02d", month) + ".xlsx");
        }
    }

    // ── 4. Geç Kalma İstatistikleri ───────────────────────────────────────────

    @GetMapping("/late-stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> lateStats(
            @RequestParam UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        var records   = attendanceRepo.findAllByBranchIdAndWorkDateBetween(branchId, from, to);
        var employees = employeeRepo.findAll();
        Map<UUID, Employee> empMap = employees.stream()
            .collect(Collectors.toMap(Employee::getId, e -> e));

        // Geç kalan personeli grupla
        Map<UUID, Long> lateCount = records.stream()
            .filter(r -> r.getStatus() == AttendanceRecord.Status.LATE)
            .collect(Collectors.groupingBy(
                AttendanceRecord::getEmployeeId, Collectors.counting()));

        List<Map<String, Object>> result = lateCount.entrySet().stream()
            .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
            .map(e -> {
                Employee emp = empMap.get(e.getKey());
                return Map.<String, Object>of(
                    "employeeId",   e.getKey(),
                    "fullName",     emp != null ? emp.getFullName() : "-",
                    "department",   emp != null && emp.getDepartment() != null
                        ? emp.getDepartment() : "-",
                    "lateCount",    e.getValue()
                );
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> buildResponse(Workbook wb, String filename)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType(CONTENT_TYPE))
            .body(out.toByteArray());
    }

    /** Cumartesi ve Pazar hariç iş günü sayısı */
    private int countWorkDays(LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate d = from;
        while (!d.isAfter(to)) {
            int dow = d.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
            if (dow < 6) count++;
            d = d.plusDays(1);
        }
        return count;
    }
}
