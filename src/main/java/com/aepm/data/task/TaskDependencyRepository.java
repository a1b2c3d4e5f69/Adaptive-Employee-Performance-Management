package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskDependency;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskDependencyRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // GET ALL
    // =========================================================

    public List<TaskDependency> getAllDependencies(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_dependency_tbl"
                        + "?select=*"
                        + "&order=dependency_id.asc";


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
                "Unable to load task dependencies"
        );


        return mapDependencies(
                response.body()
        );
    }


    // =========================================================
    // GET ACTIVE
    // =========================================================

    public List<TaskDependency> getActiveDependencies(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_dependency_tbl"
                        + "?select=*"
                        + "&rule_status=eq.Active"
                        + "&order=dependency_id.asc";


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
                "Unable to load active task dependencies"
        );


        return mapDependencies(
                response.body()
        );
    }


    // =========================================================
    // GET DEPENDENCY FOR NEXT TASK
    // =========================================================

    public List<TaskDependency> getDependenciesForNextTask(
            UUID nextTaskId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_dependency_tbl"
                        + "?select=*"
                        + "&next_task_id=eq."
                        + nextTaskId
                        + "&rule_status=eq.Active";


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
                "Unable to load task dependency"
        );


        return mapDependencies(
                response.body()
        );
    }


    // =========================================================
    // CHECK DUPLICATE
    // =========================================================

    public boolean dependencyExists(
            UUID prerequisiteTaskId,
            UUID nextTaskId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_dependency_tbl"
                        + "?select=dependency_id"
                        + "&prerequisite_task_id=eq."
                        + prerequisiteTaskId
                        + "&next_task_id=eq."
                        + nextTaskId
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
                send(request);


        validateResponse(
                response,
                "Unable to check task dependency"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        return !array.isEmpty();
    }


    // =========================================================
    // CREATE
    // =========================================================

    public void createDependency(
            TaskDependency dependency,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "prerequisite_task_id",
                dependency
                        .getPrerequisiteTaskId()
                        .toString()
        );


        body.put(
                "next_task_id",
                dependency
                        .getNextTaskId()
                        .toString()
        );


        body.put(
                "rule_status",
                dependency.getRuleStatus()
        );


        body.put(
                "created_by",
                dependency
                        .getCreatedBy()
                        .toString()
        );


        String url =
                AppConfig.REST_URL
                        + "/task_dependency_tbl";


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
                "Unable to create task dependency"
        );
    }


    // =========================================================
    // UPDATE STATUS
    // =========================================================

    public void updateRuleStatus(
            Long dependencyId,
            String status,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "rule_status",
                status
        );


        String url =
                AppConfig.REST_URL
                        + "/task_dependency_tbl"
                        + "?dependency_id=eq."
                        + dependencyId;


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
                "Unable to update dependency status"
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteDependency(
            Long dependencyId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_dependency_tbl"
                        + "?dependency_id=eq."
                        + dependencyId;


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
                "Unable to delete task dependency"
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


    // =========================================================
    // RESPONSE VALIDATION
    // =========================================================

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

    private List<TaskDependency> mapDependencies(
            String body
    ) {

        JSONArray array =
                new JSONArray(
                        body
                );


        List<TaskDependency> dependencies =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject json =
                    array.getJSONObject(i);


            TaskDependency dependency =
                    new TaskDependency();


            dependency.setDependencyId(
                    json.getLong(
                            "dependency_id"
                    )
            );


            dependency.setPrerequisiteTaskId(
                    UUID.fromString(
                            json.getString(
                                    "prerequisite_task_id"
                            )
                    )
            );


            dependency.setNextTaskId(
                    UUID.fromString(
                            json.getString(
                                    "next_task_id"
                            )
                    )
            );


            dependency.setRuleStatus(
                    json.optString(
                            "rule_status",
                            "Active"
                    )
            );


            if (
                    !json.isNull(
                            "created_by"
                    )
            ) {

                dependency.setCreatedBy(
                        UUID.fromString(
                                json.getString(
                                        "created_by"
                                )
                        )
                );
            }


            if (
                    !json.isNull(
                            "created_at"
                    )
            ) {

                dependency.setCreatedAt(
                        OffsetDateTime.parse(
                                json.getString(
                                        "created_at"
                                )
                        )
                );
            }


            dependencies.add(
                    dependency
            );
        }


        return dependencies;
    }
}