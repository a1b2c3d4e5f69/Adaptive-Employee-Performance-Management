package com.aepm.logic.ai;

import com.aepm.data.repository.AIConfigurationRepository;
import com.aepm.model.User;
import com.aepm.util.SessionManager;
import com.google.gson.JsonObject;

public class AIConfigurationService {

    private final AIConfigurationRepository repository =
            new AIConfigurationRepository();

    public JsonObject load() throws Exception {
        requireAdmin();
        return repository.getConfiguration();
    }

    public JsonObject save(
            long configId,
            boolean enabled,
            String modelName,
            boolean useKpi,
            boolean useTaskCompletion,
            boolean useAttendance,
            boolean useMissedDeadlines,
            boolean useHistory,
            int analysisPeriodDays,
            int minimumDataPoints,
            int maxRecommendations,
            String additionalPrompt
    ) throws Exception {

        requireAdmin();

            if (enabled && !useKpi) {
        throw new IllegalArgumentException(
                "KPI Performance is required while AI recommendations "
                + "are enabled. Select KPI Performance, or turn off "
                + "Enable AI Performance Recommendation."
        );
    }

        if (configId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid configuration ID."
            );
        }

        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException(
                    "Please enter a Gemini model name."
            );
        }

        if (analysisPeriodDays < 7
                || analysisPeriodDays > 365) {
            throw new IllegalArgumentException(
                    "Analysis period must be 7 to 365 days."
            );
        }

        if (minimumDataPoints < 1
                || minimumDataPoints > 13) {
            throw new IllegalArgumentException(
                    "Minimum data points must be 1 to 13: "
                    + "one current KPI plus up to 12 historical KPI periods."
            );
        }

        if (maxRecommendations < 1
                || maxRecommendations > 10) {
            throw new IllegalArgumentException(
                    "Maximum recommendations must be 1 to 10."
            );
        }

        if (enabled
                && !useKpi
                && !useTaskCompletion
                && !useAttendance
                && !useMissedDeadlines
                && !useHistory) {
            throw new IllegalArgumentException(
                    "Please select at least one "
                    + "performance indicator."
            );
        }

        if (additionalPrompt != null
                && additionalPrompt.length() > 2000) {
            throw new IllegalArgumentException(
                    "Additional prompt cannot exceed "
                    + "2000 characters."
            );
        }

        JsonObject changes = new JsonObject();

        changes.addProperty(
                "recommendation_enabled", enabled
        );
        changes.addProperty("model_name", modelName.trim());
        changes.addProperty("use_kpi", useKpi);
        changes.addProperty(
                "use_task_completion", useTaskCompletion
        );
        changes.addProperty(
                "use_attendance", useAttendance
        );
        changes.addProperty(
                "use_missed_deadlines", useMissedDeadlines
        );
        changes.addProperty(
                "use_performance_history", useHistory
        );
        changes.addProperty(
                "analysis_period_days", analysisPeriodDays
        );
        changes.addProperty(
                "minimum_data_points", minimumDataPoints
        );
        changes.addProperty(
                "max_recommendations", maxRecommendations
        );
        changes.addProperty(
                "additional_prompt",
                additionalPrompt == null
                        ? "" : additionalPrompt.trim()
        );

        return repository.saveConfiguration(
                configId, changes
        );
    }

    private void requireAdmin() {
        User user = SessionManager.getCurrentUser();

        if (user == null || user.getRoleId() != 1) {
            throw new SecurityException(
                    "Only Administrators can manage "
                    + "AI Recommendation settings."
            );
        }
    }
}
