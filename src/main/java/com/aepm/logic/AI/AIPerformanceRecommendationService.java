package com.aepm.logic.ai;

import com.google.gson.JsonObject;
import com.aepm.data.repository.AIConfigurationRepository;
import com.google.gson.JsonElement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public final class AIPerformanceRecommendationService {

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");

    private final AIPerformanceAnalysisService analysisService =
            new AIPerformanceAnalysisService();

    public record RecommendationResult(
            JsonObject snapshot,
            JsonObject recommendation,
            JsonObject savedRecord
    ) {}

    public RecommendationResult generate(
            UUID employeeId,
            LocalDate start,
            LocalDate end,
            Long kpiResultId,
            String modelName,
            String additionalPrompt
    ) throws Exception {

        var requestingUser = com.aepm.util.SessionManager.getCurrentUser();

        if (requestingUser == null) {
            throw new IllegalStateException("Please sign in again.");
        }

        final UUID requestingManagerId =
                UUID.fromString(requestingUser.getUserId());

        final String generationAccessToken =
                com.aepm.util.SessionManager.getAccessToken();

        if (generationAccessToken == null
                || generationAccessToken.isBlank()) {
            throw new IllegalStateException("Please sign in again.");
        }

        JsonObject configuration;
        try {
            System.out.println("[AI] Step 1: Loading AI configuration");

            configuration =
                    new AIConfigurationRepository().getConfiguration();

            System.out.println("[AI] Step 1 completed");

        } catch (java.net.http.HttpTimeoutException ex) {
            throw new IllegalStateException(
                    "Loading AI settings from Supabase timed out. "
                    + "Please check your connection and try again.",
                    ex
            );
        }

        if (kpiResultId == null || kpiResultId <= 0) {
            throw new IllegalArgumentException(
                    "A valid selected KPI result ID is required."
            );
        }

        if (configuration == null) {
            throw new IllegalStateException(
                    "AI configuration is unavailable."
            );
        }

        if (!requireConfigurationBoolean(
                configuration, "recommendation_enabled")) {
            throw new IllegalStateException(
                    "AI recommendations are disabled by the administrator."
            );
        }

        if (!requireConfigurationBoolean(configuration, "use_kpi")) {
            throw new IllegalStateException(
                    "The current generation flow requires KPI analysis "
                    + "to be enabled."
            );
        }

        // Administrator configuration takes precedence over caller values.
        modelName = requireConfigurationString(
                configuration, "model_name"
        ).trim();

        additionalPrompt = optionalConfigurationString(
                configuration, "additional_prompt"
        );

        validateRequest(employeeId, start, end, modelName);

        if (additionalPrompt.length() > 2000) {
            throw new IllegalStateException(
                    "Configured additional prompt exceeds 2000 characters."
            );
        }

        int analysisPeriodDays = requireConfigurationInt(
                configuration, "analysis_period_days", 7, 365
        );

        int minimumDataPoints = requireConfigurationInt(
                configuration, "minimum_data_points", 1, 13
        );

        int maxRecommendations = requireConfigurationInt(
                configuration, "max_recommendations", 1, 10
        );

        // Both start and end are included in the reporting period.
        long requestedDays = java.time.temporal.ChronoUnit.DAYS
                .between(start, end) + 1;

        // This flow treats analysis_period_days as the maximum
        // permitted length of a manually selected reporting period.
        if (requestedDays > analysisPeriodDays) {
            throw new IllegalArgumentException(
                    "The selected reporting period contains "
                    + requestedDays + " days. AI settings allow at most "
                    + analysisPeriodDays + " days."
            );
        }

        String generationInstructions = additionalPrompt
                + "\nReturn at most "
                + maxRecommendations
                + " items in the recommendations array."
                + "\nReturn fewer items, including zero, when evidence "
                + "does not support additional recommendations.";

        GeminiAIService gemini = new GeminiAIService(modelName);

        // Uses the existing repository and snapshot table.
        JsonObject savedSnapshot;

        try {
            System.out.println(
                    "[AI] Step 2: Reading KPI and saving snapshot"
            );

            savedSnapshot = analysisService.analyseKpiAndSave(
                    employeeId,
                    start,
                    end,
                    kpiResultId
            );

            System.out.println("[AI] Step 2 completed");

        } catch (java.net.http.HttpTimeoutException ex) {
            throw new IllegalStateException(
                    "Reading KPI data or saving the snapshot "
                    + "in Supabase timed out. Please try again.",
                    ex
            );
        }

        validateSavedIdentity(
                savedSnapshot,
                employeeId,
                start,
                end
        );

        JsonObject savedMetrics =
        savedSnapshot.getAsJsonObject("additional_metrics");

        if (savedMetrics == null
                || !savedMetrics.has("kpi_result_id")
                || savedMetrics.get("kpi_result_id").isJsonNull()
                || !kpiResultId.toString().equals(
                        savedMetrics.get("kpi_result_id").getAsString()
                )) {
            throw new IllegalStateException(
                    "The saved snapshot does not match the selected KPI result."
            );
        }

        // Reject preliminary or inconsistent saved data.
        // This method never changes verification flags.
        JsonObject verifiedSnapshot =
                AIPerformanceDataValidator.validateForGeneration(
                        savedSnapshot
                );
        boolean useHistory = requireConfigurationBoolean(configuration, "use_performance_history");

        JsonObject history;

        try {
            history = new AIHistoricalKpiService().load(
                    employeeId,
                    start,
                    useHistory,
                    generationAccessToken
            );
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IllegalStateException(
                    "Historical KPI could not be loaded or validated. "
                    + "No Gemini request was sent. Please try again.",
                    ex
            );
        }

        verifiedSnapshot
                .getAsJsonObject("additional_metrics")
                .add("historical_performance", history);
                int historicalDataPoints = useHistory
                ? history.getAsJsonArray("records").size()
                : 0;

        int availableDataPoints = 1 + historicalDataPoints;

        System.out.printf(
                "[AI] KPI data points: required=%d, available=%d "
                        + "(current=1, historical=%d)%n",
                minimumDataPoints,
                availableDataPoints,
                historicalDataPoints
        );

        if (availableDataPoints < minimumDataPoints) {
            throw new IllegalStateException(
                    "Insufficient KPI data. Required: "
                    + minimumDataPoints
                    + "; available: "
                    + availableDataPoints
                    + " (1 current KPI + "
                    + historicalDataPoints
                    + " historical KPI periods). "
                    + (useHistory
                        ? "There are not enough eligible earlier KPI periods. "
                        : "Historical Performance is disabled. ")
                    + "No AI recommendation was generated."
            );
        }

        JsonObject dataPointEvidence = new JsonObject();
        dataPointEvidence.addProperty("required", minimumDataPoints);
        dataPointEvidence.addProperty("available", availableDataPoints);
        dataPointEvidence.addProperty("current_kpi_records", 1);
        dataPointEvidence.addProperty(
                "historical_kpi_records", historicalDataPoints
        );
        dataPointEvidence.addProperty(
                "definition",
                "One current KPI plus the eligible historical KPI periods "
                        + "included in this generation."
        );

        verifiedSnapshot.getAsJsonObject("additional_metrics")
                .add("data_point_requirement", dataPointEvidence);

        System.out.printf(
                "[AI] Historical performance: enabled=%s, status=%s, records=%d%n",
                useHistory,
                history.get("status").getAsString(),
                history.get("record_count").getAsInt()
        );
        boolean useDeadlines = requireConfigurationBoolean(
                configuration, "use_missed_deadlines"
        );

        JsonObject deadlineEvidence;

        try {
            deadlineEvidence = new AIDeadlineEvidenceService().load(
                    employeeId,
                    start,
                    end,
                    useDeadlines,
                    generationAccessToken
            );
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IllegalStateException(
                    "Deadline evidence could not be loaded or validated. "
                    + "No Gemini request was sent. "
                    + ex.getMessage(),
                    ex
            );
        }

        verifiedSnapshot
                .getAsJsonObject("additional_metrics")
                .add("deadline_evidence", deadlineEvidence);

        System.out.printf(
                "[AI] Deadline evidence: enabled=%s, status=%s%n",
                useDeadlines,
                deadlineEvidence.get("status").getAsString()
        );
        JsonObject indicatorSelection = new JsonObject();

        indicatorSelection.addProperty(
                "use_attendance",
                requireConfigurationBoolean(configuration, "use_attendance")
        );

        indicatorSelection.addProperty(
                "use_task_completion",
                requireConfigurationBoolean(configuration, "use_task_completion")
        );

        verifiedSnapshot.getAsJsonObject("additional_metrics")
                .add("indicator_selection", indicatorSelection);

        // Store the exact structured evidence prepared for Gemini.
        // This excludes disabled component scores.
        JsonObject preparedAiInput =
                gemini.buildKpiOnlyInput(verifiedSnapshot);

        verifiedSnapshot.getAsJsonObject("additional_metrics")
                .add("ai_input", preparedAiInput.deepCopy());

        System.out.printf(
                "[AI] Selected indicators: attendance=%s, completion_rate=%s%n",
                indicatorSelection.get("use_attendance").getAsBoolean(),
                indicatorSelection.get("use_task_completion").getAsBoolean()
        );
        JsonObject recommendation;

        try {
            System.out.println(
                    "[AI] Step 3: Requesting Gemini recommendations"
            );

            recommendation = gemini.generateRecommendation(
                    verifiedSnapshot,
                    generationInstructions
            );

            System.out.println(
                    "[AI] Step 3 completed: Response received and validated"
            );

        } catch (java.net.http.HttpTimeoutException ex) {
            throw new IllegalStateException(
                    "Gemini did not respond before the request timed out. "
                    + "The KPI snapshot was saved, but no AI "
                    + "recommendation was returned to the app.",
                    ex
            );
        }

        int recommendationCount = recommendation
                .getAsJsonArray("recommendations")
                .size();

        if (recommendationCount > maxRecommendations) {
            throw new IllegalStateException(
                    "Gemini returned "
                    + recommendationCount
                    + " recommendations, exceeding the configured maximum of "
                    + maxRecommendations
                    + ". The result was not accepted."
            );
        }

        var currentUser =
        com.aepm.util.SessionManager.getCurrentUser();

        if (currentUser == null
                || !requestingManagerId.toString().equals(
                        currentUser.getUserId())) {
            throw new IllegalStateException(
                    "The signed-in user changed. The recommendation was not saved."
            );
        }

        JsonObject savedRecord =
                new com.aepm.data.repository.AIRecommendationRepository()
                        .save(
                                verifiedSnapshot,
                                recommendation,
                                modelName,
                                requestingManagerId,
                                generationAccessToken
                        );

        System.out.println(
                "[AI] Recommendation saved: "
                + savedRecord.get("recommendation_id").getAsString()
        );

        return new RecommendationResult(
                verifiedSnapshot.deepCopy(),
                recommendation.deepCopy(),
                savedRecord.deepCopy()
        );
    }

    private static void validateRequest(
            UUID employeeId,
            LocalDate start,
            LocalDate end,
            String modelName
    ) {
        if (employeeId == null || start == null || end == null) {
            throw new IllegalArgumentException(
                    "Employee and reporting dates are required."
            );
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "Reporting end date cannot precede start date."
            );
        }

        if (end.isAfter(LocalDate.now(ZONE))) {
            throw new IllegalArgumentException(
                    "Reporting end date cannot be in the future."
            );
        }

        if (modelName == null
                || !modelName.matches("[a-zA-Z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "A valid Gemini model name is required."
            );
        }
    }

    private static void validateSavedIdentity(
            JsonObject snapshot,
            UUID employeeId,
            LocalDate start,
            LocalDate end
    ) {
        if (snapshot == null) {
            throw new IllegalStateException(
                    "Snapshot repository returned no record."
            );
        }

        requireMatchingString(
                snapshot,
                "employee_id",
                employeeId.toString(),
                true
        );

        requireMatchingString(
                snapshot,
                "period_start",
                start.toString(),
                false
        );

        requireMatchingString(
                snapshot,
                "period_end",
                end.toString(),
                false
        );
    }

    private static void requireMatchingString(
            JsonObject object,
            String key,
            String expected,
            boolean ignoreCase
    ) {
        var value = object.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException(
                    "Saved snapshot is missing a valid " + key + "."
            );
        }

        String actual = value.getAsString();

        boolean matches = ignoreCase
                ? expected.equalsIgnoreCase(actual)
                : expected.equals(actual);

        if (!matches) {
            throw new IllegalStateException(
                    "Saved snapshot " + key
                    + " does not match the analysis request."
            );
        }
    }

    private void requireKpiGenerationEnabled() throws Exception {

        JsonObject configuration =
                new AIConfigurationRepository().getConfiguration();

        if (configuration == null) {
            throw new IllegalStateException(
                    "AI configuration is unavailable."
            );
        }

        boolean enabled = requireConfigurationBoolean(
                configuration,
                "recommendation_enabled"
        );

        if (!enabled) {
            throw new IllegalStateException(
                    "AI recommendations are disabled by the administrator."
            );
        }

        boolean useKpi = requireConfigurationBoolean(
                configuration,
                "use_kpi"
        );

        if (!useKpi) {
            throw new IllegalStateException(
                    "KPI analysis is disabled in AI settings. "
                    + "The current generation flow requires KPI analysis."
            );
        }
    }

    private boolean requireConfigurationBoolean(
            JsonObject configuration,
            String key
    ) {
        JsonElement value = configuration.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalStateException(
                    "AI configuration field "
                    + key + " must be a JSON boolean."
            );
        }

        return value.getAsBoolean();
    }

    private String requireConfigurationString(
            JsonObject configuration,
            String key
    ) {
        JsonElement value = configuration.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new IllegalStateException(
                    "AI configuration field "
                    + key + " must be a nonblank string."
            );
        }

        return value.getAsString();
    }

    private String optionalConfigurationString(
            JsonObject configuration,
            String key
    ) {
        JsonElement value = configuration.get(key);

        if (value == null || value.isJsonNull()) {
            return "";
        }

        if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException(
                    "AI configuration field "
                    + key + " must be a string."
            );
        }

        return value.getAsString().trim();
    }

    private int requireConfigurationInt(
            JsonObject configuration,
            String key,
            int minimum,
            int maximum
    ) {
        JsonElement value = configuration.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException(
                    "AI configuration field "
                    + key + " must be a JSON number."
            );
        }

        int number;

        try {
            number = new java.math.BigDecimal(
                    value.getAsString()
            ).intValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalStateException(
                    "AI configuration field "
                    + key + " must be a finite whole number.",
                    ex
            );
        }

        if (number < minimum || number > maximum) {
            throw new IllegalStateException(
                    "AI configuration field "
                    + key + " must be between "
                    + minimum + " and " + maximum + "."
            );
        }

        return number;
    }
}