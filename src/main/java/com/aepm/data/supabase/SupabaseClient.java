package com.aepm.data.supabase;

import com.aepm.config.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class SupabaseClient {

    private static SupabaseClient instance;

    private final HttpClient httpClient;


    private SupabaseClient() {

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }


    public static SupabaseClient getInstance() {

        if (instance == null) {

            instance = new SupabaseClient();
        }

        return instance;
    }


    public HttpClient getHttpClient() {

        return httpClient;
    }


    public HttpRequest.Builder createRequest(
            String url
    ) {

        return HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .header(
                        "apikey",
                        AppConfig.SUPABASE_PUBLISHABLE_KEY
                )
                .header(
                        "Content-Type",
                        "application/json"
                );
    }


    public HttpRequest.Builder createAuthenticatedRequest(
            String url,
            String accessToken
    ) {

        return createRequest(url)
                .header(
                        "Authorization",
                        "Bearer " + accessToken
                );
    }
}