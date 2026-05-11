package com.pdks.auth.verification;

import com.pdks.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    /**
     * GET /api/auth/verify-email?token=uuid
     * Frontend'den yönlendirilen link buraya gelir.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam UUID token) {
        try {
            verificationService.verifyEmail(token);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "E-posta doğrulandı. 14 günlük deneme süreniz başladı!"
            ));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/auth/resend-verification
     * Body: { "email": "firma@example.com" }
     * Süresi dolan link için yeni aktivasyon maili gönderir.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "E-posta adresi gerekli."
            ));
        }

        try {
            verificationService.resendVerificationEmail(email.trim().toLowerCase());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Yeni aktivasyon linki gönderildi. Lütfen e-postanızı kontrol edin."
            ));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
