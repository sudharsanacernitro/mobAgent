package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DAOs.ConfigHeaderDao;
import com.example.myapplication.DAOs.PluginDatabase;
import com.example.myapplication.DAOs.entities.ConfigHeader;
import com.example.myapplication.DAOs.entities.ModelPlugin;
import com.example.myapplication.ui.adapter.ConfigHeaderListAdapter;
import com.rk.terminal.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ModelPluginDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MODEL_PLUGIN_ID = "model_plugin_id";

    private int modelPluginId;
    private ConfigHeaderListAdapter headersAdapter;
    private List<ConfigHeader> headersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_plugin_detail);

        modelPluginId = getIntent().getIntExtra(EXTRA_MODEL_PLUGIN_ID, -1);

        TextView txtName = findViewById(R.id.txtDetailName);
        TextView txtUrl = findViewById(R.id.txtDetailUrl);
        TextView txtStream = findViewById(R.id.txtDetailStream);
        TextView txtTimeout = findViewById(R.id.txtDetailTimeout);
        ImageButton btnAddHeader = findViewById(R.id.btnAddHeader);
        Button btnSaveHeaders = findViewById(R.id.btnSaveHeaders);
        RecyclerView recyclerHeaders = findViewById(R.id.recyclerHeaders);

        headersList = new ArrayList<>();
        headersAdapter = new ConfigHeaderListAdapter(headersList, (position, item) -> {
            // Delete from DB if it has an id, then remove from list
            if (item.getId() > 0) {
                Executors.newSingleThreadExecutor().execute(() ->
                        PluginDatabase.getInstance(this).configHeaderDao().delete(item));
            }
            headersAdapter.removeItem(position);
        });
        recyclerHeaders.setLayoutManager(new LinearLayoutManager(this));
        recyclerHeaders.setAdapter(headersAdapter);

        // Load ModelPlugin + headers from DB
        Executors.newSingleThreadExecutor().execute(() -> {
            PluginDatabase db = PluginDatabase.getInstance(this);
            ModelPlugin mp = db.modelPluginDao().getByPluginId(modelPluginId);
            List<ConfigHeader> headers = db.configHeaderDao().getByConfigId(modelPluginId);

            if (mp == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Model plugin not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            runOnUiThread(() -> {
                txtName.setText(mp.getModelName());
                txtUrl.setText("URL: " + mp.getApiUrl());
                txtStream.setText("Stream: " + (mp.isStream() ? "ON" : "OFF"));
                txtTimeout.setText("Timeout: " + mp.getTimeoutMs() + " ms");

                headersList.clear();
                headersList.addAll(headers);
                headersAdapter.notifyDataSetChanged();
            });
        });

        // Add empty header row
        btnAddHeader.setOnClickListener(v -> {
            ConfigHeader newHeader = new ConfigHeader(modelPluginId, "", "");
            headersAdapter.addItem(newHeader);
        });

        // Save all headers
        btnSaveHeaders.setOnClickListener(v -> {
            List<ConfigHeader> allHeaders = headersAdapter.getAll();
            Executors.newSingleThreadExecutor().execute(() -> {
                ConfigHeaderDao dao = PluginDatabase.getInstance(this).configHeaderDao();
                // Delete old, insert fresh
                dao.deleteByConfigId(modelPluginId);
                for (ConfigHeader h : allHeaders) {
                    if (!h.getHeaderKey().trim().isEmpty()) {
                        h.setConfigId(modelPluginId);
                        dao.insert(h);
                    }
                }
                runOnUiThread(() ->
                        Toast.makeText(this, "Headers saved", Toast.LENGTH_SHORT).show());
            });
        });
    }
}
