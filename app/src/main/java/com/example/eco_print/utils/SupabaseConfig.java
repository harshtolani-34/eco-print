package com.example.eco_print.utils;

public final class SupabaseConfig {

    private SupabaseConfig() {
        // Configuration holder.
    }

    public static final String SUPABASE_URL =
            "https://mgjikkjowdugemmmwjpz.supabase.co/";

    /**
     * Paste the project's Supabase anon key here in the local project.
     * Do not commit service-role keys to an Android application.
     */
    public static final String SUPABASE_ANON_KEY = "";

    public static final String WASTE_IMAGE_BUCKET =
            "waste-report-images";

    public static boolean isConfigured() {
        return SUPABASE_URL.startsWith("https://")
                && SUPABASE_URL.endsWith("/")
                && !SUPABASE_ANON_KEY.trim().isEmpty()
                && !WASTE_IMAGE_BUCKET.trim().isEmpty();
    }
}
