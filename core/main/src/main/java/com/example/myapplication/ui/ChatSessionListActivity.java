package com.example.myapplication.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DAOs.PluginDatabase;
import com.example.myapplication.DAOs.entities.ChatSession;
import com.example.myapplication.DAOs.entities.ModelPlugin;
import com.example.myapplication.DAOs.entities.MemoryPlugin;
import com.example.myapplication.DAOs.entities.Plugin;
import com.rk.terminal.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ChatSessionListActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "session_id";

    private List<ChatSession> sessions;
    private SessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_session_list);

        sessions = new ArrayList<>();
        adapter = new SessionAdapter();

        RecyclerView recycler = findViewById(R.id.recyclerSessions);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        ImageButton btnAdd = findViewById(R.id.btnAddSession);
        btnAdd.setOnClickListener(v -> showAddDialog());

        loadSessions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSessions();
    }

    private void loadSessions() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ChatSession> all = PluginDatabase.getInstance(this).chatSessionDao().getAll();
            runOnUiThread(() -> {
                sessions.clear();
                sessions.addAll(all);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void showAddDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            PluginDatabase db = PluginDatabase.getInstance(this);
            List<ModelPlugin> modelPlugins = db.modelPluginDao().getAll();
            List<MemoryPlugin> memoryPlugins = db.memoryPluginDao().getAll();

            // Fetch plugin names
            List<String> modelNames = new ArrayList<>();
            List<Integer> modelIds = new ArrayList<>();
            for (ModelPlugin mp : modelPlugins) {
                Plugin p = db.pluginDao().getById(mp.getPluginId());
                modelNames.add(p != null ? p.getName() : "Unknown");
                modelIds.add(mp.getPluginId());
            }

            List<String> memoryNames = new ArrayList<>();
            List<Integer> memoryIds = new ArrayList<>();
            memoryNames.add("None");
            memoryIds.add(-1);
            for (MemoryPlugin mem : memoryPlugins) {
                Plugin p = db.pluginDao().getById(mem.getPluginId());
                memoryNames.add(p != null ? p.getName() : "Unknown");
                memoryIds.add(mem.getPluginId());
            }

            runOnUiThread(() -> buildAndShowDialog(modelNames, modelIds, memoryNames, memoryIds));
        });
    }

    private void buildAndShowDialog(List<String> modelNames, List<Integer> modelIds,
                                     List<String> memoryNames, List<Integer> memoryIds) {
        if (modelNames.isEmpty()) {
            Toast.makeText(this, "No model plugins available. Add one first.", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_session, null);
        EditText edtName = dialogView.findViewById(R.id.edtSessionName);
        Spinner spinnerModel = dialogView.findViewById(R.id.spinnerModelPlugin);
        Spinner spinnerMemory = dialogView.findViewById(R.id.spinnerMemoryPlugin);

        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modelNames);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);

        ArrayAdapter<String> memoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, memoryNames);
        memoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMemory.setAdapter(memoryAdapter);

        new AlertDialog.Builder(this)
                .setTitle("New Chat Session")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Session name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int modelPos = spinnerModel.getSelectedItemPosition();
                    int memoryPos = spinnerMemory.getSelectedItemPosition();

                    int selectedModelId = modelIds.get(modelPos);
                    Integer selectedMemoryId = memoryIds.get(memoryPos);
                    if (selectedMemoryId == -1) selectedMemoryId = null;

                    Integer finalMemoryId = selectedMemoryId;
                    Executors.newSingleThreadExecutor().execute(() -> {
                        ChatSession session = new ChatSession(name, selectedModelId, finalMemoryId, System.currentTimeMillis());
                        long id = PluginDatabase.getInstance(this).chatSessionDao().insert(session);
                        session.setId((int) id);

                        runOnUiThread(() -> {
                            sessions.add(0, session);
                            adapter.notifyItemInserted(0);
                            Toast.makeText(this, "Session created", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Adapter ──

    private void showEditPluginsDialog(ChatSession session) {
        Executors.newSingleThreadExecutor().execute(() -> {
            PluginDatabase db = PluginDatabase.getInstance(this);
            List<ModelPlugin> modelPlugins = db.modelPluginDao().getAll();
            List<MemoryPlugin> memoryPlugins = db.memoryPluginDao().getAll();

            List<String> modelNames = new ArrayList<>();
            List<Integer> modelIds = new ArrayList<>();
            for (ModelPlugin mp : modelPlugins) {
                Plugin p = db.pluginDao().getById(mp.getPluginId());
                modelNames.add(p != null ? p.getName() : "Unknown");
                modelIds.add(mp.getPluginId());
            }

            List<String> memoryNames = new ArrayList<>();
            List<Integer> memoryIds = new ArrayList<>();
            memoryNames.add("None (In-Memory)");
            memoryIds.add(-1);
            for (MemoryPlugin mem : memoryPlugins) {
                Plugin p = db.pluginDao().getById(mem.getPluginId());
                memoryNames.add(p != null ? p.getName() : "Unknown");
                memoryIds.add(mem.getPluginId());
            }

            runOnUiThread(() -> {
                if (modelNames.isEmpty()) {
                    Toast.makeText(this, "No model plugins available.", Toast.LENGTH_SHORT).show();
                    return;
                }

                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_plugins, null);
                Spinner spinnerModel = dialogView.findViewById(R.id.spinnerModelPlugin);
                Spinner spinnerMemory = dialogView.findViewById(R.id.spinnerMemoryPlugin);
                Button btnSettings = dialogView.findViewById(R.id.btnGoToSettings);

                ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, modelNames);
                modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerModel.setAdapter(modelAdapter);

                // Pre-select current model
                int modelIdx = modelIds.indexOf(session.getModelPluginId());
                if (modelIdx >= 0) spinnerModel.setSelection(modelIdx);

                ArrayAdapter<String> memoryAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, memoryNames);
                memoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerMemory.setAdapter(memoryAdapter);

                // Pre-select current memory
                if (session.getMemoryPluginId() != null) {
                    int memIdx = memoryIds.indexOf(session.getMemoryPluginId());
                    if (memIdx >= 0) spinnerMemory.setSelection(memIdx);
                }

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Change Plugins for: " + session.getSessionName())
                        .setView(dialogView)
                        .setPositiveButton("Save", (d, which) -> {
                            int selectedModelId = modelIds.get(spinnerModel.getSelectedItemPosition());
                            Integer selectedMemoryId = memoryIds.get(spinnerMemory.getSelectedItemPosition());
                            if (selectedMemoryId == -1) selectedMemoryId = null;

                            Integer finalMemoryId = selectedMemoryId;
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.chatSessionDao().updatePlugins(session.getId(), selectedModelId, finalMemoryId);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Plugins updated", Toast.LENGTH_SHORT).show();
                                    loadSessions();
                                });
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .create();

                btnSettings.setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(new Intent(this, SettingsActivity.class));
                });

                dialog.show();
            });
        });
    }

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ChatSession session = sessions.get(position);
            holder.title.setText(session.getSessionName());
            holder.subtitle.setText("ID: " + session.getId());

            // Click to select session
            holder.itemView.setOnClickListener(v -> {
                Intent result = new Intent();
                result.putExtra(EXTRA_SESSION_ID, session.getId());
                setResult(Activity.RESULT_OK, result);
                finish();
            });

            // Long-click to change plugins for this session
            holder.itemView.setOnLongClickListener(v -> {
                showEditPluginsDialog(session);
                return true;
            });

            // Delete session
            holder.deleteBtn.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                ChatSession s = sessions.get(pos);
                Executors.newSingleThreadExecutor().execute(() -> {
                    PluginDatabase.getInstance(ChatSessionListActivity.this).chatSessionDao().delete(s);
                    runOnUiThread(() -> {
                        sessions.remove(pos);
                        notifyItemRemoved(pos);
                    });
                });
            });
        }

        @Override
        public int getItemCount() { return sessions.size(); }

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

