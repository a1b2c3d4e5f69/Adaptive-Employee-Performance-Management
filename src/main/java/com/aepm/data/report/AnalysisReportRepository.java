package com.aepm.data.report;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.EmployeeAnalysisResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

public class AnalysisReportRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    public List<EmployeeAnalysisResult> getAll(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/aepm_employee_analysis_v"
                        + "?select=*"
                        + "&order=employee_id.asc";


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
                response
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<EmployeeAnalysisResult> result =
                new ArrayList<>();


        for (int i = 0; i < array.length(); i++) {

            JSONObject json =
                    array.getJSONObject(i);

            EmployeeAnalysisResult row =
                    new EmployeeAnalysisResult();

            row.setEmployeeId(
                    json.optString(
                            "employee_id",
                            ""
                    )
            );

            row.setEmployeeName(
                    json.optString(
                            "employee_name",
                            ""
                    )
            );

            row.setDepartment(
                    json.optString(
                            "department",
                            ""
                    )
            );

            row.setProductivity(
                    json.optDouble(
                            "productivity",
                            0
                    )
            );

            row.setTimeliness(
                    json.optDouble(
                            "timeliness",
                            0
                    )
            );

            row.setCompletion(
                    json.optDouble(
                            "completion",
                            0
                    )
            );

            row.setAttendance(
                    json.optDouble(
                            "attendance",
                            0
                    )
            );

            row.setOverallKpi(
                    json.optDouble(
                            "overall_kpi",
                            0
                    )
            );

            row.setTotalTasks(
                    json.optInt(
                            "total_tasks",
                            0
                    )
            );

            row.setCompletedTasks(
                    json.optInt(
                            "completed_tasks",
                            0
                    )
            );

            row.setOverdueTasks(
                    json.optInt(
                            "overdue_tasks",
                            0
                    )
            );


            result.add(
                    row
            );
        }


        return result;
    }


    private void validate(
            HttpResponse<String> response
    ) {

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    "Unable to load analysis report: "
                            + response.body()
            );
        }
    }
}