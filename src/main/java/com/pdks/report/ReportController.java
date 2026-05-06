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
import com.pdks.branch.BranchRepository;
import com.pdks.employee.EmployeeRepository;
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AttendanceRepository attendanceRepo;
    private final EmployeeRepository employeeRepo;
    private final TenantRepository tenantRepo;
    private final com.pdks.branch.BranchRepository branchRepo;

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

    // GET /api/reports/employees/excel
    @GetMapping("/employees/excel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> employeesExcel(
            @RequestHeader("X-Tenant-Id") String tenantId) throws Exception {

        // Plan kontrolü
        Tenant tenant = tenantRepo.findById(UUID.fromString(tenantId))
                .orElseThrow(() -> new BusinessException("Firma bulunamadı"));

        if (!tenant.getPlan().canExportExcel())
            throw new BusinessException(
                    "Excel export Professional veya Enterprise planlarda kullanılabilir");

        var employees = employeeRepo.findAll();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Personel Listesi");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] cols = {"Ad", "Soyad", "Email", "Telefon", "Departman", "Pozisyon", "Durum", "İşe Başlama"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowNum = 1;
            for (var emp : employees) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(emp.getFirstName());
                row.createCell(1).setCellValue(emp.getLastName());
                row.createCell(2).setCellValue(emp.getEmail() != null ? emp.getEmail() : "");
                row.createCell(3).setCellValue(emp.getPhone() != null ? emp.getPhone() : "");
                row.createCell(4).setCellValue(emp.getDepartment() != null ? emp.getDepartment() : "");
                row.createCell(5).setCellValue(emp.getPosition() != null ? emp.getPosition() : "");
                row.createCell(6).setCellValue(emp.getStatus().name());
                row.createCell(7).setCellValue(emp.getStartDate() != null ? emp.getStartDate().toString() : "");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=personel.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    // POST /api/reports/employees/import
    @PostMapping("/employees/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importEmployees(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestHeader("X-Tenant-Id") String tenantId) throws Exception {

        Tenant tenant = tenantRepo.findById(UUID.fromString(tenantId))
                .orElseThrow(() -> new BusinessException("Firma bulunamadı"));

        if (!tenant.getPlan().canExportExcel())
            throw new BusinessException(
                    "Import özelliği Professional veya Enterprise planlarda kullanılabilir");

        // Şube listesi
        var branches = branchRepo.findAllByIsActiveTrue();
        if (branches.isEmpty())
            throw new BusinessException("Önce en az bir şube ekleyin");

        var defaultBranch = branches.get(0);
        int imported = 0;
        int skipped  = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String firstName = getCellValue(row, 0);
                String lastName  = getCellValue(row, 1);

                if (firstName.isBlank() || lastName.isBlank()) { skipped++; continue; }

                var emp = new com.pdks.employee.Employee();
                emp.setBranchId(defaultBranch.getId());
                emp.setFirstName(firstName);
                emp.setLastName(lastName);
                emp.setEmail(getCellValue(row, 2));
                emp.setPhone(getCellValue(row, 3));
                emp.setDepartment(getCellValue(row, 4));
                emp.setPosition(getCellValue(row, 5));
                employeeRepo.save(emp);
                imported++;
            }
        }

        return ResponseEntity.ok(java.util.Map.of(
                "imported", imported,
                "skipped",  skipped,
                "message",  imported + " personel aktarıldı, " + skipped + " satır atlandı"
        ));
    }

    private String getCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }

    // GET /api/reports/employees/template
    @GetMapping("/employees/template")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> employeesTemplate() throws Exception {

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Personel Şablonu");

            // Başlık stili
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Açıklama stili
            CellStyle descStyle = wb.createCellStyle();
            descStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            descStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font descFont = wb.createFont();
            descFont.setItalic(true);
            descFont.setColor(IndexedColors.DARK_RED.getIndex());
            descStyle.setFont(descFont);

            // Başlık satırı
            Row header = sheet.createRow(0);
            String[] cols = {"Ad *", "Soyad *", "Email", "Telefon", "Departman", "Pozisyon"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5500);
            }

            // Açıklama satırı
            Row desc = sheet.createRow(1);
            String[] descs = {
                    "Zorunlu - Personelin adı",
                    "Zorunlu - Personelin soyadı",
                    "Opsiyonel - ornek@firma.com",
                    "Opsiyonel - 0555 000 00 00",
                    "Opsiyonel - Yazılım, Muhasebe...",
                    "Opsiyonel - Manager, Uzman..."
            };
            for (int i = 0; i < descs.length; i++) {
                Cell cell = desc.createCell(i);
                cell.setCellValue(descs[i]);
                cell.setCellStyle(descStyle);
            }

            // Örnek satır
            Row example = sheet.createRow(2);
            String[] examples = {"Ali", "Yılmaz", "ali@firma.com", "0555 123 45 67", "Yazılım", "Developer"};
            for (int i = 0; i < examples.length; i++) {
                example.createCell(i).setCellValue(examples[i]);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=personel_sablonu.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}