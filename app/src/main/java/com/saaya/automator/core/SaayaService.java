package com.saaya.automator.core;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.saaya.automator.data.SaayaMemoryDB;

import java.util.List;

/**
 * SaayaService - Core Accessibility Service
 * The "Shadow" that observes user interactions and learns patterns.
 * SECURITY: Ignores password fields to protect sensitive data.
 */
public class SaayaService extends AccessibilityService {

    private static final String TAG = "SaayaService";
    private static SaayaService instance;
    private SaayaMemoryDB memoryDB;
    private boolean isActive = false;

    // Packages to ignore for privacy
    private static final String[] IGNORED_PACKAGES = {
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.inputmethod"
    };

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
        
        // Broadcast status change to MainActivity
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

        // Ignore system packages
        if (shouldIgnorePackage(packageName)) {
            return;
        }

        int eventType = event.getEventType();

        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                handleTextChanged(event, packageName);
                break;

            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                handleViewClicked(event, packageName);
                break;

            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                handleWindowContentChanged(event, packageName);
                break;

            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handleWindowStateChanged(event, packageName);
                break;
        }
    }

    /**
     * SECURITY CRITICAL: Handle text changes with password protection
     */
    private void handleTextChanged(AccessibilityEvent event, String packageName) {
        AccessibilityNodeInfo source = event.getSource();
        
        if (source == null) {
            return;
        }

        // ★ SECURITY CHECK: Ignore password fields ★
        if (source.isPassword()) {
            Log.d(TAG, "⚠ Password field detected - IGNORED for security");
            source.recycle();
            return;
        }

        // Extract text safely
        CharSequence text = source.getText();
        if (text != null && text.length() > 0) {
            String textContent = text.toString();
            
            // Additional security: Don't store very short inputs (likely passwords/PINs)
            if (textContent.length() < 3) {
                source.recycle();
                return;
            }

            // Learn pattern
            memoryDB.learnPattern(packageName, "text_input", textContent);
            Log.d(TAG, "📝 Learned text input pattern from: " + packageName);
        }

        source.recycle();
    }

    /**
     * Handle view click events
     */
    private void handleViewClicked(AccessibilityEvent event, String packageName) {
        AccessibilityNodeInfo source = event.getSource();
        
        if (source == null) {
            return;
        }

        CharSequence contentDesc = source.getContentDescription();
        CharSequence text = source.getText();
        
        String context = "";
        if (contentDesc != null) {
            context = contentDesc.toString();
        } else if (text != null) {
            context = text.toString();
        }

        if (!context.isEmpty()) {
            memoryDB.learnPattern(packageName, "click_event", context);
            Log.d(TAG, "👆 Learned click pattern: " + context);
        }

        source.recycle();
    }

    /**
     * Handle window content changes
     */
    private void handleWindowContentChanged(AccessibilityEvent event, String packageName) {
        // Can be used for detecting app state changes
        // For now, we just log it to avoid excessive database writes
        Log.v(TAG, "Window content changed in: " + packageName);
    }

    /**
     * Handle window state changes (app switches)
     */
    private void handleWindowStateChanged(AccessibilityEvent event, String packageName) {
        CharSequence className = event.getClassName();
        if (className != null) {
            String activity = className.toString();
            memoryDB.learnPattern(packageName, "app_opened", activity);
            Log.d(TAG, "📱 App opened: " + packageName);
        }
    }

    /**
     * Tap at specific coordinates using gesture automation
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if gesture was dispatched successfully
     */
    public boolean tapCoordinates(int x, int y) {
        Path tapPath = new Path();
        tapPath.moveTo(x, y);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        GestureDescription.StrokeDescription stroke = 
            new GestureDescription.StrokeDescription(tapPath, 0, 100);
        gestureBuilder.addStroke(stroke);

        boolean result = dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "✓ Tap gesture completed at (" + x + ", " + y + ")");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "✗ Tap gesture cancelled");
            }
        }, null);

        return result;
    }

    /**
     * Perform swipe gesture
     * 
     * @param startX Start X coordinate
     * @param startY Start Y coordinate
     * @param endX   End X coordinate
     * @param endY   End Y coordinate
     * @return true if gesture was dispatched successfully
     */
    public boolean swipe(int startX, int startY, int endX, int endY) {
        Path swipePath = new Path();
        swipePath.moveTo(startX, startY);
        swipePath.lineTo(endX, endY);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        GestureDescription.StrokeDescription stroke = 
            new GestureDescription.StrokeDescription(swipePath, 0, 500);
        gestureBuilder.addStroke(stroke);

        boolean result = dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "✓ Swipe gesture completed");
            }
        }, null);

        return result;
    }

    /**
     * Find node by text and perform click
     * 
     * @param targetText The text to search for
     * @return true if found and clicked
     */
    public boolean clickByText(String targetText) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return false;
        }

        List<AccessibilityNodeInfo> nodes = 
            rootNode.findAccessibilityNodeInfosByText(targetText);

        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isClickable()) {
                    boolean result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    node.recycle();
                    return result;
                }
            }
        }

        rootNode.recycle();
        return false;
    }

    /**
     * Get suggestions based on current context
     */
    public List<String> getSuggestions(String context) {
        return memoryDB.recallPattern(context);
    }

    /**
     * Check if package should be ignored
     */
    private boolean shouldIgnorePackage(String packageName) {
        for (String ignored : IGNORED_PACKAGES) {
            if (packageName.contains(ignored)) {
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
