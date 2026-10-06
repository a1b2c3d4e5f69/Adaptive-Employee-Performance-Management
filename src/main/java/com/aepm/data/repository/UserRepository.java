package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.Role;
import com.aepm.model.User;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;

public class UserRepository {

    private final SupabaseClient supabaseClient;


    public UserRepository() {

        supabaseClient =
                SupabaseClient.getInstance();
    }


    // =========================================================
    // FIND USER
    // =========================================================

    public User findUserById(
            String userId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?user_id=eq."
                        + encode(userId)
                        + "&select=*";


        HttpResponse<String> response =
                sendGet(
                        url,
                        accessToken
                );


        checkResponse(
                response,
                "Unable to retrieve user profile."
        );


        JsonArray array =
                JsonParser
                        .parseString(
                                response.body()
                        )
                        .getAsJsonArray();


        if (array.isEmpty()) {

            throw new Exception(
                    "User profile does not exist."
            );
        }


        return mapUser(
                array
                        .get(0)
                        .getAsJsonObject(),
                accessToken
        );
    }


    // =========================================================
    // GET ALL USERS
    // =========================================================

    public List<User> getAllUsers(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?select=*"
                        + "&order=created_at.desc";


        HttpResponse<String> response =
                sendGet(
                        url,
                        accessToken
                );


        System.out.println(
                "GET ALL USERS STATUS: "
                        + response.statusCode()
        );

        System.out.println(
                "GET ALL USERS BODY: "
                        + response.body()
        );


        checkResponse(
                response,
                "Unable to retrieve users."
        );


        JsonArray array =
                JsonParser
                        .parseString(
                                response.body()
                        )
                        .getAsJsonArray();


        List<User> userList =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.size();
                i++
        ) {

            User user =
                    mapUser(
                            array
                                    .get(i)
                                    .getAsJsonObject(),
                            accessToken
                    );

            userList.add(
                    user
            );
        }


