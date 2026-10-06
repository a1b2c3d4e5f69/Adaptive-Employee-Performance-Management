package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskCategory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

public class TaskCategoryRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // GET ACTIVE CATEGORIES
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
                supabaseClient
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );


        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    "Unable to load task categories: "
                            + response.body()
            );
        }


        JSONArray array =
                new JSONArray(
                        response.body()
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
                    json.isNull("description")
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


            categories.add(
                    category
            );
        }


        return categories;
    }
}