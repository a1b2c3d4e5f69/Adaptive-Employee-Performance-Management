package com.aepm.config;

public final class AppConfig {

    private AppConfig() {
    }

    public static final String APP_NAME =
            "Adaptive Employee Performance Management System";

    public static final String APP_SHORT_NAME =
            "AEPM";

    // SUPABASE
    public static final String SUPABASE_URL =
            "https://sxlptnfmvqxuqbgvifyu.supabase.co";

    public static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_prI91xLxcuLAF6PqFAvnkQ_CjvDNe2a";

    // SUPABASE API
    public static final String AUTH_URL =
            SUPABASE_URL + "/auth/v1";

    public static final String REST_URL =
            SUPABASE_URL + "/rest/v1";

    public static final String FUNCTIONS_URL =
        SUPABASE_URL + "/functions/v1";
}