package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


/**
 * Loads the active task progress verification configuration
 * from Supabase.
 */
public class ProgressVerificationConfigRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // CONFIG MODEL
    // =========================================================

    public static class ProgressVerificationConfig {

        private final int reviewTimeoutHours;

        private final boolean autoApprovalEnabled;

        private final double minimumConfidence;

        private final int maxAutoProgressIncrease;

        private final boolean allowAutoApproveCompletion;


        public ProgressVerificationConfig(
                int reviewTimeoutHours,
                boolean autoApprovalEnabled,
                double minimumConfidence,
                int maxAutoProgressIncrease,
                boolean allowAutoApproveCompletion
        ) {

            this.reviewTimeoutHours =
                    reviewTimeoutHours;

            this.autoApprovalEnabled =
                    autoApprovalEnabled;

            this.minimumConfidence =
                    minimumConfidence;

            this.maxAutoProgressIncrease =
                    maxAutoProgressIncrease;

            this.allowAutoApproveCompletion =
                    allowAutoApproveCompletion;
        }


        public int getReviewTimeoutHours() {
            return reviewTimeoutHours;
        }


        public boolean isAutoApprovalEnabled() {
            return autoApprovalEnabled;
        }


        public double getMinimumConfidence() {
            return minimumConfidence;
        }


        public int getMaxAutoProgressIncrease() {
            return maxAutoProgressIncrease;
        }


        public boolean isAllowAutoApproveCompletion() {
            return allowAutoApproveCompletion;
        }


        @Override
        public String toString() {

            return "ProgressVerificationConfig{" +
                    "reviewTimeoutHours=" + reviewTimeoutHours +
                    ", autoApprovalEnabled=" + autoApprovalEnabled +
                    ", minimumConfidence=" + minimumConfidence +
                    ", maxAutoProgressIncrease=" + maxAutoProgressIncrease +
                    ", allowAutoApproveCompletion=" + allowAutoApproveCompletion +
                    '}';
        }
    }


    // =========================================================
    // LOAD CONFIG
    // =========================================================

    public ProgressVerificationConfig getConfig(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_progress_verification_config_tbl"
                        + "?select="
                        + "review_timeout_hours,"
                        + "auto_approval_enabled,"
                        + "minimum_confidence,"
                        + "max_auto_progress_increase,"
                        + "allow_auto_approve_completion"
                        + "&limit=1";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                supabaseClient
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );


        validate(
                response,
                "Unable to load progress verification configuration"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            return getDefaultConfig();
        }


        JSONObject row =
                array.getJSONObject(0);


        int reviewTimeoutHours =
                row.optInt(
                        "review_timeout_hours",
                        24
                );


        boolean autoApprovalEnabled =
                row.optBoolean(
                        "auto_approval_enabled",
                        true
                );


        double minimumConfidence =
                row.optDouble(
                        "minimum_confidence",
                        80.0
                );


        int maxAutoProgressIncrease =
                row.optInt(
                        "max_auto_progress_increase",
                        25
                );


        boolean allowAutoApproveCompletion =
                row.optBoolean(
                        "allow_auto_approve_completion",
                        false
                );


        // Safety fallback
        if (reviewTimeoutHours <= 0) {

            reviewTimeoutHours = 24;
        }


        if (
                minimumConfidence < 0
                        || minimumConfidence > 100
        ) {

            minimumConfidence = 80.0;
        }


        if (
                maxAutoProgressIncrease <= 0
                        || maxAutoProgressIncrease > 100
        ) {

            maxAutoProgressIncrease = 25;
        }


        return new ProgressVerificationConfig(
                reviewTimeoutHours,
                autoApprovalEnabled,
                minimumConfidence,
                maxAutoProgressIncrease,
                allowAutoApproveCompletion
        );
    }


    // =========================================================
    // DEFAULT CONFIG
    // =========================================================

    public ProgressVerificationConfig getDefaultConfig() {

        return new ProgressVerificationConfig(

                24,

                true,

                80.0,

                25,

                false
        );
    }


    // =========================================================
    // VALIDATE RESPONSE
    // =========================================================

    private void validate(
            HttpResponse<String> response,
            String message
    ) {

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    message
                            + " [HTTP "
                            + response.statusCode()
                            + "]: "
                            + response.body()
            );
        }
    }
}