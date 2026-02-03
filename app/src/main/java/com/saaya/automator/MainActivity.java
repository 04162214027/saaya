package com.saaya.automator;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.saaya.automator.core.SaayaService;
import com.saaya.automator.data.SaayaMemoryDB;

/**
 * MainActivity - Control Center for Saaya
 * Manages permissions and displays service status
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    private TextView statusText;
    private TextView patternCountText;
    private Button btnAccessibility;
    private Button btnOverlay;
    private Button btnUsageStats;
    private MaterialCardView statusCard;
    
    private SaayaMemoryDB memoryDB;
    private ServiceStatusReceiver statusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database
        memoryDB = SaayaMemoryDB.getInstance(this);

        // Initialize views
        initViews();

        // Register broadcast receiver for service status
        statusReceiver = new ServiceStatusReceiver();
        IntentFilter filter = new IntentFilter("com.saaya.automator.SERVICE_STATUS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }

        // Check permissions and update UI
        checkPermissions();
    }

    private void initViews() {
        statusText = findViewById(R.id.statusText);
        patternCountText = findViewById(R.id.patternCountText);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnOverlay = findViewById(R.id.btnOverlay);
        btnUsageStats = findViewById(R.id.btnUsageStats);
        statusCard = findViewById(R.id.statusCard);

        // Set button click listeners
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnOverlay.setOnClickListener(v -> requestOverlayPermission());
        btnUsageStats.setOnClickListener(v -> openUsageStatsSettings());

        // Refresh button
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            checkPermissions();
            Toast.makeText(this, "Status refreshed", Toast.LENGTH_SHORT).show();
        });

        // Clear patterns button
        findViewById(R.id.btnClearPatterns).setOnClickListener(v -> {
            memoryDB.clearAllPatterns();
            updatePatternCount();
            Toast.makeText(this, "All patterns cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkPermissions() {
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        boolean overlayEnabled = canDrawOverlays();
        boolean usageStatsEnabled = hasUsageStatsPermission();

        // Update button states
        updateButtonState(btnAccessibility, accessibilityEnabled);
        updateButtonState(btnOverlay, overlayEnabled);
        updateButtonState(btnUsageStats, usageStatsEnabled);

        // Update overall status
        if (accessibilityEnabled && overlayEnabled && usageStatsEnabled) {
            updateStatus("Shadow is Active", true);
        } else {
            updateStatus("Permissions Required", false);
        }

        // Update pattern count
        updatePatternCount();
    }

    private void updateButtonState(Button button, boolean granted) {
        if (granted) {
            button.setText("✓ Granted");
            button.setEnabled(false);
            button.setBackgroundColor(getColor(R.color.success_green));
        } else {
            button.setText("Grant Access");
            button.setEnabled(true);
            button.setBackgroundColor(getColor(R.color.primary));
        }
    }

    private void updateStatus(String status, boolean active) {
        statusText.setText(status);
        if (active) {
            statusCard.setCardBackgroundColor(getColor(R.color.success_green_dark));
        } else {
            statusCard.setCardBackgroundColor(getColor(R.color.warning_orange));
        }
    }

    private void updatePatternCount() {
        int count = memoryDB.getTotalPatternCount();
        patternCountText.setText("Learned Patterns: " + count);
    }

    /**
     * Check if Accessibility Service is enabled
     */
    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + SaayaService.class.getCanonicalName();
        
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (settingValue != null) {
                return settingValue.contains(service);
            }
        }

        return false;
    }

    /**
     * Check if overlay permission is granted
     */
    private boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    /**
     * Check if usage stats permission is granted
     */
    private boolean hasUsageStatsPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName()
            );
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Open Accessibility Settings
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Enable 'Saaya' in Accessibility", Toast.LENGTH_LONG).show();
    }

    /**
     * Request Overlay Permission
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            Toast.makeText(this, "Enable 'Display over other apps'", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Open Usage Stats Settings
     */
    private void openUsageStatsSettings() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Enable 'Saaya' in Usage Access", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusReceiver != null) {
            unregisterReceiver(statusReceiver);
        }
    }

    /**
     * BroadcastReceiver to listen for service status changes
     */
    private class ServiceStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            if ("active".equals(status)) {
                updateStatus("Shadow is Active", true);
            } else {
                updateStatus("Shadow is Inactive", false);
            }
        }
    }
}
