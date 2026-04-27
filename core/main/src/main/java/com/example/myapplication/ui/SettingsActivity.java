package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rk.terminal.R;
import com.rk.terminal.service.SshService;
import com.rk.terminal.service.TerminalSessionManager;
import com.rk.terminal.utils.SessionCodes;
import com.termux.terminal.TerminalAsynchronousSessionHandler;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupMenuItem(findViewById(R.id.menuLocalLLM),
                "Local LLM Upload",
                "Upload and manage .gguf model files",
                LocalLLMActivity.class);

        setupMenuItem(findViewById(R.id.menuModelPlugin),
                "Model Plugin",
                "Configure model endpoints, headers and settings",
                ModelPluginListActivity.class);

        setupMenuItem(findViewById(R.id.menuSkills),
                "Skills",
                "Upload and manage agent skills",
                SkillsActivity.class);

        setupMenuItem(findViewById(R.id.menuPublicTools),
                "Public Tools",
                "Upload and manage public tools",
                PublicToolsActivity.class);

        setupMenuItem(findViewById(R.id.menuFormatterPlugin),
                "Formatter Plugin",
                "Manage response formatters",
                FormatterPluginActivity.class);

        setupMenuItem(findViewById(R.id.menuMemoryPlugin),
                "Memory Plugin",
                "Manage memory plugins for agents",
                MemoryPluginActivity.class);

        setupMenuItem(findViewById(R.id.menuLlamaCpp),
                "LlamaCpp",
                "Manage LlamaCpp server, port and model selection",
                LlamaCppActivity.class);

        setupMenuItem(findViewById(R.id.menuAccessTerminal),
                "Access Terminal",
                "Open the terminal emulator",
                TerminalActivity.class);

        Switch switchSsh = findViewById(R.id.switchSsh);
        switchSsh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // TODO: Start SSH service
                Toast.makeText(this, "Starting SSH Server at port 8022", Toast.LENGTH_SHORT).show();

                TerminalAsynchronousSessionHandler sshSession = (TerminalAsynchronousSessionHandler)TerminalSessionManager.getInstance().getSession("ssh");
                SshService.startSSHServer(sshSession);

            } else {

                TerminalAsynchronousSessionHandler sshSession = (TerminalAsynchronousSessionHandler)TerminalSessionManager.getInstance().getSession("ssh");
                SshService.stopSSHServer(sshSession);


            }
        });
    }

    private void setupMenuItem(View container, String title, String description, Class<?> targetActivity) {
        ((TextView) container.findViewById(R.id.txtMenuTitle)).setText(title);
        ((TextView) container.findViewById(R.id.txtMenuDescription)).setText(description);
        if (targetActivity != null) {
            container.setOnClickListener(v -> startActivity(new Intent(this, targetActivity)));
        }
    }
}
