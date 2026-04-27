package com.example.myapplication.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.rk.terminal.R;

import org.json.JSONObject;

import java.util.List;

public class PublicToolsListAdapter extends RecyclerView.Adapter<PublicToolsListAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(int position, JSONObject tool);
    }

    private final List<JSONObject> data;
    private final OnDeleteListener deleteListener;

    public PublicToolsListAdapter(List<JSONObject> data, OnDeleteListener deleteListener) {
        this.data = data;
        this.deleteListener = deleteListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.public_tool_container, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        JSONObject tool = data.get(position);

        String name = tool.optString("name", "Unknown");
        String description = tool.optString("description", "");

        holder.txtToolName.setText(name);
        holder.txtToolDescription.setText(description);

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (deleteListener != null) {
                deleteListener.onDelete(pos, data.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void removeItem(int position) {
        data.remove(position);
        notifyItemRemoved(position);
    }

    public void addItem(JSONObject item) {
        data.add(item);
        notifyItemInserted(data.size() - 1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtToolName;
        TextView txtToolDescription;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            txtToolName = itemView.findViewById(R.id.txtPublicToolName);
            txtToolDescription = itemView.findViewById(R.id.txtPublicToolDescription);
            btnDelete = itemView.findViewById(R.id.btnDeletePublicTool);
        }
    }
}

