package com.saaya.automator.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saaya.automator.R;
import com.saaya.automator.data.SaayaMemoryDB;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * LogsAdapter - RecyclerView adapter for history logs
 */
public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogViewHolder> {

    private List<SaayaMemoryDB.LogEntry> logs;
    private SimpleDateFormat dateFormat;

    public LogsAdapter(List<SaayaMemoryDB.LogEntry> logs) {
        this.logs = logs;
        this.dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log_entry, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        SaayaMemoryDB.LogEntry log = logs.get(position);
        holder.bind(log, dateFormat);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvApp, tvRecipient, tvMessage;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            tvApp = itemView.findViewById(R.id.tvLogApp);
            tvRecipient = itemView.findViewById(R.id.tvLogRecipient);
            tvMessage = itemView.findViewById(R.id.tvLogMessage);
        }

        void bind(SaayaMemoryDB.LogEntry log, SimpleDateFormat dateFormat) {
            if (log == null) {
                return;
            }
            
            tvTime.setText(dateFormat.format(new Date(log.timestamp)));
            tvApp.setText(getFriendlyAppName(log.packageName));
            tvRecipient.setText("To: " + (log.recipientName != null ? log.recipientName : "Unknown"));
            
            String messagePreview = "";
            if (log.messageText != null) {
                messagePreview = log.messageText.length() > 50 ? 
                                log.messageText.substring(0, 50) + "..." : log.messageText;
            }
            tvMessage.setText(messagePreview);
        }

        private String getFriendlyAppName(String packageName) {
            if (packageName.contains("whatsapp")) return "WhatsApp";
            if (packageName.contains("messenger")) return "Messenger";
            if (packageName.contains("instagram")) return "Instagram";
            return packageName;
        }
    }
}
