package com.example.eco_print.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CollectorApplicationManager {

    private static final String PREF_NAME =
            "EcoPrintPendingCollector";

    private static final String KEY_NAME = "name";
    private static final String KEY_AGE = "age";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_COMPANY_CODE = "companyCode";

    private final SharedPreferences preferences;

    public CollectorApplicationManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveApplication(
            String name,
            int age,
            String email,
            String companyCode
    ) {
        preferences.edit()
                .putString(KEY_NAME, name)
                .putInt(KEY_AGE, age)
                .putString(KEY_EMAIL, email.toLowerCase())
                .putString(KEY_COMPANY_CODE, companyCode)
                .apply();
    }

    public boolean matchesEmail(String email) {
        String savedEmail = preferences.getString(KEY_EMAIL, "");

        return email != null
                && !savedEmail.isEmpty()
                && savedEmail.equalsIgnoreCase(email.trim());
    }

    public String getName() {
        return preferences.getString(KEY_NAME, "Collector");
    }

    public int getAge() {
        return preferences.getInt(KEY_AGE, 18);
    }

    public String getCompanyCode() {
        return preferences.getString(KEY_COMPANY_CODE, "");
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
