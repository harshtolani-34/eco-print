package com.example.eco_print.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.eco_print.AdminDashboardActivity;
import com.example.eco_print.CollectorApplicationStatusActivity;
import com.example.eco_print.CollectorHomeActivity;
import com.example.eco_print.HomeActivity;
import com.example.eco_print.InventoryDashboardActivity;
import com.example.eco_print.LoginActivity;

public final class RoleNavigator {

    private RoleNavigator() {
        // Prevent object creation.
    }

    public static void openCorrectHome(
            Activity activity,
            SessionManager sessionManager
    ) {
        Class<?> destination;

        if (sessionManager == null
                || !sessionManager.isLoggedIn()) {
            destination = LoginActivity.class;
        } else if (sessionManager.isAdmin()) {
            destination = AdminDashboardActivity.class;
        } else if (sessionManager.isInventoryManager()) {
            destination = InventoryDashboardActivity.class;
        } else if (sessionManager.isApprovedCollector()) {
            destination = CollectorHomeActivity.class;
        } else if (sessionManager.isCollector()) {
            destination = CollectorApplicationStatusActivity.class;
        } else {
            destination = HomeActivity.class;
        }

        Intent intent = new Intent(activity, destination);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        activity.startActivity(intent);
        activity.finish();
    }
}
