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
import com.example.myapplication.ui.adapter.SkillsListAdapter;
import com.example.myapplication.utils.ZipUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SkillsActivity extends AppCompatActivity {

    private File skillsDir;
    private SkillsListAdapter adapter;
    private List<JSONObject> skillsList;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skills);

        skillsDir = new File(getDataDir(), "local/alpine/root/skills");

        skillsList = loadSkills();

        // Upload button
        ImageButton uploadButton = findViewById(R.id.btnUploadSkill);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        uploadSkillZip(fileUri);
                    }
                }
        );

        uploadButton.setOnClickListener(v -> openFilePicker());

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerSkills);
        adapter = new SkillsListAdapter(skillsList, (position, skill) -> {
            String folderName = skill.optString("folderName", "");
            if (!folderName.isEmpty()) {
                deleteFolder(new File(skillsDir, folderName));
                adapter.removeItem(position);
                Toast.makeText(this, "Skill deleted", Toast.LENGTH_SHORT).show();
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

    private void uploadSkillZip(Uri zipUri) {
        try (InputStream is = getContentResolver().openInputStream(zipUri)) {
            if (is == null) {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Extract to a temp dir first, then move to skills dir
            File tempDir = new File(getCacheDir(), "skill_upload_" + System.currentTimeMillis());
            ZipUtils.extractZip(is, tempDir);

            // Find the skill.json - it may be at root or inside a single subfolder
            File skillJson = new File(tempDir, "skill.json");
            File actualSkillDir = tempDir;

            if (!skillJson.exists()) {
                // Check if zip has a single root folder
                File[] children = tempDir.listFiles();
                if (children != null && children.length == 1 && children[0].isDirectory()) {
                    actualSkillDir = children[0];
                    skillJson = new File(actualSkillDir, "skill.json");
                }
            }

            if (!skillJson.exists()) {
                Toast.makeText(this, "Invalid skill zip: no skill.json found", Toast.LENGTH_LONG).show();
                deleteFolder(tempDir);
                return;
            }

            // Read skill name from skill.json
            String jsonContent = readFileContent(skillJson);
            JSONObject skillConfig = new JSONObject(jsonContent);
            String skillName = skillConfig.optString("name", actualSkillDir.getName());

            // Move to skills directory
            File destDir = new File(skillsDir, skillName);
            if (destDir.exists()) {
                deleteFolder(destDir);
            }
            skillsDir.mkdirs();

            if (!actualSkillDir.renameTo(destDir)) {
                // Fallback: copy
                copyFolder(actualSkillDir, destDir);
            }
            deleteFolder(tempDir);

            // Make binaries executable
            makeBinariesExecutable(destDir);

            // Refresh list
            JSONObject newSkill = buildSkillJson(destDir);
            if (newSkill != null) {
                adapter.addItem(newSkill);
            }

            Toast.makeText(this, "Skill '" + skillName + "' uploaded", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<JSONObject> loadSkills() {
        List<JSONObject> list = new ArrayList<>();
        if (!skillsDir.exists() || !skillsDir.isDirectory()) return list;

        File[] dirs = skillsDir.listFiles(File::isDirectory);
        if (dirs == null) return list;

        for (File dir : dirs) {
            JSONObject obj = buildSkillJson(dir);
            if (obj != null) {
                list.add(obj);
            }
        }
        return list;
    }

    private JSONObject buildSkillJson(File skillDir) {
        try {
            File skillJson = new File(skillDir, "skill.json");
            JSONObject obj = new JSONObject();
            obj.put("folderName", skillDir.getName());

            if (skillJson.exists()) {
                String content = readFileContent(skillJson);
                JSONObject config = new JSONObject(content);
                obj.put("name", config.optString("name", skillDir.getName()));
                obj.put("description", config.optString("description", ""));
            } else {
                obj.put("name", skillDir.getName());
                obj.put("description", "");
            }

            // Count tools
            File toolsDir = new File(skillDir, "tools");
            int toolCount = 0;
            if (toolsDir.exists() && toolsDir.isDirectory()) {
                File[] tools = toolsDir.listFiles(File::isDirectory);
                if (tools != null) toolCount = tools.length;
            }
            obj.put("toolCount", toolCount);

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

    private void makeBinariesExecutable(File dir) {
        // In each tool folder, make non-json files executable
        File toolsDir = new File(dir, "tools");
        if (!toolsDir.exists()) return;
        File[] tools = toolsDir.listFiles(File::isDirectory);
        if (tools == null) return;
        for (File toolDir : tools) {
            File[] files = toolDir.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (!f.getName().endsWith(".json")) {
                    f.setExecutable(true, false);
                }
            }
        }
    }
}

