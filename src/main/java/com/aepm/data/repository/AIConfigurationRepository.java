package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class AIConfigurationRepository {

    private final SupabaseClient client =
            SupabaseClient.getInstance();

    private String token() {
        String token = SessionManager.getAccessToken();

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Please log in first."
            );
        }
        return token;
    }

    private String url() {
        return AppConfig.REST_URL + "/ai_config_tbl";
    }

    private HttpResponse<String> send(HttpRequest request)
            throws Exception {

        HttpResponse<String> response =
                client.getHttpClient().send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Supabase error (" + response.statusCode()
                    + "): " + response.body()
            );
        }

        return response;
    }

    public JsonObject getConfiguration() throws Exception {

        HttpRequest request = client
                .createAuthenticatedRequest(
                        url() + "?select=*&order=ai_config_id.asc&limit=1",
                        token()
                )
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        JsonArray array = JsonParser.parseString(
                send(request).body()
        ).getAsJsonArray();

        if (array.isEmpty()) {
            throw new IllegalStateException(
                    "AI configuration record not found. "
                    + "Please create the initial configuration."
            );
        }

        return array.get(0).getAsJsonObject();
    }

    public JsonObject saveConfiguration(
            long configId,
            JsonObject changes
    ) throws Exception {

        String currentUserId =
                SessionManager.getCurrentUser().getUserId();

        changes.addProperty("updated_by", currentUserId);
        changes.addProperty(
                "updated_at",
                java.time.OffsetDateTime.now().toString()
        );

        String endpoint = url()
                + "?ai_config_id=eq."
                + URLEncoder.encode(
                        String.valueOf(configId),
                        StandardCharsets.UTF_8
                )
                + "&select=*";

        HttpRequest request = client
                .createAuthenticatedRequest(endpoint, token())
                .header("Prefer", "return=representation")
                .timeout(Duration.ofSeconds(20))
                .method(
                        "PATCH",
                        HttpRequest.BodyPublishers.ofString(
                                changes.toString()
                        )
                )
                .build();

        JsonArray result = JsonParser.parseString(
                send(request).body()
        ).getAsJsonArray();

        if (result.isEmpty()) {
            throw new IllegalStateException(
                    "No configuration was updated. "
                    + "Check Admin permissions and RLS policies."
            );
        }

        return result.get(0).getAsJsonObject();
    }
}
