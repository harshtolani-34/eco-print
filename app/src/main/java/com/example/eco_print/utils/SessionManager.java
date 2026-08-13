package com.example.eco_print.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "EcoPrintSession";

    private static final String KEY_LOGIN = "isLoggedIn";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_COLLECTOR_STATUS = "collectorStatus";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveSession(
            String accessToken,
            String refreshToken,
            String userId,
            String userEmail
    ) {
        preferences.edit()
                .putBoolean(KEY_LOGIN, true)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, userEmail)
                .apply();
    }

    public void saveProfile(
            String role,
            String userName,
            String collectorStatus
    ) {
        preferences.edit()
                .putString(
                        KEY_USER_ROLE,
                        role == null ? "citizen" : role
                )
                .putString(
                        KEY_USER_NAME,
                        userName == null ? "" : userName
                )
                .putString(
                        KEY_COLLECTOR_STATUS,
                        collectorStatus == null
                                ? "not_applicable"
                                : collectorStatus
                )
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGIN, false)
                && !getAccessToken().isEmpty()
                && !getUserId().isEmpty();
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, "");
    }

    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, "");
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, "");
    }

    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return preferences.getString(KEY_USER_ROLE, "citizen");
    }

    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, "");
    }

    public String getCollectorStatus() {
        return preferences.getString(
                KEY_COLLECTOR_STATUS,
                "not_applicable"
        );
    }

    public boolean isCollector() {
        return "collector".equalsIgnoreCase(getUserRole());
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getUserRole());
    }

    public boolean isApprovedCollector() {
        return isCollector()
                && "approved".equalsIgnoreCase(
                getCollectorStatus()
        );
    }

    public String getAuthorizationHeader() {
        String token = getAccessToken();

        if (token.isEmpty()) {
            return "";
        }

        return "Bearer " + token;
    }

    public void logout() {
        preferences.edit()
                .clear()
                .apply();
    }
}
