package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.repo.LlamaCppServerRepo;
import com.example.myapplication.utils.PropertiesReader;
import com.rk.terminal.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LlamaCppActivity extends AppCompatActivity {

    private TextView txtServerStatus;
    private EditText edtPort;
    private Spinner spinnerModels;
    private TextView txtSelectedModel;
    private int currentPort;
    private String selectedModel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_llamacpp);

        txtServerStatus = findViewById(R.id.txtServerStatus);
        Button btnCheckStatus = findViewById(R.id.btnCheckStatus);
        Button btnStartServer = findViewById(R.id.btnStartServer);
        Button btnStopServer = findViewById(R.id.btnStopServer);
        Button btnDownloadBinaries = findViewById(R.id.btnDownloadBinaries);
        edtPort = findViewById(R.id.edtPort);
        Button btnSavePort = findViewById(R.id.btnSavePort);
        spinnerModels = findViewById(R.id.spinnerModels);
        txtSelectedModel = findViewById(R.id.txtSelectedModel);

        // Load current port from properties
        String portStr = PropertiesReader.getProperty(this, "LlamaServerport");
        currentPort = (portStr != null) ? Integer.parseInt(portStr) : 8080;
        edtPort.setText(String.valueOf(currentPort));

        // Load models from llmFilePath
        loadModels();

        // Check server status
        btnCheckStatus.setOnClickListener(v -> {
            txtServerStatus.setText("Status: Checking...");
            new Thread(() -> {
                boolean online = LlamaCppServerRepo.isServerOnline(currentPort);
                runOnUiThread(() -> {
                    if (online) {
                        txtServerStatus.setText("Status: ● Online");
                        txtServerStatus.setTextColor(0xFF4CAF50);
                    } else {
                        txtServerStatus.setText("Status: ● Offline");
                        txtServerStatus.setTextColor(0xFFF44336);
                    }
                });
            }).start();
        });

        // Start server
        btnStartServer.setOnClickListener(v -> {
            if (selectedModel == null || selectedModel.isEmpty()) {
                Toast.makeText(this, "Please select a model first", Toast.LENGTH_SHORT).show();
                return;
            }

            LlamaCppServerRepo repo = LlamaCppServerRepo.getInstance();
            if (repo == null) {
                Toast.makeText(this, "LlamaCpp repo not initialized", Toast.LENGTH_SHORT).show();
                return;
            }

            // Strip .gguf extension for the server command — use full filename
            repo.startLlama(currentPort, selectedModel);
            Toast.makeText(this, "Starting server on port " + currentPort + " with " + selectedModel, Toast.LENGTH_SHORT).show();
        });

        // Stop server
        btnStopServer.setOnClickListener(v -> {
            LlamaCppServerRepo repo = LlamaCppServerRepo.getInstance();
            if (repo != null) {
                repo.stopLlama();
                Toast.makeText(this, "Server stopped", Toast.LENGTH_SHORT).show();
                txtServerStatus.setText("Status: ● Offline");
                txtServerStatus.setTextColor(0xFFF44336);
            }
        });

        // Download binaries
        btnDownloadBinaries.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Download LlamaCpp Binaries")
                    .setMessage("If LlamaCpp is already installed, this will download the binaries and overwrite the existing ones. Do you want to proceed?")
                    .setPositiveButton("Proceed", (dialog, which) -> {
                        LlamaCppServerRepo repo = LlamaCppServerRepo.getInstance();
                        if (repo == null) {
                            Toast.makeText(this, "LlamaCpp repo not initialized", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(this, "Downloading binaries...", Toast.LENGTH_SHORT).show();
                        new Thread(() -> {
                            String rootDir = PropertiesReader.getProperty(this, "rootDirFromLocalDir");
                            String llamaCppDir = PropertiesReader.getProperty(this, "LlamaCppDir");
                            boolean success = repo.downloadLlamaCppBinaries(rootDir, llamaCppDir);
                            runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(this, "Binaries downloaded successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to download binaries", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Save port
        btnSavePort.setOnClickListener(v -> {
            String newPortStr = edtPort.getText().toString().trim();
            if (newPortStr.isEmpty()) {
                Toast.makeText(this, "Port cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int newPort = Integer.parseInt(newPortStr);
                if (newPort < 1 || newPort > 65535) {
                    Toast.makeText(this, "Port must be between 1 and 65535", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentPort = newPort;
                Toast.makeText(this, "Port set to " + currentPort, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
            }
        });

        // Model selection
        spinnerModels.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedModel = (String) parent.getItemAtPosition(position);
                txtSelectedModel.setText("Selected: " + selectedModel);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedModel = null;
                txtSelectedModel.setText("Selected: none");
            }
        });
    }

    private void loadModels() {
        String llmFilePath = PropertiesReader.getProperty(this, "llmFilePath");
        // llmFilePath is relative to alpine root, e.g. /root/llmModels
        // Resolve to actual path: {dataDir}/local/alpine{llmFilePath}
        File modelsDir = new File(getDataDir(), "local/alpine" + llmFilePath);

        List<String> modelNames = new ArrayList<>();

        if (modelsDir.exists() && modelsDir.isDirectory()) {
            File[] files = modelsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".gguf")) {
                        modelNames.add(f.getName());
                    }
                }
            }
        }

        if (modelNames.isEmpty()) {
            modelNames.add("No .gguf models found");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modelNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModels.setAdapter(adapter);
    }
}

