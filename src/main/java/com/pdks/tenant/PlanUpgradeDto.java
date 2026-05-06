package com.pdks.tenant;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlanUpgradeDto {

    @NotNull(message = "Plan seçilmeli")
    private Tenant.Plan requestedPlan;

    /** Opsiyonel not — neden yükseltmek istediği */
    private String note;
}
