package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskCategory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

public class TaskCategoryRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // GET ALL CATEGORIES
    // Admin use
    // =========================================================

    public List<TaskCategory> getAllCategories(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_category_tbl"
                        + "?select=*"
                        + "&order=category_name.asc";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to load task categories"
        );


        return mapCategories(
                response.body()
        );
    }


    // =========================================================
    // GET ACTIVE CATEGORIES
    // Manager / Employee use
    // =========================================================

    public List<TaskCategory> getActiveCategories(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_category_tbl"
                        + "?select=*"
                        + "&category_status=eq.Active"
                        + "&order=category_name.asc";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to load active task categories"
        );


        return mapCategories(
                response.body()
        );
    }


    // =========================================================
    // CHECK DUPLICATE CATEGORY NAME
    // =========================================================

    public boolean categoryNameExists(
            String categoryName,
            String accessToken
    ) throws Exception {

        String encodedName =
                URLEncoder.encode(
                        categoryName,
                        StandardCharsets.UTF_8
                );


        String url =
                AppConfig.REST_URL
                        + "/task_category_tbl"
                        + "?select=category_id"
                        + "&category_name=eq."
                        + encodedName;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to validate category name"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        return !array.isEmpty();
    }


    // =========================================================
    // CREATE CATEGORY
    // =========================================================

    public void createCategory(
            TaskCategory category,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "category_name",
                category.getCategoryName()
        );


        if (
                category.getDescription() == null
                        || category.getDescription().isBlank()
        ) {

            body.put(
                    "description",
                    JSONObject.NULL
            );

        } else {

            body.put(
                    "description",
                    category.getDescription()
            );
        }


        body.put(
                "category_status",
                category.getCategoryStatus()
        );


        if (
                category.getCreatedBy() != null
                        && !category.getCreatedBy().isBlank()
        ) {

            body.put(
                    "created_by",
                    category.getCreatedBy()
            );
        }


        String url =
                AppConfig.REST_URL
                        + "/task_category_tbl";


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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to create task category"
        );
    }


    // =========================================================
    // UPDATE CATEGORY
    // =========================================================

    public void updateCategory(
            TaskCategory category,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "category_name",
                category.getCategoryName()
        );


        if (
                category.getDescription() == null
                        || category.getDescription().isBlank()
        ) {

            body.put(
                    "description",
                    JSONObject.NULL
            );

        } else {

            body.put(
                    "description",
                    category.getDescription()
            );
        }


        body.put(
                "category_status",
                category.getCategoryStatus()
        );


        body.put(
                "updated_at",
                java.time.OffsetDateTime
                        .now()
                        .toString()
        );


        String url =
                AppConfig.REST_URL
                        + "/task_category_tbl"
                        + "?category_id=eq."
                        + category.getCategoryId();


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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to update task category"
        );
    }


    // =========================================================
    // UPDATE STATUS
    // =========================================================

    public void updateCategoryStatus(
            Long categoryId,
            String newStatus,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "category_status",
                newStatus
        );


        body.put(
                "updated_at",
                java.time.OffsetDateTime
                        .now()
                        .toString()
        );


        String url =
                AppConfig.REST_URL
                        + "/task_category_tbl"
                        + "?category_id=eq."
                        + categoryId;


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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to update category status"
        );
    }


    // =========================================================
    // CHECK CATEGORY USED BY TASK
    // =========================================================

    public boolean isCategoryUsed(
            Long categoryId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?select=task_id"
                        + "&category_id=eq."
                        + categoryId
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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to check category usage"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        return !array.isEmpty();
    }


    // =========================================================
    // DELETE CATEGORY
    // =========================================================

    public void deleteCategory(
            Long categoryId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_category_tbl"
                        + "?category_id=eq."
                        + categoryId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .DELETE()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to delete task category"
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
    // VALIDATE RESPONSE
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

    private List<TaskCategory> mapCategories(
            String responseBody
    ) {

        JSONArray array =
                new JSONArray(
                        responseBody
                );


        List<TaskCategory> categories =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject json =
                    array.getJSONObject(i);


            TaskCategory category =
                    new TaskCategory();


            category.setCategoryId(
                    json.getLong(
                            "category_id"
                    )
            );


            category.setCategoryName(
                    json.optString(
                            "category_name",
                            ""
                    )
            );


            category.setDescription(
                    json.isNull(
                            "description"
                    )
                            ? null
                            : json.optString(
                                    "description",
                                    null
                            )
            );


            category.setCategoryStatus(
                    json.optString(
                            "category_status",
                            "Active"
                    )
            );


            category.setCreatedBy(
                    json.isNull(
                            "created_by"
                    )
                            ? null
                            : json.optString(
                                    "created_by",
                                    null
                            )
            );


            categories.add(
                    category
            );
        }


        return categories;
    }
}