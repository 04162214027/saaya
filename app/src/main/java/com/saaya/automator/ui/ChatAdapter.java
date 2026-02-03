package com.saaya.automator.ui;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saaya.automator.R;

import java.util.List;

/**
 * ChatAdapter - RecyclerView adapter for chat messages
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<MainActivity.ChatMessage> messages;

    public ChatAdapter(List<MainActivity.ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MainActivity.ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        LinearLayout container;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.messageText);
            container = itemView.findViewById(R.id.messageContainer);
        }

        void bind(MainActivity.ChatMessage message) {
            textView.setText(message.text);
            
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) container.getLayoutParams();
            
            if (message.isUser) {
                // User message (right side, blue)
                params.gravity = Gravity.END;
                container.setBackgroundResource(R.drawable.bg_message_user);
            } else {
                // Bot message (left side, dark grey)
                params.gravity = Gravity.START;
                container.setBackgroundResource(R.drawable.bg_message_bot);
            }
            
            container.setLayoutParams(params);
        }
    }
}
