package com.example.eco_print.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "EcoPrintSession";
    private static final String KEY_LOGIN = "isLoggedIn";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void setLoggedIn(boolean loggedIn) {

        preferences.edit()
                .putBoolean(KEY_LOGIN, loggedIn)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGIN, false);
    }

    public void logout() {

        preferences.edit()
                .clear()
                .apply();
    }
}