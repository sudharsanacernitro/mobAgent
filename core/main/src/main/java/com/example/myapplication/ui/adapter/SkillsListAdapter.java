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

public class SkillsListAdapter extends RecyclerView.Adapter<SkillsListAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(int position, JSONObject skill);
    }

    private final List<JSONObject> data;
    private final OnDeleteListener deleteListener;

    public SkillsListAdapter(List<JSONObject> data, OnDeleteListener deleteListener) {
        this.data = data;
        this.deleteListener = deleteListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.skill_container, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        JSONObject skill = data.get(position);

        String name = skill.optString("name", "Unknown");
        String description = skill.optString("description", "");
        int toolCount = skill.optInt("toolCount", 0);

        holder.txtSkillName.setText(name);
        holder.txtSkillDescription.setText(description);
        holder.txtSkillToolCount.setText(toolCount + " tool(s)");

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
        TextView txtSkillName;
        TextView txtSkillDescription;
        TextView txtSkillToolCount;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            txtSkillName = itemView.findViewById(R.id.txtSkillName);
            txtSkillDescription = itemView.findViewById(R.id.txtSkillDescription);
            txtSkillToolCount = itemView.findViewById(R.id.txtSkillToolCount);
            btnDelete = itemView.findViewById(R.id.btnDeleteSkill);
        }
    }
}

