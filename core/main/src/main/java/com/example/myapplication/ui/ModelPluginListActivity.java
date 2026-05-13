package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DAOs.PluginDatabase;
import com.example.myapplication.DAOs.entities.ConfigHeader;
import com.example.myapplication.DAOs.entities.FormatterPlugin;
import com.example.myapplication.DAOs.entities.ModelPlugin;
import com.example.myapplication.DAOs.entities.ModelPluginWithPluginName;
import com.example.myapplication.DAOs.entities.Plugin;
import com.example.myapplication.ui.adapter.ModelPluginListAdapter;
import com.rk.terminal.R;

import org.mobchain.models.BuiltInFormatters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ModelPluginListActivity extends AppCompatActivity {

    private ModelPluginListAdapter adapter;
    private List<ModelPluginWithPluginName> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_plugin_list);

        items = new ArrayList<>();
        adapter = new ModelPluginListAdapter(
                items,
                (position, item) -> {
                    Intent intent = new Intent(this, ModelPluginDetailActivity.class);
                    intent.putExtra(ModelPluginDetailActivity.EXTRA_MODEL_PLUGIN_ID, item.getModelPlugin().getPluginId());
                    startActivity(intent);
                },
                (position, item) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        PluginDatabase db = PluginDatabase.getInstance(this);
                        int pluginId = item.getModelPlugin().getPluginId();
                        db.modelPluginDao().delete(item.getModelPlugin());
                        // Also delete the parent Plugin row to free the unique name
                        Plugin parentPlugin = db.pluginDao().getById(pluginId);
                        if (parentPlugin != null) {
                            db.pluginDao().delete(parentPlugin);
                        }
                        runOnUiThread(() -> {
                            adapter.removeItem(position);
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                        });
                    });
                }
        );

        RecyclerView recycler = findViewById(R.id.recyclerItems);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        ImageButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> showAddDialog());

        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    private void loadItems() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ModelPluginWithPluginName> all = PluginDatabase.getInstance(this).modelPluginDao().getAllWithPluginName();
            runOnUiThread(() -> {
                items.clear();
                items.addAll(all);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void showAddDialog() {
        // Load formatters + their plugin names from DB first, then show dialog
        Executors.newSingleThreadExecutor().execute(() -> {
            PluginDatabase db = PluginDatabase.getInstance(this);
            List<FormatterPlugin> formatters = db.formatterDao().getAll();
            List<String> fmtNames = new ArrayList<>();
            for (FormatterPlugin fp : formatters) {
                Plugin p = db.pluginDao().getById(fp.getPluginId());
                fmtNames.add(p != null ? p.getName() : "Unknown");
            }
            runOnUiThread(() -> buildAndShowDialog(formatters, fmtNames));
        });
    }

    private void buildAndShowDialog(List<FormatterPlugin> formatters, List<String> fmtNames) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_model_plugin, null);
        EditText edtPluginName = dialogView.findViewById(R.id.edtPluginName);
        EditText edtName = dialogView.findViewById(R.id.edtModelName);
        EditText edtUrl = dialogView.findViewById(R.id.edtApiUrl);
        EditText edtTimeout = dialogView.findViewById(R.id.edtTimeout);
        Spinner spinnerFormatter = dialogView.findViewById(R.id.spinnerFormatter);
        LinearLayout headersContainer = dialogView.findViewById(R.id.headersContainer);
        ImageButton btnAddHeader = dialogView.findViewById(R.id.btnAddHeaderInDialog);

        // Build parallel display-name/id lists for the formatter spinner.
        // Order:  [None, <built-in formatters...>, <user-uploaded formatters...>]
        List<String> formatterNames = new ArrayList<>();
        List<Integer> formatterIdList = new ArrayList<>(); // null = "None"
        formatterNames.add("None");
        formatterIdList.add(null);

        // Built-in formatters (loaded from code, no upload required).
        for (Map.Entry<Integer, String> e : BuiltInFormatters.getAll().entrySet()) {
            formatterNames.add(e.getValue());
            formatterIdList.add(e.getKey());
        }

        // User-uploaded formatter plugins.
        for (int i = 0; i < formatters.size(); i++) {
            formatterNames.add(fmtNames.get(i));
            formatterIdList.add(formatters.get(i).getPluginId());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, formatterNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFormatter.setAdapter(spinnerAdapter);

        // Default-select the first built-in formatter so users can proceed
        // without having uploaded anything.
        if (formatterNames.size() > 1) {
            spinnerFormatter.setSelection(1);
        }

        List<View> headerRows = new ArrayList<>();

        btnAddHeader.setOnClickListener(v -> {
            View row = LayoutInflater.from(this).inflate(R.layout.dialog_header_row, headersContainer, false);
            headersContainer.addView(row);
            headerRows.add(row);
            row.findViewById(R.id.btnRemoveRow).setOnClickListener(rv -> {
                headersContainer.removeView(row);
                headerRows.remove(row);
            });
        });

        new AlertDialog.Builder(this)
                .setTitle("Add Model Plugin")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String pluginName = edtPluginName.getText().toString().trim();
                    String name = edtName.getText().toString().trim();
                    String url = edtUrl.getText().toString().trim();
                    int timeout;
                    try { timeout = Integer.parseInt(edtTimeout.getText().toString().trim()); }
                    catch (Exception e) { timeout = 30000; }

                    if (pluginName.isEmpty() || name.isEmpty() || url.isEmpty()) {
                        Toast.makeText(this, "Plugin Name, Model Name and URL are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get selected formatter id from the parallel id list
                    int selectedPos = spinnerFormatter.getSelectedItemPosition();
                    Integer formatterId = (selectedPos >= 0 && selectedPos < formatterIdList.size())
                            ? formatterIdList.get(selectedPos)
                            : null;

                    // Built-in formatters use negative sentinel IDs — store null in DB
                    // to avoid FK violation (formatter_id FK → formatter_plugin.plugin_id).
                    // The sentinel is remembered separately and resolved in initAgent().
                    // For now we store the sentinel value in a tag on the plugin name so
                    // initAgent can detect built-in at load time via formatterId == null check
                    // combined with DexLoader's BuiltInFormatters.isBuiltIn() fallback.
                    final Integer formatterIdForDb = (formatterId != null && BuiltInFormatters.isBuiltIn(formatterId))
                            ? null : formatterId;

                    // Collect headers
                    List<String[]> headers = new ArrayList<>();
                    for (View row : headerRows) {
                        String key = ((EditText) row.findViewById(R.id.edtRowKey)).getText().toString().trim();
                        String val = ((EditText) row.findViewById(R.id.edtRowValue)).getText().toString().trim();
                        if (!key.isEmpty()) {
                            headers.add(new String[]{key, val});
                        }
                    }

                    Integer finalFormatterId = formatterIdForDb;
                    String finalPluginName = pluginName;
                    String finalName = name;
                    String finalUrl = url;
                    int finalTimeout = timeout;
                    Executors.newSingleThreadExecutor().execute(() -> {
                        PluginDatabase db = PluginDatabase.getInstance(this);

                        // Create parent Plugin row first (type 1 = model plugin)
                        Plugin parentPlugin = new Plugin(finalPluginName, "1.0", true, 1, "");
                        long pluginId = db.pluginDao().insert(parentPlugin);

                        ModelPlugin mp = new ModelPlugin((int) pluginId, finalName, finalUrl, false, finalTimeout);
                        mp.setFormatterId(finalFormatterId);
                        db.modelPluginDao().insert(mp);

                        // Insert headers linked to the plugin_id
                        for (String[] h : headers) {
                            db.configHeaderDao().insert(new ConfigHeader((int) pluginId, h[0], h[1]));
                        }

                        ModelPluginWithPluginName result = new ModelPluginWithPluginName();
                        result.modelPlugin = mp;
                        result.pluginName = finalPluginName;

                        runOnUiThread(() -> adapter.addItem(result));
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
