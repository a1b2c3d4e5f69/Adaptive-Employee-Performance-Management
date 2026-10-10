package com.aepm.data.kpi;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.KpiResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;

public class KpiResultRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // CREATE RESULT
    // =========================================================

    public void createResult(
            KpiResult result,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "employee_id",
                result.getEmployeeId()
        );

        body.put(
                "config_id",
                result.getConfigId()
        );

        body.put(
                "evaluation_start_date",
                result.getEvaluationStartDate()
                        .toString()
        );

        body.put(
                "evaluation_end_date",
                result.getEvaluationEndDate()
                        .toString()
        );

        body.put(
                "productivity_score",
                result.getProductivityScore()
        );

        body.put(
                "timeliness_score",
                result.getTimelinessScore()
        );

        body.put(
                "completion_rate",
                result.getCompletionRate()
        );

        body.put(
                "attendance_score",
                result.getAttendanceScore()
        );

        body.put(
                "overall_kpi_score",
                result.getOverallKpiScore()
        );

        body.put(
                "performance_status",
                result.getPerformanceStatus()
        );


        String url =
                AppConfig.REST_URL
                        + "/kpi_result_tbl";


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
                "Unable to save KPI result"
        );
    }


    // =========================================================
    // GET EMPLOYEE RESULTS
    // =========================================================

    public List<KpiResult> getResultsByEmployee(
            String employeeId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/kpi_result_tbl"
                        + "?select=*"
                        + "&employee_id=eq."
                        + employeeId
                        + "&order=calculated_at.desc";


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
                "Unable to load KPI results"
        );


        return mapResults(
                response.body()
        );
    }


    // =========================================================
    // GET ALL RESULTS
    // =========================================================

    public List<KpiResult> getAllResults(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/kpi_result_tbl"
                        + "?select=*"
                        + "&order=calculated_at.desc";


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
                "Unable to load KPI results"
        );


        return mapResults(
                response.body()
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
    // MAP
    // =========================================================

    private List<KpiResult> mapResults(
            String body
    ) {

        JSONArray array =
                new JSONArray(body);


        List<KpiResult> results =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject json =
                    array.getJSONObject(i);


            KpiResult result =
                    new KpiResult();


            result.setResultId(
                    json.getLong(
                            "result_id"
                    )
            );


            result.setEmployeeId(
                    json.getString(
                            "employee_id"
                    )
            );


            result.setConfigId(
                    json.getLong(
                            "config_id"
                    )
            );


            result.setEvaluationStartDate(
                    LocalDate.parse(
                            json.getString(
                                    "evaluation_start_date"
                            )
                    )
            );


            result.setEvaluationEndDate(
                    LocalDate.parse(
                            json.getString(
                                    "evaluation_end_date"
                            )
                    )
            );


            result.setProductivityScore(
                    json.optDouble(
                            "productivity_score",
                            0
                    )
            );


            result.setTimelinessScore(
                    json.optDouble(
                            "timeliness_score",
                            0
                    )
            );


            result.setCompletionRate(
                    json.optDouble(
                            "completion_rate",
                            0
                    )
            );


            result.setAttendanceScore(
                    json.optDouble(
                            "attendance_score",
                            0
                    )
            );


            result.setOverallKpiScore(
                    json.optDouble(
                            "overall_kpi_score",
                            0
                    )
            );


            result.setPerformanceStatus(
                    json.optString(
                            "performance_status",
                            ""
                    )
            );


            if (
                    !json.isNull(
                            "calculated_at"
                    )
            ) {

                result.setCalculatedAt(
                        OffsetDateTime.parse(
                                json.getString(
                                        "calculated_at"
                                )
                        )
                );
            }


            results.add(
                    result
            );
        }


        return results;
    }
}