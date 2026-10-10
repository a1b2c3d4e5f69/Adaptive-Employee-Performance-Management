package com.aepm.logic.kpi;

import com.aepm.data.kpi.KpiComponentRepository;

import com.aepm.model.KpiComponent;
import com.aepm.model.Role;
import com.aepm.model.User;

import com.aepm.util.SessionManager;

import java.util.List;

public class KpiComponentService {

    private final KpiComponentRepository repository =
            new KpiComponentRepository();


    // =========================================================
    // GET
    // =========================================================

    public List<KpiComponent> getComponentsByConfig(
            Long configId
    ) throws Exception {

        if (configId == null) {

            throw new IllegalArgumentException(
                    "Invalid KPI configuration."
            );
        }


        return repository
                .getComponentsByConfig(
                        configId,
                        requireAccessToken()
                );
    }


    public List<KpiComponent> getActiveComponentsByConfig(
            Long configId
    ) throws Exception {

        if (configId == null) {

            throw new IllegalArgumentException(
                    "Invalid KPI configuration."
            );
        }


        return repository
                .getActiveComponentsByConfig(
                        configId,
                        requireAccessToken()
                );
    }


    // =========================================================
    // CREATE
    // =========================================================

    public void createComponent(
            Long configId,
            String componentName,
            double weighting
    ) throws Exception {

        requireAdmin();


        validateComponent(
                configId,
                componentName,
                weighting
        );


        double currentTotal =
                getTotalActiveWeighting(
                        configId
                );


        if (
                currentTotal + weighting > 100.0001
        ) {

            throw new IllegalArgumentException(
                    "Total active KPI weighting cannot exceed 100%."
            );
        }


        KpiComponent component =
                new KpiComponent();


        component.setConfigId(
                configId
        );

        component.setComponentName(
                componentName.trim()
        );

        component.setWeighting(
                weighting
        );

        component.setComponentStatus(
                "Active"
        );


        repository.createComponent(
                component,
                requireAccessToken()
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public void updateComponent(
            KpiComponent component
    ) throws Exception {

        requireAdmin();


        if (
                component == null
                        || component.getComponentId() == null
        ) {

            throw new IllegalArgumentException(
                    "Invalid KPI component."
            );
        }


        validateComponent(
                component.getConfigId(),
                component.getComponentName(),
                component.getWeighting()
        );


        if (
                "Active".equals(
                        component.getComponentStatus()
                )
        ) {

            List<KpiComponent> components =
                    getComponentsByConfig(
                            component.getConfigId()
                    );


            double total =
                    0;


            for (KpiComponent item : components) {

                if (
                        item.getComponentId()
                                .equals(
                                        component.getComponentId()
                                )
                ) {

                    continue;
                }


                if (
                        "Active".equals(
                                item.getComponentStatus()
                        )
                ) {

                    total +=
                            item.getWeighting();
                }
            }


            total +=
                    component.getWeighting();


            if (total > 100.0001) {

                throw new IllegalArgumentException(
                        "Total active KPI weighting cannot exceed 100%."
                );
            }
        }


        repository.updateComponent(
                component,
                requireAccessToken()
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    public void updateComponentStatus(
            KpiComponent component,
            String newStatus
    ) throws Exception {

        requireAdmin();


        if (
                component == null
                        || component.getComponentId() == null
        ) {

            throw new IllegalArgumentException(
                    "Invalid KPI component."
            );
        }


        validateStatus(
                newStatus
        );


        if (
                "Active".equals(
                        newStatus
                )
                        && !"Active".equals(
                        component.getComponentStatus()
                )
        ) {

            double currentTotal =
                    getTotalActiveWeighting(
                            component.getConfigId()
                    );


            if (
                    currentTotal
                            + component.getWeighting()
                            > 100.0001
            ) {

                throw new IllegalArgumentException(
                        "Activating this component would make total weighting exceed 100%."
                );
            }
        }


        repository.updateComponentStatus(
                component.getComponentId(),
                newStatus,
                requireAccessToken()
        );
    }


    // =========================================================
    // TOTAL WEIGHTING
    // =========================================================

    public double getTotalActiveWeighting(
            Long configId
    ) throws Exception {

        List<KpiComponent> components =
                repository.getActiveComponentsByConfig(
                        configId,
                        requireAccessToken()
                );


        double total =
                0;


        for (KpiComponent component : components) {

            total +=
                    component.getWeighting();
        }


        return total;
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteComponent(
            Long componentId
    ) throws Exception {

        requireAdmin();


        if (componentId == null) {

            throw new IllegalArgumentException(
                    "Invalid KPI component."
            );
        }


        repository.deleteComponent(
                componentId,
                requireAccessToken()
        );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateComponent(
            Long configId,
            String name,
            double weighting
    ) {

        if (configId == null) {

            throw new IllegalArgumentException(
                    "KPI configuration is required."
            );
        }


        if (
                name == null
                        || name.trim().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Component name is required."
            );
        }


        if (
                name.trim().length() > 100
        ) {

            throw new IllegalArgumentException(
                    "Component name cannot exceed 100 characters."
            );
        }


        if (
                weighting <= 0
                        || weighting > 100
        ) {

            throw new IllegalArgumentException(
                    "Weighting must be greater than 0 and not exceed 100."
            );
        }
    }


    private void validateStatus(
            String status
    ) {

        if (
                !"Active".equals(status)
                        && !"Inactive".equals(status)
        ) {

            throw new IllegalArgumentException(
                    "Invalid KPI component status."
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
                    "Only administrators can manage KPI components."
            );
        }


        return user;
    }
}