package com.example.myapplication.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.ui.adapter.ModelsListAdapter;
import com.rk.terminal.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LocalLLMActivity extends AppCompatActivity {

    private File modelsDir;
    private ModelsListAdapter adapter;
    private List<File> modelsList;
    private ActivityResultLauncher<Intent> filePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_llm);

        modelsDir = new File(getDataDir(), "local/alpine/root/llmModels");
        modelsDir.mkdirs();

        modelsList = loadModels();

        filePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        uploadModel(result.getData().getData());
                    }
                }
        );

        ImageButton btnUpload = findViewById(R.id.btnUploadModel);
        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            filePicker.launch(intent);
        });

        RecyclerView recycler = findViewById(R.id.recyclerModels);
        adapter = new ModelsListAdapter(modelsList, (position, file) -> {
            file.delete();
            adapter.removeItem(position);
            Toast.makeText(this, "Model deleted", Toast.LENGTH_SHORT).show();
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    private void uploadModel(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = "model_" + System.currentTimeMillis() + ".gguf";
            try {
                android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) fileName = cursor.getString(idx);
                    cursor.close();
                }
            } catch (Exception ignored) {}

            if (!fileName.endsWith(".gguf")) {
                Toast.makeText(this, "Please select a .gguf file", Toast.LENGTH_LONG).show();
                return;
            }

            File destFile = new File(modelsDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) != -1) fos.write(buf, 0, len);
            }

            adapter.addItem(destFile);
            Toast.makeText(this, "Model '" + fileName + "' uploaded", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<File> loadModels() {
        List<File> list = new ArrayList<>();
        if (!modelsDir.exists()) return list;
        File[] files = modelsDir.listFiles((dir, name) -> name.endsWith(".gguf"));
        if (files != null) {
            for (File f : files) list.add(f);
        }
        return list;
    }
}