        return userList;
    }


    // =========================================================
    // ROLE ID BY NAME
    // =========================================================

    public long findRoleIdByName(
            String roleName,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/role_tbl"
                        + "?role_name=eq."
                        + encode(roleName)
                        + "&select=role_id";


        HttpResponse<String> response =
                sendGet(
                        url,
                        accessToken
                );


        checkResponse(
                response,
                "Unable to retrieve role."
        );


        JsonArray array =
                JsonParser
                        .parseString(
                                response.body()
                        )
                        .getAsJsonArray();


        if (array.isEmpty()) {

            throw new Exception(
                    "Role does not exist."
            );
        }


        return array
                .get(0)
                .getAsJsonObject()
                .get("role_id")
                .getAsLong();
    }


    // =========================================================
    // CREATE EMPLOYEE PROFILE
    // =========================================================

    public void createEmployeeProfile(
            String userId,
            String employeeId,
            String fullName,
            String username,
            String email,
            String department,
            long roleId,
            String accessToken
    ) throws Exception {

        JsonObject json =
                new JsonObject();


        json.addProperty(
                "user_id",
                userId
        );

        json.addProperty(
                "employee_id",
                employeeId
        );

        json.addProperty(
                "full_name",
                fullName
        );

        json.addProperty(
                "username",
                username
        );

        json.addProperty(
                "email",
                email
        );

        json.addProperty(
                "department",
                department
        );

        json.addProperty(
                "role_id",
                roleId
        );

        json.addProperty(
                "account_status",
                "Active"
        );


        String url =
                AppConfig.REST_URL
                        + "/user_tbl";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .header(
                                "Prefer",
                                "return=minimal"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                json.toString()
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


        checkResponse(
                response,
                "Unable to create employee profile."
        );
    }


    // =========================================================
    // UPDATE CONTACT
    // =========================================================

    public void updateContactNumber(
            String userId,
            String contactNumber,
            String accessToken
    ) throws Exception {

        JsonObject json =
                new JsonObject();


        json.addProperty(
                "contact_number",
                contactNumber
        );


        patchUser(
                userId,
                json,
                accessToken
        );
    }


    // =========================================================
    // UPDATE USER
    // =========================================================

    public void updateUser(
            User user,
            String accessToken
    ) throws Exception {

        JsonObject json =
                new JsonObject();


        json.addProperty(
                "full_name",
                user.getFullName()
        );

        json.addProperty(
                "username",
                user.getUsername()
        );

        json.addProperty(
                "contact_number",
                user.getContactNumber()
        );

        json.addProperty(
                "department",
                user.getDepartment()
        );

        json.addProperty(
                "role_id",
                user.getRoleId()
        );


        patchUser(
                user.getUserId(),
                json,
                accessToken
        );
    }


    // =========================================================
    // ACCOUNT STATUS
    // =========================================================

    public void updateAccountStatus(
            String userId,
            String status,
            String accessToken
    ) throws Exception {

        JsonObject json =
                new JsonObject();


        json.addProperty(
                "account_status",
                status
        );


        patchUser(
                userId,
                json,
                accessToken
        );
    }


    // =========================================================
    // ADMIN CREATE USER
    // =========================================================

    public void createUserByAdmin(
            String fullName,
            String employeeId,
            String department,
            String username,
            String email,
            String password,
            String role,
            String accessToken
    ) throws Exception {

        JsonObject json =
                new JsonObject();


        json.addProperty(
                "fullName",
                fullName
        );

        json.addProperty(
                "employeeId",
                employeeId
        );

        json.addProperty(
                "department",
                department
        );

        json.addProperty(
                "username",
                username
        );

        json.addProperty(
                "email",
                email
        );

        json.addProperty(
                "password",
                password
        );

        json.addProperty(
                "role",
                role
        );


        callEdgeFunction(
                "admin-create-user",
                json,
                accessToken,
                "Unable to create user."
        );
    }


    // =========================================================
    // ADMIN DELETE USER
    // =========================================================

   // =========================================================
// ADMIN DELETE USER
// =========================================================

public void deleteUserByAdmin(
        String userId,
        String accessToken
) throws Exception {

    String url =
            AppConfig.FUNCTIONS_URL
                    + "/admin-delete-user";


    JsonObject body =
            new JsonObject();


    body.addProperty(
            "userId",
            userId
    );


    HttpRequest request =
            supabaseClient
                    .createAuthenticatedRequest(
                            url,
                            accessToken
                    )
                    .POST(
                            HttpRequest.BodyPublishers
                                    .ofString(
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


    String message =
            "Unable to delete user.";


    try {

        JsonObject json =
                JsonParser
                        .parseString(
                                response.body()
                        )
                        .getAsJsonObject();


        if (
                json.has("message")
                        && !json
                        .get("message")
                        .isJsonNull()
        ) {

            message =
                    json
                            .get("message")
                            .getAsString();
        }

    } catch (Exception ignored) {
    }


    if (
            response.statusCode() < 200
                    || response.statusCode() >= 300
    ) {

        throw new Exception(
                message
        );
    }
}


    // =========================================================
    // PATCH USER
    // =========================================================

    private void patchUser(
            String userId,
            JsonObject json,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?user_id=eq."
                        + encode(userId);


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
                                                json.toString()
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


        checkResponse(
                response,
                "Unable to update user."
        );
    }


    // =========================================================
    // EDGE FUNCTION
    // =========================================================

    private void callEdgeFunction(
            String functionName,
            JsonObject body,
            String accessToken,
            String defaultMessage
    ) throws Exception {

        String url =
                AppConfig.FUNCTIONS_URL
                        + "/"
                        + functionName;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
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


        if (
                response.statusCode() >= 200
                        && response.statusCode() < 300
        ) {

            return;
        }


        String message =
                defaultMessage;


        try {

            JsonObject error =
                    JsonParser
                            .parseString(
                                    response.body()
                            )
                            .getAsJsonObject();


            if (
                    error.has("message")
                            && !error
                            .get("message")
                            .isJsonNull()
            ) {

                message =
                        error
                                .get("message")
                                .getAsString();
            }

        } catch (Exception ignored) {
        }


        throw new Exception(
                message
        );
    }


    // =========================================================
    // MAP USER
    // =========================================================

    private User mapUser(
            JsonObject json,
            String accessToken
    ) throws Exception {

        User user =
                new User();


        user.setUserId(
                getString(
                        json,
                        "user_id"
                )
        );

        user.setEmployeeId(
                getString(
                        json,
                        "employee_id"
                )
        );

        user.setFullName(
                getString(
                        json,
                        "full_name"
                )
        );

        user.setUsername(
                getString(
                        json,
                        "username"
                )
        );

        user.setEmail(
                getString(
                        json,
                        "email"
                )
        );

        user.setContactNumber(
                getString(
                        json,
                        "contact_number"
                )
        );

        user.setDepartment(
                getString(
                        json,
                        "department"
                )
        );

        user.setManagerId(
                getString(
                        json,
                        "manager_id"
                )
        );

        user.setAccountStatus(
                getString(
                        json,
                        "account_status"
                )
        );

        user.setProfileImage(
                getString(
                        json,
                        "profile_image"
                )
        );


        if (
                json.has(
                        "role_id"
                )
                        && !json
                        .get("role_id")
                        .isJsonNull()
        ) {

            long roleId =
                    json
                            .get("role_id")
                            .getAsLong();


            user.setRoleId(
                    roleId
            );


            user.setRole(
                    findRoleById(
                            roleId,
                            accessToken
                    )
            );
        }


        return user;
    }


    // =========================================================
    // FIND ROLE
    // =========================================================

    private Role findRoleById(
            long roleId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/role_tbl"
                        + "?role_id=eq."
                        + roleId
                        + "&select=role_name";


        HttpResponse<String> response =
                sendGet(
                        url,
                        accessToken
                );


        checkResponse(
                response,
                "Unable to retrieve user role."
        );


        JsonArray array =
                JsonParser
                        .parseString(
                                response.body()
                        )
                        .getAsJsonArray();


        if (array.isEmpty()) {
            return null;
        }


        String roleName =
                getString(
                        array
                                .get(0)
                                .getAsJsonObject(),
                        "role_name"
                );


        return Role.fromDatabaseValue(
                roleName
        );
    }


    // =========================================================
    // HTTP HELPERS
    // =========================================================

    private HttpResponse<String> sendGet(
            String url,
            String accessToken
    ) throws Exception {

        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        return supabaseClient
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
    }


    private void checkResponse(
            HttpResponse<String> response,
            String defaultMessage
    ) throws Exception {

        if (
                response.statusCode() >= 200
                        && response.statusCode() < 300
        ) {

            return;
        }


        System.out.println(
                "SUPABASE STATUS: "
                        + response.statusCode()
        );

        System.out.println(
                "SUPABASE RESPONSE: "
                        + response.body()
        );


        throw new Exception(
                defaultMessage
        );
    }


    private String encode(
            String value
    ) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }


    private String getString(
            JsonObject object,
            String property
    ) {

        if (
                object.has(property)
                        && !object
                        .get(property)
                        .isJsonNull()
        ) {

            return object
                    .get(property)
                    .getAsString();
        }


        return null;
    }

   // =========================================================
// UPLOAD PROFILE IMAGE
// =========================================================

public String uploadProfileImage(
        String userId,
        Path imagePath,
        String accessToken
) throws Exception {

    if (imagePath == null) {

        throw new Exception(
                "Please select an image."
        );
    }


    byte[] imageBytes =
            Files.readAllBytes(
                    imagePath
            );


    String fileName =
            imagePath
                    .getFileName()
                    .toString()
                    .toLowerCase();


    String contentType;


    if (
            fileName.endsWith(".png")
    ) {

        contentType =
                "image/png";

    } else if (
            fileName.endsWith(".jpg")
                    || fileName.endsWith(".jpeg")
    ) {

        contentType =
                "image/jpeg";

    } else {

        throw new Exception(
                "Only JPG, JPEG and PNG images are supported."
        );
    }


    // =========================================
    // OBJECT PATH
    // =========================================

    String extension =
            contentType.equals("image/png")
                    ? ".png"
                    : ".jpg";


    String objectPath =
            userId
                    + "/profile"
                    + extension;


    String uploadUrl =
            AppConfig.STORAGE_URL
                    + "/object/profile-images/"
                    + objectPath;


    // =========================================
    // IMPORTANT:
    // Do NOT use createAuthenticatedRequest()
    // because that method adds application/json.
    // =========================================

    HttpRequest request =
            HttpRequest
                    .newBuilder()
                    .uri(
                            java.net.URI.create(
                                    uploadUrl
                            )
                    )

                    .header(
                            "apikey",
                            AppConfig.SUPABASE_PUBLISHABLE_KEY
                    )

                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )

                    .header(
                            "Content-Type",
                            contentType
                    )

                    .header(
                            "x-upsert",
                            "true"
                    )

                    .POST(
                            HttpRequest
                                    .BodyPublishers
                                    .ofByteArray(
                                            imageBytes
                                    )
                    )

                    .build();


    HttpResponse<String> response =
            supabaseClient
                    .getHttpClient()
                    .send(
                            request,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );


    System.out.println(
            "PROFILE IMAGE STATUS: "
                    + response.statusCode()
    );


    System.out.println(
            "PROFILE IMAGE RESPONSE: "
                    + response.body()
    );


    if (
            response.statusCode() < 200
                    || response.statusCode() >= 300
    ) {

        throw new Exception(
                "Unable to upload profile image."
        );
    }


    // =========================================
    // PUBLIC URL
    // =========================================

    return AppConfig.STORAGE_URL
            + "/object/public/profile-images/"
            + objectPath;
}

// =========================================================
// UPDATE PROFILE IMAGE URL
// =========================================================

public void updateProfileImage(
        String userId,
        String profileImageUrl,
        String accessToken
) throws Exception {

    JsonObject json =
            new JsonObject();


    json.addProperty(
            "profile_image",
            profileImageUrl
    );


    String url =
            AppConfig.REST_URL
                    + "/user_tbl"
                    + "?user_id=eq."
                    + encode(userId);


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
                                            json.toString()
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
                "Unable to save profile image."
        );
    }
}


    
}