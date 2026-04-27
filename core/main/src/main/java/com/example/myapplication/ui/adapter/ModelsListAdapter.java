package com.example.myapplication.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rk.terminal.R;

import java.io.File;
import java.util.List;

public class ModelsListAdapter extends RecyclerView.Adapter<ModelsListAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(int position, File modelFile);
    }

    private final List<File> models;
    private final OnDeleteListener listener;

    public ModelsListAdapter(List<File> models, OnDeleteListener listener) {
        this.models = models;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.model_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = models.get(position);
        holder.name.setText(file.getName());
        long sizeMB = file.length() / (1024 * 1024);
        holder.size.setText(sizeMB + " MB");
        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(holder.getAdapterPosition(), file));
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public void addItem(File file) {
        models.add(file);
        notifyItemInserted(models.size() - 1);
    }

    public void removeItem(int position) {
        models.remove(position);
        notifyItemRemoved(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, size;
        ImageButton deleteBtn;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.txtModelName);
            size = v.findViewById(R.id.txtModelSize);
            deleteBtn = v.findViewById(R.id.btnDeleteModel);
        }
    }
}

