package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskChatRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    public void sendMessage(
            UUID taskId,
            UUID senderId,
            String text,
            String accessToken
    ) throws Exception {

        if (
                text == null
                        || text.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Message cannot be empty."
            );
        }


        JSONObject body =
                new JSONObject();

        body.put(
                "task_id",
                taskId.toString()
        );

        body.put(
                "sender_id",
                senderId.toString()
        );

        body.put(
                "message_text",
                text.trim()
        );


        String url =
                AppConfig.REST_URL
                        + "/task_chat_message_tbl";


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
                                HttpRequest.BodyPublishers.ofString(
                                        body.toString()
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


        validate(
                response,
                "Unable to send message"
        );
    }


    public List<TaskChatMessage> getMessages(
            UUID taskId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_chat_message_tbl"
                        + "?select=*,user_tbl!task_chat_message_tbl_sender_id_fkey("
                        + "user_id,username,full_name,employee_id)"
                        + "&task_id=eq."
                        + taskId
                        + "&is_deleted=eq.false"
                        + "&order=created_at.asc";


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
                response,
                "Unable to load chat messages"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<TaskChatMessage> messages =
                new ArrayList<>();


        for (int i = 0; i < array.length(); i++) {

            JSONObject json =
                    array.getJSONObject(i);

            TaskChatMessage message =
                    new TaskChatMessage();

            message.setMessageId(
                    UUID.fromString(
                            json.getString(
                                    "message_id"
                            )
                    )
            );

            message.setTaskId(
                    UUID.fromString(
                            json.getString(
                                    "task_id"
                            )
                    )
            );

            message.setSenderId(
                    UUID.fromString(
                            json.getString(
                                    "sender_id"
                            )
                    )
            );

            message.setMessageText(
                    json.optString(
                            "message_text",
                            ""
                    )
            );

            message.setDeleted(
                    json.optBoolean(
                            "is_deleted",
                            false
                    )
            );


            String created =
                    json.optString(
                            "created_at",
                            null
                    );

            if (
                    created != null
                            && !created.isBlank()
            ) {
                message.setCreatedAt(
                        OffsetDateTime.parse(
                                created
                        )
                );
            }


            JSONObject sender =
                    json.optJSONObject(
                            "user_tbl"
                    );

            if (sender != null) {

                String name =
                        sender.optString(
                                "full_name",
                                ""
                        );

                if (name.isBlank()) {
                    name =
                            sender.optString(
                                    "username",
                                    "Employee"
                            );
                }

                message.setSenderName(
                        name
                );
            }


            messages.add(
                    message
            );
        }


        return messages;
    }


    private void validate(
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
}