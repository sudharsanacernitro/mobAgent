package com.example.myapplication.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DAOs.PluginDatabase;
import com.example.myapplication.DAOs.entities.FormatterPlugin;
import com.example.myapplication.DAOs.entities.Plugin;
import com.rk.terminal.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/** Helper class to hold a FormatterPlugin together with its Plugin name */
class FormatterWithName {
    public final FormatterPlugin formatter;
    public final String pluginName;
    public final int pluginId;

    FormatterWithName(FormatterPlugin formatter, String pluginName) {
        this.formatter = formatter;
        this.pluginName = pluginName;
        this.pluginId = formatter.getPluginId();
    }
}

public class FormatterPluginActivity extends AppCompatActivity {

    private List<FormatterWithName> items;
    private ItemAdapter adapter;
    private ActivityResultLauncher<Intent> jarPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_plugin_list);

        ((TextView) findViewById(R.id.txtTitle)).setText("Formatter Plugins");

        items = new ArrayList<>();
        adapter = new ItemAdapter();

        RecyclerView recycler = findViewById(R.id.recyclerItems);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        ImageButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> openJarPicker());

        jarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) handleJarUpload(uri);
                    }
                }
        );

        loadItems();
    }

    private void openJarPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        jarPickerLauncher.launch(Intent.createChooser(intent, "Select .jar file"));
    }

    private String getFileName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        }
        return name;
    }

    private void handleJarUpload(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName == null || !fileName.endsWith(".jar")) {
            Toast.makeText(this, "Please select a .jar file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Plugin name = filename without .jar extension
        String pluginName = fileName.substring(0, fileName.length() - 4);

        // Storage folder
        File storageDir = new File(getDataDir(), "local/alpine/root/plugins/formatterplugin");
        storageDir.mkdirs();

        File destFile = new File(storageDir, fileName);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Copy jar to storage
                try (InputStream is = getContentResolver().openInputStream(uri);
                     FileOutputStream fos = new FileOutputStream(destFile)) {
                    if (is == null) throw new Exception("Failed to open input stream");
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }

                PluginDatabase db = PluginDatabase.getInstance(this);

                // Insert into plugins table (type=3 for formatter)
                Plugin plugin = new Plugin(pluginName, "1.0", true, 3, destFile.getAbsolutePath());
                long pluginId = db.pluginDao().insert(plugin);

                // Insert into formatter_plugin table
                FormatterPlugin fp = new FormatterPlugin((int) pluginId);
                db.formatterDao().insert(fp);

                FormatterWithName item = new FormatterWithName(fp, pluginName);

                runOnUiThread(() -> {
                    items.add(item);
                    adapter.notifyItemInserted(items.size() - 1);
                    Toast.makeText(this, "Uploaded: " + pluginName, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("FormatterPlugin", "Upload failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadItems() {
        Executors.newSingleThreadExecutor().execute(() -> {
            PluginDatabase db = PluginDatabase.getInstance(this);
            List<FormatterPlugin> all = db.formatterDao().getAll();
            List<FormatterWithName> loaded = new ArrayList<>();
            for (FormatterPlugin fp : all) {
                Plugin p = db.pluginDao().getById(fp.getPluginId());
                String name = (p != null) ? p.getName() : "Unknown";
                loaded.add(new FormatterWithName(fp, name));
            }
            runOnUiThread(() -> {
                items.clear();
                items.addAll(loaded);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FormatterWithName item = items.get(position);
            holder.title.setText(item.pluginName);
            holder.subtitle.setText("Plugin ID: " + item.pluginId);
            holder.deleteBtn.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                FormatterWithName fwn = items.get(pos);
                Executors.newSingleThreadExecutor().execute(() -> {
                    PluginDatabase db = PluginDatabase.getInstance(FormatterPluginActivity.this);
                    // Deleting the parent Plugin cascades to formatter_plugin
                    Plugin p = db.pluginDao().getById(fwn.pluginId);
                    if (p != null) {
                        db.pluginDao().delete(p);
                        // Also delete the jar file
                        if (p.getPath() != null) {
                            File jarFile = new File(p.getPath());
                            jarFile.delete();
                        }
                    }

                    runOnUiThread(() -> {
                        items.remove(pos);
                        notifyItemRemoved(pos);
                    });
                });
            });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
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
