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

    public void setLoggedIn(boolean loggedIn) {
        preferences.edit()
                .putBoolean(KEY_LOGIN, loggedIn)
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
