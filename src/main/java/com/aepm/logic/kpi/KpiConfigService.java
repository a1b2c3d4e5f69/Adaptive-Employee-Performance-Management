package com.aepm.logic.kpi;

import com.aepm.data.kpi.KpiConfigRepository;

import com.aepm.model.KpiConfig;
import com.aepm.model.Role;
import com.aepm.model.User;

import com.aepm.util.SessionManager;

import java.util.List;

public class KpiConfigService {

    private final KpiConfigRepository repository =
            new KpiConfigRepository();

    private final KpiComponentService componentService =
            new KpiComponentService();


    // =========================================================
    // GET
    // =========================================================

    public List<KpiConfig> getAllConfigs()
            throws Exception {

        requireAdmin();


        return repository
                .getAllConfigs(
                        requireAccessToken()
                );
    }


    public List<KpiConfig> getActiveConfigs()
            throws Exception {

        return repository
                .getActiveConfigs(
                        requireAccessToken()
                );
    }


    // =========================================================
    // CREATE
    // New config starts INACTIVE
    // =========================================================

    public void createConfig(
            String configName,
            double riskThreshold,
            String effectiveDate
    ) throws Exception {

        User admin =
                requireAdmin();


        validateConfig(
                configName,
                riskThreshold,
                effectiveDate
        );


        KpiConfig config =
                new KpiConfig();


        config.setConfigName(
                configName.trim()
        );

        config.setRiskThreshold(
                riskThreshold
        );

        config.setEffectiveDate(
                effectiveDate.trim()
        );


        // Important:
        // Create first as inactive.
        // Add components until weighting = 100%.
        config.setConfigStatus(
                "Inactive"
        );


        config.setCreatedBy(
                admin.getUserId()
        );


        repository.createConfig(
                config,
                requireAccessToken()
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public void updateConfig(
            KpiConfig config
    ) throws Exception {

        requireAdmin();


        if (
                config == null
                        || config.getConfigId() == null
        ) {

            throw new IllegalArgumentException(
                    "Invalid KPI configuration."
            );
        }


        validateConfig(
                config.getConfigName(),
                config.getRiskThreshold(),
                config.getEffectiveDate()
        );


        repository.updateConfig(
                config,
                requireAccessToken()
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    public void updateConfigStatus(
            KpiConfig config,
            String newStatus
    ) throws Exception {

        requireAdmin();


        if (
                config == null
                        || config.getConfigId() == null
        ) {

            throw new IllegalArgumentException(
                    "Invalid KPI configuration."
            );
        }


        if (
                !"Active".equals(newStatus)
                        && !"Inactive".equals(newStatus)
        ) {

            throw new IllegalArgumentException(
                    "Invalid KPI configuration status."
            );
        }


        // Before activation:
        // total active components MUST equal 100%
        if (
                "Active".equals(newStatus)
        ) {

            double totalWeighting =
                    componentService
                            .getTotalActiveWeighting(
                                    config.getConfigId()
                            );


            if (
                    Math.abs(
                            totalWeighting - 100
                    ) > 0.001
            ) {

                throw new IllegalStateException(
                        String.format(
                                "KPI configuration cannot be activated. "
                                        + "Active component weighting must total 100%%. "
                                        + "Current total: %.2f%%",
                                totalWeighting
                        )
                );
            }
        }


        repository.updateConfigStatus(
                config.getConfigId(),
                newStatus,
                requireAccessToken()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteConfig(
            Long configId
    ) throws Exception {

        requireAdmin();


        if (configId == null) {

            throw new IllegalArgumentException(
                    "Invalid KPI configuration."
            );
        }


        repository.deleteConfig(
                configId,
                requireAccessToken()
        );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateConfig(
            String name,
            double riskThreshold,
            String effectiveDate
    ) {

        if (
                name == null
                        || name.trim().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Configuration name is required."
            );
        }


        if (
                name.trim().length() > 100
        ) {

            throw new IllegalArgumentException(
                    "Configuration name cannot exceed 100 characters."
            );
        }


        if (
                riskThreshold < 0
                        || riskThreshold > 100
        ) {

            throw new IllegalArgumentException(
                    "Risk threshold must be between 0 and 100."
            );
        }


        if (
                effectiveDate == null
                        || effectiveDate.trim().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Effective date is required."
            );
        }
    }


    // =========================================================
    // SESSION
    // =========================================================

    private String requireAccessToken() {

        String token =
                SessionManager.getAccessToken();


        if (
                token == null
                        || token.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        return token;
    }


    private User requireAdmin() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        boolean admin =
                user.getRole() == Role.ADMINISTRATOR
                        || user.getRoleId() == 1;


        if (!admin) {

            throw new SecurityException(
                    "Only administrators can manage KPI configurations."
            );
        }


        return user;
    }
}