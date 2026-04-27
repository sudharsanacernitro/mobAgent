package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DAOs.PluginDatabase;
import com.example.myapplication.DAOs.entities.MemoryPlugin;
import com.rk.terminal.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MemoryPluginActivity extends AppCompatActivity {

    private List<MemoryPlugin> items;
    private ItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_plugin_list);

        ((TextView) findViewById(R.id.txtTitle)).setText("Memory Plugins");

        items = new ArrayList<>();
        adapter = new ItemAdapter();

        RecyclerView recycler = findViewById(R.id.recyclerItems);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        ImageButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> showAddDialog());

        loadItems();
    }

    private void loadItems() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MemoryPlugin> all = PluginDatabase.getInstance(this).memoryPluginDao().getAll();
            runOnUiThread(() -> {
                items.clear();
                items.addAll(all);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void showAddDialog() {
        EditText input = new EditText(this);
        input.setHint("Plugin ID");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Add Memory Plugin")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    int pluginId;
                    try { pluginId = Integer.parseInt(input.getText().toString().trim()); }
                    catch (Exception e) {
                        Toast.makeText(this, "Invalid plugin ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    MemoryPlugin mp = new MemoryPlugin(pluginId);
                    Executors.newSingleThreadExecutor().execute(() -> {
                        long id = PluginDatabase.getInstance(this).memoryPluginDao().insert(mp);
                        mp.setPluginId((int) id);
                        runOnUiThread(() -> {
                            items.add(mp);
                            adapter.notifyItemInserted(items.size() - 1);
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            MemoryPlugin item = items.get(position);
            holder.subtitle.setText("Plugin ID: " + item.getPluginId());
            holder.deleteBtn.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                Executors.newSingleThreadExecutor().execute(() -> {
                    PluginDatabase.getInstance(MemoryPluginActivity.this).memoryPluginDao().delete(item);
                    runOnUiThread(() -> {
                        items.remove(pos);
                        notifyItemRemoved(pos);
                    });
                });
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            ImageButton deleteBtn;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.txtItemTitle);
                subtitle = v.findViewById(R.id.txtItemSubtitle);
                deleteBtn = v.findViewById(R.id.btnDeleteItem);
            }
        }
    }
}

