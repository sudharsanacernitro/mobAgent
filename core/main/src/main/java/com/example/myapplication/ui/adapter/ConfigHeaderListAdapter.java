package com.example.myapplication.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DAOs.entities.ConfigHeader;
import com.rk.terminal.R;

import java.util.List;

public class ConfigHeaderListAdapter extends RecyclerView.Adapter<ConfigHeaderListAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(int position, ConfigHeader item);
    }

    private final List<ConfigHeader> items;
    private final OnDeleteListener deleteListener;

    public ConfigHeaderListAdapter(List<ConfigHeader> items, OnDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.config_header_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConfigHeader item = items.get(position);

        // Remove previous watchers to avoid loops
        if (holder.keyWatcher != null) holder.key.removeTextChangedListener(holder.keyWatcher);
        if (holder.valueWatcher != null) holder.value.removeTextChangedListener(holder.valueWatcher);

        holder.key.setText(item.getHeaderKey());
        holder.value.setText(item.getHeaderValue());

        holder.keyWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    items.get(pos).setHeaderKey(s.toString());
                }
            }
        };
        holder.valueWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    items.get(pos).setHeaderValue(s.toString());
                }
            }
        };

        holder.key.addTextChangedListener(holder.keyWatcher);
        holder.value.addTextChangedListener(holder.valueWatcher);

        holder.deleteBtn.setOnClickListener(v -> deleteListener.onDelete(holder.getAdapterPosition(), item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void addItem(ConfigHeader item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public List<ConfigHeader> getAll() {
        return items;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        EditText key, value;
        ImageButton deleteBtn;
        TextWatcher keyWatcher, valueWatcher;

        ViewHolder(View v) {
            super(v);
            key = v.findViewById(R.id.edtHeaderKey);
            value = v.findViewById(R.id.edtHeaderValue);
            deleteBtn = v.findViewById(R.id.btnDeleteHeader);
        }
    }

    static abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}

