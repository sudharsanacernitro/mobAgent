package com.example.myapplication.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DAOs.entities.ModelPluginWithPluginName;
import com.rk.terminal.R;

import java.util.List;

public class ModelPluginListAdapter extends RecyclerView.Adapter<ModelPluginListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(int position, ModelPluginWithPluginName item);
    }

    public interface OnDeleteListener {
        void onDelete(int position, ModelPluginWithPluginName item);
    }

    private final List<ModelPluginWithPluginName> items;
    private final OnItemClickListener clickListener;
    private final OnDeleteListener deleteListener;

    public ModelPluginListAdapter(List<ModelPluginWithPluginName> items, OnItemClickListener clickListener, OnDeleteListener deleteListener) {
        this.items = items;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.model_plugin_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelPluginWithPluginName item = items.get(position);
        holder.name.setText(item.getPluginName());
        holder.url.setText(item.getModelPlugin().getApiUrl());
        holder.stream.setText(item.getModelPlugin().isStream() ? "Stream: ON" : "Stream: OFF");
        holder.itemView.setOnClickListener(v -> clickListener.onClick(holder.getAdapterPosition(), item));
        holder.deleteBtn.setOnClickListener(v -> deleteListener.onDelete(holder.getAdapterPosition(), item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void addItem(ModelPluginWithPluginName item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, url, stream;
        ImageButton deleteBtn;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.txtModelPluginName);
            url = v.findViewById(R.id.txtModelPluginUrl);
            stream = v.findViewById(R.id.txtModelPluginStream);
            deleteBtn = v.findViewById(R.id.btnDeleteModelPlugin);
        }
    }
}
