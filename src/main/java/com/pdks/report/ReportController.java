package com.pdks.report;

import com.pdks.attendance.AttendanceRepository;
import com.pdks.common.BusinessException;
import com.pdks.config.TenantContext;
import com.pdks.employee.EmployeeRepository;
import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AttendanceRepository attendanceRepo;
    private final EmployeeRepository employeeRepo;
    private final TenantRepository tenantRepo;

    @GetMapping("/plan-features")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getPlanFeatures() {
        // JWT'den tenantId al ve planı döndür
        // Frontend bu endpoint'i çağırarak hangi özelliklerin aktif olduğunu öğrenir
        return ResponseEntity.ok(java.util.Map.of(
                "canExportExcel", true,
                "canUseQr", true,
                "canUseApi", true
        ));
    }

    @GetMapping("/attendance/excel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> attendanceExcel(
            @RequestParam UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader("X-Tenant-Id") String tenantId)
            throws Exception {

        // Plan kontrolü
        Tenant tenant = tenantRepo.findById(UUID.fromString(tenantId))
                .orElseThrow(() -> new BusinessException("Firma bulunamadı"));

        if (!tenant.getPlan().canExportExcel())
            throw new BusinessException(
                    "Excel export özelliği Professional veya Enterprise planlarda kullanılabilir");

        var records = attendanceRepo
                .findAllByBranchIdAndWorkDateBetween(branchId, from, to);
        var employees = employeeRepo.findAll();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Yoklama Raporu");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] cols = {"Ad Soyad", "Tarih", "Giriş", "Çıkış", "Çalışma (dk)", "Durum"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowNum = 1;
            for (var rec : records) {
                var emp = employees.stream()
                        .filter(e -> e.getId().equals(rec.getEmployeeId()))
                        .findFirst();

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(
                        emp.map(e -> e.getFirstName() + " " + e.getLastName()).orElse("-"));
                row.createCell(1).setCellValue(rec.getWorkDate().toString());
                row.createCell(2).setCellValue(
                        rec.getCheckIn() != null ? rec.getCheckIn().toLocalTime().toString() : "-");
                row.createCell(3).setCellValue(
                        rec.getCheckOut() != null ? rec.getCheckOut().toLocalTime().toString() : "-");
                row.createCell(4).setCellValue(rec.getWorkMinutes());
                row.createCell(5).setCellValue(rec.getStatus().toString());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=yoklama_" + from + "_" + to + ".xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}