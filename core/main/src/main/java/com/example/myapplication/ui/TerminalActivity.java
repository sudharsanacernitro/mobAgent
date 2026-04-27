package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rk.terminal.R;
import com.rk.terminal.service.TerminalSessionManager;
import com.termux.terminal.TerminalAsynchronousSessionHandler;

public class TerminalActivity extends AppCompatActivity {

    private TextView terminalOutput;
    private EditText terminalInput;
    private ScrollView scrollView;
    private TerminalAsynchronousSessionHandler session;
    private final StringBuilder outputBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        terminalOutput = findViewById(R.id.terminalOutput);
        terminalInput = findViewById(R.id.terminalInput);
        scrollView = findViewById(R.id.terminalScrollView);

        // Get user terminal session
        try {
            session = (TerminalAsynchronousSessionHandler)
                    TerminalSessionManager.getInstance().getSession("user_terminal_session");
        } catch (Exception e) {
            terminalOutput.setText("Error: Could not get terminal session.\n" + e.getMessage());
            return;
        }

        // Set output listener
        session.setOutputListener(text -> runOnUiThread(() -> {
            outputBuffer.append(text);
            // Keep buffer reasonable
            if (outputBuffer.length() > 50000) {
                outputBuffer.delete(0, outputBuffer.length() - 40000);
            }
            terminalOutput.setText(outputBuffer);
            scrollToBottom();
        }));

        // Handle input
        terminalInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendCommand();
                return true;
            }
            return false;
        });

        terminalInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                sendCommand();
                return true;
            }
            return false;
        });
    }

    private void sendCommand() {
        String cmd = terminalInput.getText().toString();
        terminalInput.setText("");

        // Echo command
        outputBuffer.append("$ ").append(cmd).append("\n");
        terminalOutput.setText(outputBuffer);
        scrollToBottom();

        // Write to PTY with newline
        String cmdWithNewline = cmd + "\n";
        byte[] bytes = cmdWithNewline.getBytes();
        session.write(bytes, bytes.length);
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (session != null) {
            session.setOutputListener(null);
        }
    }
}

