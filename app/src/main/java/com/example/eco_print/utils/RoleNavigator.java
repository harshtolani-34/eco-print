package com.example.eco_print.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.eco_print.CollectorHomeActivity;
import com.example.eco_print.HomeActivity;

public final class RoleNavigator {

    private RoleNavigator() {
        // Prevent object creation.
    }

    public static void openCorrectHome(
            Activity activity,
            SessionManager sessionManager
    ) {
        Class<?> destination = sessionManager.isCollector()
                ? CollectorHomeActivity.class
                : HomeActivity.class;

        Intent intent = new Intent(
                activity,
                destination
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        activity.startActivity(intent);
        activity.finish();
    }
}
