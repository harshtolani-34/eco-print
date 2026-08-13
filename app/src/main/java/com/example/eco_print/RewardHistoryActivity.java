package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.RewardHistoryAdapter;
import com.example.eco_print.api.RewardsApi;
import com.example.eco_print.models.RewardTransaction;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RewardHistoryActivity
        extends AppCompatActivity {

    private RecyclerView rewardsRecyclerView;
    private ProgressBar loadingProgress;

    private View stateContainer;
    private TextView stateText;

    private TextView totalPointsText;
    private TextView rewardCountText;

    private MaterialButton retryButton;

    private RewardHistoryAdapter adapter;
    private SessionManager sessionManager;

    private Call<List<RewardTransaction>> rewardsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_reward_history
        );

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        bindViews();

        adapter = new RewardHistoryAdapter();

        rewardsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rewardsRecyclerView.setAdapter(adapter);
        rewardsRecyclerView.setHasFixedSize(true);
        rewardsRecyclerView.setItemAnimator(null);

        ImageButton backButton =
                findViewById(R.id.backButton);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        retryButton.setOnClickListener(v ->
                loadRewards()
        );
    }

    private void bindViews() {

        rewardsRecyclerView =
                findViewById(R.id.rewardsRecyclerView);

        loadingProgress =
                findViewById(R.id.loadingProgress);

        stateContainer =
                findViewById(R.id.stateContainer);

        stateText =
                findViewById(R.id.stateText);

        totalPointsText =
                findViewById(R.id.totalPointsText);

        rewardCountText =
                findViewById(R.id.rewardCountText);

        retryButton =
                findViewById(R.id.retryButton);
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadRewards();
    }

    private void loadRewards() {

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (!SupabaseConfig.isConfigured()) {
            showError(
                    "Supabase is not configured."
            );
            return;
        }

        if (rewardsCall != null) {
            rewardsCall.cancel();
        }

        showLoading();

        RewardsApi rewardsApi =
                SupabaseClient.getClient()
                        .create(RewardsApi.class);

        rewardsCall = rewardsApi.getMyRewards(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "*",
                "created_at.desc"
        );

        rewardsCall.enqueue(
                new Callback<List<RewardTransaction>>() {

                    @Override
                    public void onResponse(
                            Call<List<RewardTransaction>> call,
                            Response<List<RewardTransaction>> response
                    ) {
                        rewardsCall = null;

                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        if (response.code() == 401) {
                            showToast(
                                    "Your session expired"
                            );

                            redirectToLogin();
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            showError(
                                    "Reward history could not be loaded. Error "
                                            + response.code()
                            );

                            return;
                        }

                        List<RewardTransaction> rewards =
                                response.body();

                        adapter.setRewards(rewards);

                        int totalPoints = 0;

                        for (RewardTransaction reward : rewards) {
                            totalPoints += reward.getPoints();
                        }

                        totalPointsText.setText(
                                String.valueOf(totalPoints)
                        );

                        int count = rewards.size();

                        rewardCountText.setText(
                                count == 1
                                        ? "1 reward transaction"
                                        : count + " reward transactions"
                        );

                        if (rewards.isEmpty()) {
                            showEmpty();
                        } else {
                            showRewards();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<RewardTransaction>> call,
                            Throwable throwable
                    ) {
                        rewardsCall = null;

                        if (call.isCanceled()
                                || isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showError(
                                "Could not connect to the server. Check your internet and try again."
                        );
                    }
                }
        );
    }

    private void showLoading() {

        loadingProgress.setVisibility(
                View.VISIBLE
        );

        rewardsRecyclerView.setVisibility(
                View.GONE
        );

        stateContainer.setVisibility(
                View.GONE
        );

        rewardCountText.setText(
                "Loading reward history..."
        );
    }

    private void showRewards() {

        loadingProgress.setVisibility(
                View.GONE
        );

        stateContainer.setVisibility(
                View.GONE
        );

        rewardsRecyclerView.setVisibility(
                View.VISIBLE
        );
    }

    private void showEmpty() {

        loadingProgress.setVisibility(
                View.GONE
        );

        rewardsRecyclerView.setVisibility(
                View.GONE
        );

        stateContainer.setVisibility(
                View.VISIBLE
        );

        retryButton.setVisibility(
                View.GONE
        );

        stateText.setText(
                "No rewards yet\n\n"
                        + "Complete a verified plastic-waste collection "
                        + "to start earning Eco Points."
        );

        totalPointsText.setText("0");

        rewardCountText.setText(
                "No reward transactions yet"
        );
    }

    private void showError(String message) {

        loadingProgress.setVisibility(
                View.GONE
        );

        rewardsRecyclerView.setVisibility(
                View.GONE
        );

        stateContainer.setVisibility(
                View.VISIBLE
        );

        retryButton.setVisibility(
                View.VISIBLE
        );

        stateText.setText(message);

        rewardCountText.setText(
                "Rewards could not be loaded"
        );
    }

    private void redirectToLogin() {

        sessionManager.logout();

        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {

        if (rewardsCall != null) {
            rewardsCall.cancel();
            rewardsCall = null;
        }

        super.onDestroy();
    }
}