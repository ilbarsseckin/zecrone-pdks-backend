package com.pdks.auth;

import com.pdks.common.BusinessException;
import com.pdks.config.TenantContext;
import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
import com.pdks.user.User;
import com.pdks.user.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

// ─── DTO'lar ─────────────────────────────────────────────

@Data
class LoginDto {
    @NotBlank @Email
    public String tenantEmail;

    @NotBlank @Email
    public String userEmail;

    @NotBlank
    public String password;
}

@Data
class RegisterUserDto {
    @NotBlank @Email
    public String email;

    @NotBlank @Size(min = 8)
    public String password;

    public User.UserRole role = User.UserRole.STAFF;
    public UUID branchId;
    public UUID employeeId;
}

@Data
class AuthResponseDto {
    public String token;
    public String role;
    public long expiresIn = 86400;
    public String firstName;
    public String lastName;
    public UUID branchId;
    public UUID userId;
}

// ─── Service ─────────────────────────────────────────────

@Service
@RequiredArgsConstructor
@Slf4j
class AuthService {

    private final TenantRepository tenantRepo;
    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponseDto login(LoginDto dto) {

        // Super Admin kontrolü — tenant gerekmez
        if ("superadmin@zecrone.com".equals(dto.userEmail)) {
            TenantContext.setTenant("public");
            User user = userRepo.findByEmail(dto.userEmail)
                    .orElseThrow(() -> new BusinessException("Email veya şifre hatalı"));

            if (!passwordEncoder.matches(dto.password, user.getPasswordHash()))
                throw new BusinessException("Email veya şifre hatalı");

            user.setLastLogin(LocalDateTime.now());
            userRepo.save(user);

            String token = jwtService.generateToken(
                    user.getId(),
                    UUID.fromString("00000000-0000-0000-0000-000000000000"),
                    "public",
                    user.getRole().name(),
                    null,
                    "ENTERPRISE"
            );

            AuthResponseDto res = new AuthResponseDto();
            res.token  = token;
            res.role   = user.getRole().name();
            res.userId = user.getId();
            return res;
        }

        // 1. Firmayı bul
        Tenant tenant = tenantRepo
                .findByContactEmail(dto.tenantEmail)
                .orElseThrow(() -> new BusinessException("Firma bulunamadı"));

        if (!tenant.getIsActive())
            throw new BusinessException("Firma hesabı askıya alınmış");

        // 2. Tenant schema'sına geç
        TenantContext.setTenant(tenant.getSchemaName());

        // 3. Kullanıcıyı bul
        User user = userRepo.findByEmail(dto.userEmail)
                .orElseThrow(() -> new BusinessException("Email veya şifre hatalı"));

        // 4. Şifre kontrol
        if (!passwordEncoder.matches(dto.password, user.getPasswordHash()))
            throw new BusinessException("Email veya şifre hatalı");

        if (!user.getIsActive())
            throw new BusinessException("Hesabınız askıya alınmış");

        // 5. Son giriş güncelle
        user.setLastLogin(LocalDateTime.now());
        userRepo.save(user);

        // 6. Token üret
        String token = jwtService.generateToken(
                user.getId(),
                tenant.getId(),
                tenant.getSchemaName(),
                user.getRole().name(),
                user.getBranchId(),
                tenant.getPlan().name()
        );

        log.info("Giriş: {} / {}", tenant.getCompanyName(), user.getEmail());

        AuthResponseDto res = new AuthResponseDto();
        res.token    = token;
        res.role     = user.getRole().name();
        res.userId   = user.getId();
        res.branchId = user.getBranchId();
        return res;
    }

    public User register(String schemaName, RegisterUserDto dto) {
        TenantContext.setTenant(schemaName);

        if (userRepo.existsByEmail(dto.email))
            throw new BusinessException("Bu email zaten kayıtlı: " + dto.email);

        User user = new User();
        user.setEmail(dto.email);
        user.setPasswordHash(passwordEncoder.encode(dto.password));
        user.setRole(dto.role);
        user.setBranchId(dto.branchId);
        user.setEmployeeId(dto.employeeId);

        return userRepo.save(user);
    }
}

// ─── Controller ──────────────────────────────────────────

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final TenantRepository tenantRepo;


    @GetMapping("/hash")
    public String hash(@RequestParam String pw) {
        return passwordEncoder.encode(pw);
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @RequestBody LoginDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    // POST /api/auth/register
    // Tenant schema adını header'dan al
    @PostMapping("/register")
    public ResponseEntity<User> register(
            @RequestHeader("X-Schema-Name") String schemaName,
            @RequestBody RegisterUserDto dto) {
        return ResponseEntity.ok(authService.register(schemaName, dto));
    }

}