package com.pdks.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFlywayConfig {

    private final DataSource dataSource;

    /**
     * Yeni tenant şemasına V2-V6 migration'larını uygular.
     * V1 (tenants tablosu) public şemadadır, buraya dokunulmaz.
     */
    public void migrateSchema(String schemaName) {
        log.info("Flyway migration başlatılıyor: {}", schemaName);

        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/tenant-migration")
            .schemas(schemaName)
            .defaultSchema(schemaName)
            .table("flyway_schema_history")
            .outOfOrder(false)
            .cleanDisabled(true)
            .load();

        var result = flyway.migrate();
        log.info("Migration tamamlandı: {} - {} script uygulandı",
            schemaName, result.migrationsExecuted);
    }
}
