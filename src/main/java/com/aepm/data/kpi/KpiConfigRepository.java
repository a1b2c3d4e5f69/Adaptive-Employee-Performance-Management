package com.aepm.data.kpi;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.KpiConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;

public class KpiConfigRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // GET ALL
    // =========================================================

    public List<KpiConfig> getAllConfigs(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/kpi_config_tbl"
                        + "?select=*"
                        + "&order=created_at.desc";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(request);


        validateResponse(
                response,
                "Unable to load KPI configurations"
        );


        return mapConfigs(
                response.body()
        );
    }


    // =========================================================
    // GET ACTIVE CONFIG
    // =========================================================

    public List<KpiConfig> getActiveConfigs(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/kpi_config_tbl"
                        + "?select=*"
                        + "&config_status=eq.Active"
                        + "&order=created_at.desc";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(request);


        validateResponse(
                response,
                "Unable to load active KPI configurations"
        );


        return mapConfigs(
                response.body()
        );
    }


    // =========================================================
    // CREATE
    // =========================================================

    public void createConfig(
            KpiConfig config,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "config_name",
                config.getConfigName()
        );

        body.put(
                "risk_threshold",
                config.getRiskThreshold()
        );

        body.put(
                "effective_date",
                config.getEffectiveDate()
        );

        body.put(
                "config_status",
                config.getConfigStatus()
        );

        body.put(
                "created_by",
                config.getCreatedBy()
        );


        String url =
                AppConfig.REST_URL
                        + "/kpi_config_tbl";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .header(
                                "Prefer",
                                "return=representation"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                send(request);


        validateResponse(
                response,
                "Unable to create KPI configuration"
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public void updateConfig(
            KpiConfig config,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "config_name",
                config.getConfigName()
        );

        body.put(
                "risk_threshold",
                config.getRiskThreshold()
        );

        body.put(
                "effective_date",
                config.getEffectiveDate()
        );

        body.put(
                "updated_at",
                OffsetDateTime
                        .now()
                        .toString()
        );


        String url =
                AppConfig.REST_URL
                        + "/kpi_config_tbl"
                        + "?config_id=eq."
                        + config.getConfigId();


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .method(
                                "PATCH",
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                send(request);


        validateResponse(
                response,
                "Unable to update KPI configuration"
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    public void updateConfigStatus(
            Long configId,
            String status,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "config_status",
                status
        );

        body.put(
                "updated_at",
                OffsetDateTime
                        .now()
                        .toString()
        );


        String url =
                AppConfig.REST_URL
                        + "/kpi_config_tbl"
                        + "?config_id=eq."
                        + configId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .method(
                                "PATCH",
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                send(request);


        validateResponse(
                response,
                "Unable to update KPI configuration status"
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteConfig(
            Long configId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/kpi_config_tbl"
                        + "?config_id=eq."
                        + configId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .DELETE()
                        .build();


        HttpResponse<String> response =
                send(request);


        validateResponse(
                response,
                "Unable to delete KPI configuration"
        );
    }


    // =========================================================
    // HTTP
    // =========================================================

    private HttpResponse<String> send(
            HttpRequest request
    ) throws Exception {

        return supabaseClient
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString()
                );
    }


    private void validateResponse(
            HttpResponse<String> response,
            String message
    ) {

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    message
                            + ": "
                            + response.body()
            );
        }
    }


    // =========================================================
    // MAPPING
    // =========================================================

    private List<KpiConfig> mapConfigs(
            String body
    ) {

        JSONArray array =
                new JSONArray(body);


        List<KpiConfig> configs =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject json =
                    array.getJSONObject(i);


            KpiConfig config =
                    new KpiConfig();


            config.setConfigId(
                    json.getLong(
                            "config_id"
                    )
            );


            config.setConfigName(
                    json.optString(
                            "config_name",
                            ""
                    )
            );


            config.setRiskThreshold(
                    json.optDouble(
                            "risk_threshold",
                            0
                    )
            );


            config.setEffectiveDate(
                    json.optString(
                            "effective_date",
                            ""
                    )
            );


            config.setConfigStatus(
                    json.optString(
                            "config_status",
                            "Inactive"
                    )
            );


            if (!json.isNull("created_by")) {

                config.setCreatedBy(
                        json.optString(
                                "created_by",
                                null
                        )
                );
            }


            if (!json.isNull("created_at")) {

                config.setCreatedAt(
                        OffsetDateTime.parse(
                                json.getString(
                                        "created_at"
                                )
                        )
                );
            }


            if (!json.isNull("updated_at")) {

                config.setUpdatedAt(
                        OffsetDateTime.parse(
                                json.getString(
                                        "updated_at"
                                )
                        )
                );
            }


            configs.add(
                    config
            );
        }


        return configs;
    }
}