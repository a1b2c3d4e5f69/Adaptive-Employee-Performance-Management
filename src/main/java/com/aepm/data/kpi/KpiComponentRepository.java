package com.aepm.data.kpi;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.KpiComponent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

public class KpiComponentRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // GET BY CONFIG
    // =========================================================

    public List<KpiComponent> getComponentsByConfig(
            Long configId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/kpi_component_tbl"
                        + "?select=*"
                        + "&config_id=eq."
                        + configId
                        + "&order=component_id.asc";


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
                "Unable to load KPI components"
        );


        return mapComponents(
                response.body()
        );
    }


    // =========================================================
    // GET ACTIVE BY CONFIG
    // =========================================================

    public List<KpiComponent> getActiveComponentsByConfig(
            Long configId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/kpi_component_tbl"
                        + "?select=*"
                        + "&config_id=eq."
                        + configId
                        + "&component_status=eq.Active"
                        + "&order=component_id.asc";


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
                "Unable to load active KPI components"
        );


        return mapComponents(
                response.body()
        );
    }


    // =========================================================
    // CREATE
    // =========================================================

    public void createComponent(
            KpiComponent component,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "config_id",
                component.getConfigId()
        );

        body.put(
                "component_name",
                component.getComponentName()
        );

        body.put(
                "weighting",
                component.getWeighting()
        );

        body.put(
                "component_status",
                component.getComponentStatus()
        );


        String url =
                AppConfig.REST_URL
                        + "/kpi_component_tbl";


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
                "Unable to create KPI component"
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public void updateComponent(
            KpiComponent component,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "component_name",
                component.getComponentName()
        );

        body.put(
                "weighting",
                component.getWeighting()
        );


        String url =
                AppConfig.REST_URL
                        + "/kpi_component_tbl"
                        + "?component_id=eq."
                        + component.getComponentId();


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
                "Unable to update KPI component"
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    public void updateComponentStatus(
            Long componentId,
            String status,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "component_status",
                status
        );


        String url =
                AppConfig.REST_URL
                        + "/kpi_component_tbl"
                        + "?component_id=eq."
                        + componentId;


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
                "Unable to update KPI component status"
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteComponent(
            Long componentId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/kpi_component_tbl"
                        + "?component_id=eq."
                        + componentId;


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
                "Unable to delete KPI component"
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

    private List<KpiComponent> mapComponents(
            String body
    ) {

        JSONArray array =
                new JSONArray(body);


        List<KpiComponent> components =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject json =
                    array.getJSONObject(i);


            KpiComponent component =
                    new KpiComponent();


            component.setComponentId(
                    json.getLong(
                            "component_id"
                    )
            );


            component.setConfigId(
                    json.getLong(
                            "config_id"
                    )
            );


            component.setComponentName(
                    json.optString(
                            "component_name",
                            ""
                    )
            );


            component.setWeighting(
                    json.optDouble(
                            "weighting",
                            0
                    )
            );


            component.setComponentStatus(
                    json.optString(
                            "component_status",
                            "Inactive"
                    )
            );


            components.add(
                    component
            );
        }


        return components;
    }
}