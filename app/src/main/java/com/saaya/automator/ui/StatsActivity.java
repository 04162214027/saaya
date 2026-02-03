package com.saaya.automator.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.saaya.automator.R;
import com.saaya.automator.data.SaayaMemoryDB;

import java.util.List;
import java.util.Map;

/**
 * StatsActivity - Dashboard with Analytics
 */
public class StatsActivity extends AppCompatActivity {

    private static final String TAG = "StatsActivity";
    private TextView tvTotalMessages, tvWritingStyle, tvPeakTime, tvFavApp;
    private RecyclerView logsRecyclerView;
    private LogsAdapter logsAdapter;
    private SaayaMemoryDB memoryDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            setContentView(R.layout.activity_stats);
            Log.d(TAG, "setContentView successful");

            // Set title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Dashboard");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Initialize database
            memoryDB = SaayaMemoryDB.getInstance(this);
            Log.d(TAG, "Database initialized");

            // Initialize views
            tvTotalMessages = findViewById(R.id.tvTotalMessages);
            tvWritingStyle = findViewById(R.id.tvWritingStyle);
            tvPeakTime = findViewById(R.id.tvPeakTime);
            tvFavApp = findViewById(R.id.tvFavApp);
            logsRecyclerView = findViewById(R.id.logsRecyclerView);
            
            Log.d(TAG, "Views found - TotalMsg: " + (tvTotalMessages != null) + 
                       ", Style: " + (tvWritingStyle != null) +
                       ", Peak: " + (tvPeakTime != null) +
                       ", Fav: " + (tvFavApp != null) +
                       ", RecyclerView: " + (logsRecyclerView != null));

            // Null checks for all views
            if (tvTotalMessages == null || tvWritingStyle == null || 
                tvPeakTime == null || tvFavApp == null || logsRecyclerView == null) {
                Log.e(TAG, "Error: One or more views not found");
                Toast.makeText(this, "Error loading dashboard", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Setup RecyclerView
            logsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Load data
            loadAnalytics();
            loadLogs();
            
            Log.d(TAG, "onCreate completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "FATAL ERROR in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadAnalytics() {
        // Run on background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Map<String, String> profile = memoryDB.getPersonalityProfile();
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvTotalMessages.setText("Total Messages: " + profile.get("totalMessages"));
                        tvWritingStyle.setText("Writing Style: " + profile.get("writingStyle"));
                        tvPeakTime.setText("Peak Time: " + profile.get("peakTime"));
                        tvFavApp.setText("Favorite App: " + profile.get("favApp"));
                    }
                });
            }
        }).start();
    }

    private void loadLogs() {
        // Run on background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<SaayaMemoryDB.LogEntry> logs = memoryDB.getAllLogs();
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logsAdapter = new LogsAdapter(logs);
                        logsRecyclerView.setAdapter(logsAdapter);
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
