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

import com.rk.terminal.R;
import com.example.myapplication.ui.adapter.PublicToolsListAdapter;
import com.example.myapplication.utils.ZipUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PublicToolsActivity extends AppCompatActivity {

    private File toolsDir;
    private PublicToolsListAdapter adapter;
    private List<JSONObject> toolsList;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_tools);

        toolsDir = new File(getDataDir(), "local/alpine/root/tools");

        toolsList = loadTools();

        // Upload button
        ImageButton uploadButton = findViewById(R.id.btnUploadPublicTool);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        uploadToolZip(fileUri);
                    }
                }
        );

        uploadButton.setOnClickListener(v -> openFilePicker());

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerPublicTools);
        adapter = new PublicToolsListAdapter(toolsList, (position, tool) -> {
            String folderName = tool.optString("folderName", "");
            if (!folderName.isEmpty()) {
                deleteFolder(new File(toolsDir, folderName));
                adapter.removeItem(position);
                Toast.makeText(this, "Tool deleted", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed"});
        filePickerLauncher.launch(intent);
    }

    private void uploadToolZip(Uri zipUri) {
        try (InputStream is = getContentResolver().openInputStream(zipUri)) {
            if (is == null) {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
                return;
            }

            File tempDir = new File(getCacheDir(), "tool_upload_" + System.currentTimeMillis());
            ZipUtils.extractZip(is, tempDir);

            // The zip should contain a tool folder (folder name = tool name) with config.json + binary
            // It may be at root level or inside a wrapper folder
            File configJson = new File(tempDir, "config.json");
            File actualToolDir = tempDir;

            if (configJson.exists()) {
                // The zip contents are the tool contents directly
                // We need to figure out the tool name from config.json
                String jsonContent = readFileContent(configJson);
                JSONObject config = new JSONObject(jsonContent);
                String toolName = config.optString("name", "tool_" + System.currentTimeMillis());

                File destDir = new File(toolsDir, toolName);
                if (destDir.exists()) deleteFolder(destDir);
                toolsDir.mkdirs();

                if (!tempDir.renameTo(destDir)) {
                    copyFolder(tempDir, destDir);
                    deleteFolder(tempDir);
                }

                makeBinaryExecutable(destDir);

                JSONObject newTool = buildToolJson(destDir);
                if (newTool != null) adapter.addItem(newTool);

                Toast.makeText(this, "Tool '" + toolName + "' uploaded", Toast.LENGTH_SHORT).show();

            } else {
                // Check for subdirectories (each is a tool folder)
                File[] children = tempDir.listFiles(File::isDirectory);
                if (children == null || children.length == 0) {
                    Toast.makeText(this, "Invalid tool zip: no config.json or tool folders found", Toast.LENGTH_LONG).show();
                    deleteFolder(tempDir);
                    return;
                }

                int count = 0;
                for (File child : children) {
                    File childConfig = new File(child, "config.json");
                    String toolName = child.getName();

                    File destDir = new File(toolsDir, toolName);
                    if (destDir.exists()) deleteFolder(destDir);
                    toolsDir.mkdirs();

                    if (!child.renameTo(destDir)) {
                        copyFolder(child, destDir);
                    }

                    makeBinaryExecutable(destDir);

                    JSONObject newTool = buildToolJson(destDir);
                    if (newTool != null) {
                        adapter.addItem(newTool);
                        count++;
                    }
                }
                deleteFolder(tempDir);
                Toast.makeText(this, count + " tool(s) uploaded", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<JSONObject> loadTools() {
        List<JSONObject> list = new ArrayList<>();
        if (!toolsDir.exists() || !toolsDir.isDirectory()) return list;

        File[] dirs = toolsDir.listFiles(File::isDirectory);
        if (dirs == null) return list;

        for (File dir : dirs) {
            JSONObject obj = buildToolJson(dir);
            if (obj != null) list.add(obj);
        }
        return list;
    }

    private JSONObject buildToolJson(File toolDir) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("folderName", toolDir.getName());

            File configJson = new File(toolDir, "config.json");
            if (configJson.exists()) {
                String content = readFileContent(configJson);
                JSONObject config = new JSONObject(content);
                obj.put("name", config.optString("name", toolDir.getName()));
                obj.put("description", config.optString("description", ""));
            } else {
                obj.put("name", toolDir.getName());
                obj.put("description", "");
            }

            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readFileContent(File file) throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fis.read(bytes);
        fis.close();
        return new String(bytes);
    }

    private void deleteFolder(File folder) {
        if (folder == null || !folder.exists()) return;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteFolder(f);
                else f.delete();
            }
        }
        folder.delete();
    }

    private void copyFolder(File src, File dest) throws Exception {
        if (src.isDirectory()) {
            dest.mkdirs();
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyFolder(child, new File(dest, child.getName()));
                }
            }
        } else {
            java.io.FileInputStream fis = new java.io.FileInputStream(src);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) fos.write(buf, 0, len);
            fis.close();
            fos.close();
        }
    }

    private void makeBinaryExecutable(File toolDir) {
        File[] files = toolDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.getName().endsWith(".json") && f.isFile()) {
                f.setExecutable(true, false);
            }
        }
    }
}

