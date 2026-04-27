package com.example.myapplication.ui.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rk.terminal.R;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    public static class ChatMsg {
        public final String sender; // "You" or "AI"
        public final String message;

        public ChatMsg(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }
    }

    private final List<ChatMsg> messages;

    public ChatMessageAdapter(List<ChatMsg> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMsg msg = messages.get(position);
        holder.sender.setText(msg.sender);
        holder.message.setText(msg.message);

        boolean isUser = "You".equals(msg.sender);
        holder.sender.setTextColor(isUser ? 0xFF64B5F6 : 0xFF81C784);
        holder.container.setGravity(isUser ? Gravity.END : Gravity.START);
        holder.message.setBackgroundColor(isUser ? 0xFF1A237E : 0xFF1B5E20);
        holder.message.setPadding(24, 16, 24, 16);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public void addMessage(ChatMsg msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sender, message;
        LinearLayout container;

        ViewHolder(View v) {
            super(v);
            sender = v.findViewById(R.id.txtSender);
            message = v.findViewById(R.id.txtMessage);
            container = (LinearLayout) v;
        }
    }
}

