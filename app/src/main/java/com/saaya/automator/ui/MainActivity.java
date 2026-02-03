package com.saaya.automator.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.saaya.automator.R;
import com.saaya.automator.core.SaayaService;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity - Chat Interface
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private EditText inputField;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "setContentView successful");

            // Set title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Saaya");
            }

            // Initialize views
            Log.d(TAG, "Finding views...");
            recyclerView = findViewById(R.id.chatRecyclerView);
            inputField = findViewById(R.id.inputField);
            sendButton = findViewById(R.id.sendButton);
            
            Log.d(TAG, "RecyclerView: " + (recyclerView != null));
            Log.d(TAG, "InputField: " + (inputField != null));
            Log.d(TAG, "SendButton: " + (sendButton != null));

            // Null checks
            if (recyclerView == null || inputField == null || sendButton == null) {
                Log.e(TAG, "ERROR: One or more views are null!");
                Toast.makeText(this, "Error: UI components not found. Please reinstall app.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Setup RecyclerView
            Log.d(TAG, "Setting up RecyclerView...");
            messages = new ArrayList<>();
            chatAdapter = new ChatAdapter(messages);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(chatAdapter);
            Log.d(TAG, "RecyclerView setup complete");

            // Add welcome message
            addBotMessage("Assalam o Alaikum! Main Saaya hoon, aapka digital shadow. Main aapki messages observe kar ke seekhta rehta hoon.");

            // Send button click
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessage();
                }
            });

            // Check accessibility
            checkAccessibilityService();
            
            Log.d(TAG, "onCreate completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "FATAL ERROR in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Fatal Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    private void sendMessage() {
        String messageText = inputField.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            return;
        }

        // Add user message
        messages.add(new ChatMessage(messageText, true));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        recyclerView.smoothScrollToPosition(messages.size() - 1);

        // Clear input
        inputField.setText("");

        // Simulate bot reply
        simulateBotReply(messageText);
    }

    private void simulateBotReply(String userMessage) {
        // Delayed response
        recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                String botReply;
                
                if (userMessage.toLowerCase().contains("stats") || 
                    userMessage.toLowerCase().contains("dashboard")) {
                    botReply = "Dashboard dekhne ke liye upar right corner mein menu icon pe tap karen.";
                } else if (userMessage.toLowerCase().contains("kaise") || 
                          userMessage.toLowerCase().contains("how")) {
                    botReply = "Main accessibility service use karke aapki messages observe karta hoon aur patterns seekhta hoon. Privacy first - passwords kabhi nahi record hote.";
                } else {
                    botReply = "Note kar liya sir! Main aapki writing style samajh raha hoon.";
                }
                
                addBotMessage(botReply);
            }
        }, 800);
    }

    private void addBotMessage(String text) {
        messages.add(new ChatMessage(text, false));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        recyclerView.smoothScrollToPosition(messages.size() - 1);
    }

    private void checkAccessibilityService() {
        SaayaService service = SaayaService.getInstance();
        if (service == null || !service.isServiceActive()) {
            // Show message to enable accessibility
            addBotMessage("⚠️ Abhi main inactive hoon. Mujhe activate karne ke liye:\n\n1. Settings → Accessibility\n2. Saaya ko enable karen\n3. Wapas aayein");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Dashboard")
            .setIcon(android.R.drawable.ic_menu_info_details)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        menu.add(0, 2, 0, "Enable Service")
            .setIcon(android.R.drawable.ic_menu_preferences)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                // Open dashboard
                Intent intent = new Intent(this, StatsActivity.class);
                startActivity(intent);
                return true;
            
            case 2:
                // Open accessibility settings
                Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(settingsIntent);
                Toast.makeText(this, "Enable 'Saaya' in Accessibility", Toast.LENGTH_LONG).show();
                return true;
            
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * ChatMessage - Model class
     */
    public static class ChatMessage {
        public final String text;
        public final boolean isUser;

        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }
}
