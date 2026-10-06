package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.AuthSession;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuthRepository {

    private final SupabaseClient supabaseClient;
    private final Gson gson;

    public AuthRepository() {
        supabaseClient = SupabaseClient.getInstance();
        gson = new Gson();
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public AuthSession login(
            String email,
            String password
    ) throws Exception {

        String url =
                AppConfig.AUTH_URL
                        + "/token?grant_type=password";

        JsonObject requestBody =
                new JsonObject();

        requestBody.addProperty(
                "email",
                email
        );

        requestBody.addProperty(
                "password",
                password
        );

        HttpRequest request =
                supabaseClient
                        .createRequest(url)
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        gson.toJson(requestBody)
                                )
                        )
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

            throw new Exception(
                    extractErrorMessage(
                            response.body(),
                            "Invalid email or password."
                    )
            );
        }

        JsonObject json =
                JsonParser
                        .parseString(
                                response.body()
                        )
                        .getAsJsonObject();

        if (
                !json.has("user")
                        || json.get("user").isJsonNull()
        ) {

            throw new Exception(
                    "Login response did not contain a user."
            );
        }

        JsonObject authUser =
                json.getAsJsonObject(
                        "user"
                );

        String userId =
                authUser
                        .get("id")
                        .getAsString();

        String userEmail =
                authUser.has("email")
                        && !authUser.get("email").isJsonNull()
                        ? authUser.get("email").getAsString()
                        : email;

        String accessToken =
                json.has("access_token")
                        && !json.get("access_token").isJsonNull()
                        ? json.get("access_token").getAsString()
                        : null;

        String refreshToken =
                json.has("refresh_token")
                        && !json.get("refresh_token").isJsonNull()
                        ? json.get("refresh_token").getAsString()
                        : null;

        return new AuthSession(
                accessToken,
                refreshToken,
                userId,
                userEmail
        );
    }


    // =========================================================
    // REGISTER
    // =========================================================

    public AuthSession signUp(
        String email,
        String password
    ) throws Exception {

    String url =
            AppConfig.AUTH_URL
                    + "/signup";

    JsonObject requestBody =
            new JsonObject();

    requestBody.addProperty(
            "email",
            email
    );

    requestBody.addProperty(
            "password",
            password
    );

    HttpRequest request =
            supabaseClient
                    .createRequest(url)
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    gson.toJson(requestBody)
                            )
                    )
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

        throw new Exception(
                extractErrorMessage(
                        response.body(),
                        "Unable to create account."
                )
        );
    }

    JsonObject json =
            JsonParser
                    .parseString(
                            response.body()
                    )
                    .getAsJsonObject();


    // ==========================================
    // Get User
    // ==========================================

    JsonObject authUser;

    // Normal signup with session
    if (
            json.has("user")
                    && !json.get("user").isJsonNull()
    ) {

        authUser =
                json.getAsJsonObject(
                        "user"
                );

    } else if (
            json.has("id")
                    && !json.get("id").isJsonNull()
    ) {

        // Email confirmation enabled response
        authUser =
                json;

    } else {

        throw new Exception(
                "Unable to retrieve the created user."
        );
    }


    String userId =
            authUser
                    .get("id")
                    .getAsString();


    String userEmail =
            authUser.has("email")
                    && !authUser.get("email").isJsonNull()

                    ? authUser
                    .get("email")
                    .getAsString()

                    : email;


    // ==========================================
    // Get Session
    // ==========================================

    String accessToken = null;

    String refreshToken = null;


    if (
            json.has("access_token")
                    && !json.get("access_token").isJsonNull()
    ) {

        accessToken =
                json.get(
                        "access_token"
                ).getAsString();
    }


    if (
            json.has("refresh_token")
                    && !json.get("refresh_token").isJsonNull()
    ) {

        refreshToken =
                json.get(
                        "refresh_token"
                ).getAsString();
    }


    return new AuthSession(
            accessToken,
            refreshToken,
            userId,
            userEmail
    );
}

    // =========================================================
    // ERROR MESSAGE HELPER
    // =========================================================

    private String extractErrorMessage(
            String responseBody,
            String defaultMessage
    ) {

        try {

            JsonObject json =
                    JsonParser
                            .parseString(
                                    responseBody
                            )
                            .getAsJsonObject();

            if (
                    json.has("msg")
                            && !json.get("msg").isJsonNull()
            ) {

                return json
                        .get("msg")
                        .getAsString();
            }

            if (
                    json.has("message")
                            && !json.get("message").isJsonNull()
            ) {

                return json
                        .get("message")
                        .getAsString();
            }

            if (
                    json.has("error_description")
                            && !json.get("error_description").isJsonNull()
            ) {

                return json
                        .get("error_description")
                        .getAsString();
            }

            if (
                    json.has("error")
                            && !json.get("error").isJsonNull()
            ) {

                return json
                        .get("error")
                        .getAsString();
            }

        } catch (Exception ignored) {
        }

        return defaultMessage;
    }

    public void updatePassword(
        String accessToken,
        String newPassword
    ) throws Exception {

    String url =
            AppConfig.AUTH_URL
                    + "/user";


    JsonObject body =
            new JsonObject();

    body.addProperty(
            "password",
            newPassword
    );


    HttpRequest request =
            supabaseClient
                    .createAuthenticatedRequest(
                            url,
                            accessToken
                    )
                    .method(
                            "PUT",
                            HttpRequest.BodyPublishers.ofString(
                                    gson.toJson(body)
                            )
                    )
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

        throw new Exception(
                extractErrorMessage(
                        response.body(),
                        "Unable to change password."
                )
        );
    }
    }


}