package com.pdks.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("Firebase credentials-path tanımlanmamış. Push notification devre dışı.");
            return;
        }

        try {
            // Firebase sınıfları reflection ile yüklenir — bağımlılık yoksa hata vermez
            Class<?> firebaseAppClass = Class.forName("com.google.firebase.FirebaseApp");
            java.util.List<?> apps = (java.util.List<?>) firebaseAppClass
                .getMethod("getApps").invoke(null);

            if (!apps.isEmpty()) {
                log.info("Firebase zaten başlatılmış.");
                return;
            }

            Resource resource = resourceLoader.getResource(credentialsPath);
            if (!resource.exists()) {
                log.warn("Firebase credentials dosyası bulunamadı: {}. Push notification devre dışı.",
                    credentialsPath);
                return;
            }

            Class<?> credClass    = Class.forName("com.google.auth.oauth2.GoogleCredentials");
            Class<?> optionsClass = Class.forName("com.google.firebase.FirebaseOptions");

            Object credentials = credClass.getMethod("fromStream", java.io.InputStream.class)
                .invoke(null, resource.getInputStream());

            Object builder = optionsClass.getMethod("builder").invoke(null);
            builder.getClass().getMethod("setCredentials",
                Class.forName("com.google.auth.Credentials"))
                .invoke(builder, credentials);
            Object options = builder.getClass().getMethod("build").invoke(builder);

            firebaseAppClass.getMethod("initializeApp", optionsClass).invoke(null, options);
            log.info("Firebase başarıyla başlatıldı.");

        } catch (ClassNotFoundException e) {
            log.warn("Firebase SDK bulunamadı (pom.xml'e eklenmemiş). Push notification devre dışı.");
        } catch (Exception e) {
            log.warn("Firebase başlatılamadı: {}. Push notification devre dışı.", e.getMessage());
        }
    }
}
