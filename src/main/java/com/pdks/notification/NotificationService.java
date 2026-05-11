package com.pdks.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * E-posta doğrulama / aktivasyon maili.
     * Kullanıcı linke tıklayınca hesap aktifleşir ve 14 günlük trial başlar.
     */
    public void sendVerificationEmail(String toEmail, String companyName, java.util.UUID token) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("MAIL_USERNAME tanımlı değil, aktivasyon maili gönderilmedi: {}", toEmail);
            return;
        }

        try {
            // Base URL — production'da env variable olacak
            String baseUrl = "http://localhost:3000";
            String verifyUrl = baseUrl + "/verify-email?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("PDKS - E-posta Adresinizi Doğrulayın ✉️");

            String html = """
                <div style="font-family:sans-serif;max-width:520px;margin:0 auto">
                  <div style="background:#2563eb;padding:24px;border-radius:12px 12px 0 0">
                    <h1 style="color:white;margin:0;font-size:24px">PDKS</h1>
                    <p style="color:#bfdbfe;margin:4px 0 0">Personel Devam Kontrol Sistemi</p>
                  </div>
                  <div style="background:white;padding:32px;border:1px solid #e5e7eb;border-radius:0 0 12px 12px">
                    <h2 style="color:#1f2937;margin-top:0">E-posta Adresinizi Doğrulayın</h2>
                    <p style="color:#4b5563">
                      <strong>%s</strong> firması için hesabınız oluşturuldu.
                      Hesabınızı aktifleştirmek ve <strong>14 günlük ücretsiz deneme</strong>
                      sürenizi başlatmak için aşağıdaki butona tıklayın.
                    </p>
                    <div style="text-align:center;margin:32px 0">
                      <a href="%s"
                         style="display:inline-block;padding:14px 32px;background:#2563eb;color:white;
                                text-decoration:none;border-radius:8px;font-weight:600;font-size:16px">
                        ✅ Hesabımı Aktifleştir
                      </a>
                    </div>
                    <p style="color:#6b7280;font-size:13px">
                      Bu link <strong>24 saat</strong> geçerlidir.
                      Butona tıklayamıyorsanız aşağıdaki linki tarayıcınıza kopyalayın:
                    </p>
                    <p style="word-break:break-all;font-size:12px;color:#2563eb;background:#eff6ff;
                              padding:8px 12px;border-radius:6px">%s</p>
                    <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0">
                    <p style="color:#9ca3af;font-size:12px;margin:0">
                      Bu maili siz talep etmediyseniz güvenle yoksayabilirsiniz.
                    </p>
                  </div>
                </div>
            """.formatted(companyName, verifyUrl, verifyUrl);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Aktivasyon maili gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("Aktivasyon maili gönderilemedi: {} - {}", toEmail, e.getMessage());
        }
    }

    public void sendWelcomeEmail(String toEmail,
                                 String companyName,
                                 String password) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("MAIL_USERNAME tanımlı değil, email gönderilmedi: {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("PDKS - Hoş Geldiniz! 🎉");

            String html = """
                <div style="font-family:sans-serif;max-width:500px;margin:0 auto">
                  <div style="background:#2563eb;padding:24px;border-radius:12px 12px 0 0">
                    <h1 style="color:white;margin:0;font-size:24px">PDKS</h1>
                    <p style="color:#bfdbfe;margin:4px 0 0">Personel Devam Kontrol Sistemi</p>
                  </div>
                  <div style="background:white;padding:32px;border:1px solid #e5e7eb;border-radius:0 0 12px 12px">
                    <h2 style="color:#1f2937;margin-top:0">Hoş Geldiniz! 👋</h2>
                    <p style="color:#4b5563"><strong>%s</strong> firması için hesabınız oluşturuldu.</p>
                    <div style="background:#f9fafb;border-radius:8px;padding:16px;margin:20px 0">
                      <p style="margin:0 0 8px;font-size:13px;color:#6b7280">Giriş Bilgileri</p>
                      <p style="margin:0 0 4px"><strong>Email:</strong> %s</p>
                      <p style="margin:0"><strong>Şifre:</strong>
                        <span style="font-family:monospace;background:#eff6ff;padding:2px 8px;border-radius:4px;color:#2563eb">%s</span>
                      </p>
                    </div>
                    <a href="http://localhost:3000"
                       style="display:inline-block;padding:12px 24px;background:#2563eb;color:white;
                              text-decoration:none;border-radius:8px;font-weight:600">
                      Giriş Yap →
                    </a>
                    <p style="color:#9ca3af;font-size:12px;margin-top:24px">
                      Şifrenizi ilk girişten sonra değiştirmenizi öneririz.
                    </p>
                  </div>
                </div>
            """.formatted(companyName, toEmail, password);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Hoşgeldin emaili gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("Email gönderilemedi: {} - {}", toEmail, e.getMessage());
        }
    }
}