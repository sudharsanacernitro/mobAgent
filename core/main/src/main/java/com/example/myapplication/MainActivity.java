package com.example.myapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import com.example.myapplication.DAOs.ChatMessageStore;
import com.example.myapplication.DAOs.PluginDatabase;
import com.example.myapplication.DAOs.entities.ChatSession;
import com.example.myapplication.DAOs.entities.ModelPlugin;
import com.example.myapplication.DAOs.entities.ModelPluginWithFormatterPath;
import com.example.myapplication.DAOs.entities.MemoryPlugin;
import com.example.myapplication.DAOs.entities.Plugin;
import com.example.myapplication.repo.LlamaCppServerRepo;
import com.example.myapplication.utils.DexLoader;
import com.example.myapplication.utils.PropertiesReader;
import com.rk.TerminalLogger;
import com.rk.terminal.R;
import com.rk.terminal.service.TerminalSessionManager;
import com.rk.terminal.ui.screens.terminal.MkSession;
import com.termux.terminal.TerminalSynchronousSessionHandler;

import com.example.myapplication.ui.SettingsActivity;
import com.example.myapplication.ui.ChatSessionListActivity;
import com.example.myapplication.ui.adapter.ChatMessageAdapter;

import org.json.JSONObject;
import org.mobAgent.plugin.interfaces.FormatterBuilder;
import org.mobAgent.plugin.interfaces.FormatterInterface;
import org.mobAgent.plugin.interfaces.Memory;
import org.mobchain.memory.BuiltInMemory;
import org.mobchain.memory.InMemory;
import org.mobchain.messages.HumanMessages;
import org.mobchain.messages.SystemMessages;
import org.mobchain.models.BuiltInFormatters;
import org.mobchain.models.ModelInterface;
import org.mobchain.skills.SkillsScanner;
import org.mobchain.tools.OwnTools.NativeTools.SpawnAgentTool;
import org.mobchain.tools.ToolsManager;
import org.mobchain.tools.ToolsScanner;

import java.util.Map;




public class MainActivity extends AppCompatActivity {
    private final CountDownLatch serverReady = new CountDownLatch(1);

    private ModelInterface agent = null;
    private volatile boolean alpineReady = false;

    // ── Text-mode views ──────────────────────────────────────────────────
    EditText inputText;
    ImageButton checkServerStatus;
    Button sendMsg;
    ImageButton manageSettings;
    ImageButton btnSessions;
    View inputBar;

    // ── Speech-mode views ────────────────────────────────────────────────
    View speechPanel;
    TextView speechStatus;
    ImageButton btnMic;
    ImageButton btnToggleSpeech;

    // ── Speech mode state ────────────────────────────────────────────────
    private boolean isSpeechMode = false;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private static final int REQUEST_RECORD_AUDIO = 101;

    // ── Session / chat ───────────────────────────────────────────────────
    private int currentSessionId = -1;
    private ActivityResultLauncher<Intent> sessionPickerLauncher;
    RecyclerView recyclerChat;
    ChatMessageAdapter chatAdapter;
    java.util.List<ChatMessageAdapter.ChatMsg> chatMessages = new java.util.ArrayList<>();

    private static Context context;

    public static Context getAppContext() {
        return MainActivity.context;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        context = getApplicationContext();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ── Find views ───────────────────────────────────────────────────
        inputText        = findViewById(R.id.msg);
        sendMsg          = findViewById(R.id.sendMsg);
        checkServerStatus = findViewById(R.id.checkServerStatus);
        manageSettings   = findViewById(R.id.manageSettings);
        btnSessions      = findViewById(R.id.btnSessions);
        inputBar         = findViewById(R.id.inputBar);
        speechPanel      = findViewById(R.id.speechPanel);
        speechStatus     = findViewById(R.id.speechStatus);
        btnMic           = findViewById(R.id.btnMic);
        btnToggleSpeech  = findViewById(R.id.btnToggleSpeech);

        // ── Session picker ───────────────────────────────────────────────
        sessionPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        int sessionId = result.getData().getIntExtra(ChatSessionListActivity.EXTRA_SESSION_ID, -1);
                        if (sessionId != -1) {
                            currentSessionId = sessionId;
                            loadSessionChat(sessionId);
                            reinitAgentForSession(sessionId);
                        }
                    }
                }
        );

        btnSessions.setOnClickListener(v ->
                sessionPickerLauncher.launch(new Intent(this, ChatSessionListActivity.class)));

        // ── RecyclerView ─────────────────────────────────────────────────
        recyclerChat = findViewById(R.id.recyclerChat);
        chatAdapter  = new ChatMessageAdapter(chatMessages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerChat.setLayoutManager(lm);
        recyclerChat.setAdapter(chatAdapter);

        // ── LlamaCpp status ──────────────────────────────────────────────
        LlamaCppServerRepo llamaCppServerRepo = new LlamaCppServerRepo(this);
        int port = Integer.parseInt(PropertiesReader.getProperty(this, "LlamaServerport"));

        checkServerStatus.setOnClickListener(v -> {
            new Thread(() -> {
                boolean online = LlamaCppServerRepo.isServerOnline(port);
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Server Status")
                        .setMessage(online ? "LlamaCpp Server is online!" : "Server is offline")
                        .setPositiveButton("OK", null)
                        .show());
            }).start();
        });

        // ── Send button (text mode) ──────────────────────────────────────
        sendMsg.setOnClickListener(v -> {
            String text = inputText.getText().toString().trim();
            if (text.isEmpty()) return;
            inputText.setText("");
            sendMessage(text);
        });

        // ── Speech toggle ────────────────────────────────────────────────
        btnToggleSpeech.setOnClickListener(v -> toggleSpeechMode());

        // ── Mic button ───────────────────────────────────────────────────
        btnMic.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            }
        });

        // ── TextToSpeech init ────────────────────────────────────────────
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {}
                    @Override public void onDone(String id) {
                        if (isSpeechMode) setSpeechStatus("Tap mic to speak");
                    }
                    @Override public void onError(String id) {
                        if (isSpeechMode) setSpeechStatus("Tap mic to speak");
                    }
                });
            }
        });

        // ── Settings ─────────────────────────────────────────────────────
        manageSettings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        TerminalLogger.init(this);

        // ── Alpine setup dialog (first-run only) ─────────────────────────
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);

        // Dialog refs — valid only when isFirstRun
        final AlertDialog[] dialogRef  = {null};
        final TextView[]    logViewRef = {null};
        final ScrollView[]  scrollRef  = {null};

        if (isFirstRun) {
            // We are on the UI thread inside onCreate — create dialog directly
            TextView logView = new TextView(this);
            logView.setTypeface(Typeface.MONOSPACE);
            logView.setTextSize(11f);
            logView.setPadding(24, 16, 24, 16);

            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(logView);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("🚀 Setting up Alpine…")
                    .setView(scrollView)
                    .setCancelable(false)
                    .setPositiveButton("Done", null)
                    .create();
            dialog.show();

            // Force a small fixed window height (200 dp)
            if (dialog.getWindow() != null) {
                int widthPx = android.view.WindowManager.LayoutParams.MATCH_PARENT;
                int heightPx = (int) (200 * getResources().getDisplayMetrics().density);
                dialog.getWindow().setLayout(widthPx, heightPx);
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            dialogRef[0]  = dialog;
            logViewRef[0] = logView;
            scrollRef[0]  = scrollView;
        }

        // Unified log callback — appends to dialog AND logcat
        Consumer<String> logCallback = line -> {
            System.out.println("[Alpine] " + line);
            runOnUiThread(() -> {
                TextView lv = logViewRef[0];
                ScrollView sv = scrollRef[0];
                if (lv != null) {
                    lv.append(line + "\n");
                    if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
                }
            });
        };

        // Track last-reported download percentage to avoid flooding the log
        final AtomicInteger lastPct = new AtomicInteger(-1);

        AlpineWrapper.setupAlpineAsync(

                // onProgress — called per downloaded chunk on a background thread
                (Function1<Float, Unit>) progress -> {
                    int pct = (int)(progress * 100);
                    if (pct != lastPct.getAndSet(pct)) {
                        logCallback.accept("📥 Downloading Alpine binaries… " + pct + "%");
                    }
                    return Unit.INSTANCE;
                },

                // onComplete
                (Function0<Unit>) () -> {

                    logCallback.accept("✓ Download complete.");
                    System.out.println("port: " + Integer.parseInt(
                            PropertiesReader.getProperty(this, "LlamaServerport")));

                    initiateTerminalEmulator(logCallback);

                    // Initializing sessions
                    TerminalSessionManager.getInstance(this);

                    // Adding native public tools
                    ToolsManager.addTools("root", new SpawnAgentTool());

                    // Scanning public tools
                    ToolsScanner toolsScanner = new ToolsScanner(
                            new File(this.getDataDir(), "local/alpine/root/tools"));
                    toolsScanner.scanAndRegister();

                    // Scanning skills
                    SkillsScanner skillsScanner = new SkillsScanner(
                            new File(this.getDataDir(), "local/alpine/root/skills"));
                    skillsScanner.scanAndRegister();

                    logCallback.accept("✓ Tools and skills loaded.");
                    System.out.println(ToolsManager.getToolsCountBySkill("web-crawler"));
                    System.out.println(ToolsManager.getToolsCountBySkill("root"));

                    alpineReady = true;

                    // If session already has plugins selected, init agent now
                    if (currentSessionId != -1) {
                        reinitAgentForSession(currentSessionId);
                    }

                    // ── Dismiss dialog ────────────────────────────────────
                    runOnUiThread(() -> {
                        AlertDialog d = dialogRef[0];
                        if (d != null && d.isShowing()) {
                            d.setTitle("✅ Alpine Ready!");
                            Button doneBtn = d.getButton(AlertDialog.BUTTON_POSITIVE);
                            doneBtn.setEnabled(true);
                            // Auto-dismiss after 1.5 s so user can read the final log
                            doneBtn.postDelayed(d::dismiss, 1500);
                        }
                    });

                    return Unit.INSTANCE;
                },

                // onError
                (Function1<Exception, Unit>) e -> {
                    logCallback.accept("❌ Setup failed: " + e.getMessage());
                    System.out.println("Setup failed: " + e);
                    runOnUiThread(() -> {
                        AlertDialog d = dialogRef[0];
                        if (d != null && d.isShowing()) {
                            d.setTitle("❌ Setup Failed");
                            d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    });
                    return Unit.INSTANCE;
                }
        );



    }

    // ─────────────────────────────────────────────────────────────────────
    // Shared message-sending logic (text mode + speech mode both use this)
    // ─────────────────────────────────────────────────────────────────────
    private void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        if( currentSessionId == -1 ) {
            autoCreateSessionAndShowDialog();
            return;
        }

        // Show in chat
        chatAdapter.addMessage(new ChatMessageAdapter.ChatMsg("You", text));
        recyclerChat.scrollToPosition(chatMessages.size() - 1);

        // Persist user message
        if (currentSessionId != -1) {
            Executors.newSingleThreadExecutor().execute(() -> {
                ChatMessageStore store = new ChatMessageStore(MainActivity.this, currentSessionId);
                store.saveHumanMessage(text);
            });
        }

        if (agent == null) {
            runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Not Ready")
                    .setMessage("Default agent is not initialized")
                    .setPositiveButton("OK", null)
                    .show());
            return;
        }

        if (isSpeechMode) setSpeechStatus("Thinking...");

        new Thread(() -> {
            String output = agent.chat(new HumanMessages(text));

            // Persist AI message
            if (currentSessionId != -1) {
                ChatMessageStore store = new ChatMessageStore(MainActivity.this, currentSessionId);
                store.saveAiMessage(output);
            }

            runOnUiThread(() -> {
                chatAdapter.addMessage(new ChatMessageAdapter.ChatMsg("AI", output));
                recyclerChat.scrollToPosition(chatMessages.size() - 1);
            });

            // Speak the response in speech mode
            if (isSpeechMode && tts != null) {
                setSpeechStatus("Speaking...");
                tts.speak(output, TextToSpeech.QUEUE_FLUSH, null,
                        "RESPONSE_" + System.currentTimeMillis());
            }

            System.out.println("Agent response: " + output);
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Speech mode toggle
    // ─────────────────────────────────────────────────────────────────────
    private void toggleSpeechMode() {
        isSpeechMode = !isSpeechMode;
        if (isSpeechMode) {
            inputBar.setVisibility(View.GONE);
            speechPanel.setVisibility(View.VISIBLE);
            btnToggleSpeech.setImageResource(R.drawable.ic_keyboard);
            setSpeechStatus("Tap mic to speak");
        } else {
            speechPanel.setVisibility(View.GONE);
            inputBar.setVisibility(View.VISIBLE);
            btnToggleSpeech.setImageResource(R.drawable.ic_mic);
            if (speechRecognizer != null) speechRecognizer.stopListening();
            if (tts != null) tts.stop();
            setSpeechStatus("Tap mic to speak");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Speech recognizer
    // ─────────────────────────────────────────────────────────────────────
    private void startListening() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    setSpeechStatus("Listening...");
                    runOnUiThread(() -> btnMic.setBackgroundResource(R.drawable.mic_button_bg_active));
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {
                    setSpeechStatus("Processing...");
                    runOnUiThread(() -> btnMic.setBackgroundResource(R.drawable.mic_button_bg));
                }
                @Override
                public void onError(int error) {
                    setSpeechStatus("Tap mic to speak");
                    runOnUiThread(() -> btnMic.setBackgroundResource(R.drawable.mic_button_bg));
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches =
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        runOnUiThread(() -> sendMessage(matches.get(0)));
                    } else {
                        setSpeechStatus("Tap mic to speak");
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechRecognizer.startListening(intent);
        setSpeechStatus("Listening...");
    }

    private void setSpeechStatus(String status) {
        runOnUiThread(() -> speechStatus.setText(status));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Runtime permission result
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Microphone permission required for speech mode",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        if (speechRecognizer != null) { speechRecognizer.destroy(); speechRecognizer = null; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Session helpers (unchanged)
    // ─────────────────────────────────────────────────────────────────────
    private void loadSessionChat(int sessionId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            PluginDatabase db = PluginDatabase.getInstance(this);
            ChatSession session = db.chatSessionDao().getById(sessionId);
            if (session == null) {
                runOnUiThread(() -> Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show());
                return;
            }

            // Load chat history for this session
            ChatMessageStore store = new ChatMessageStore(this, sessionId);
            java.util.List<com.example.myapplication.DAOs.entities.ChatMessage> messages =
                    db.chatMessageDao().getBySessionId(sessionId);

            runOnUiThread(() -> {
                chatMessages.clear();
                for (com.example.myapplication.DAOs.entities.ChatMessage msg : messages) {
                    String sender = "user".equals(msg.role) ? "You" : "AI";
                    chatMessages.add(new ChatMessageAdapter.ChatMsg(sender, msg.content));
                }
                chatAdapter.notifyDataSetChanged();
                if (!chatMessages.isEmpty()) {
                    recyclerChat.scrollToPosition(chatMessages.size() - 1);
                }
            });
        });
    }

    private void autoCreateSessionAndShowDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            PluginDatabase db = PluginDatabase.getInstance(this);

            String sessionName = "Chat " + new java.text.SimpleDateFormat("MMM dd HH:mm",
                    java.util.Locale.getDefault()).format(new java.util.Date());

            // Check if any model plugins exist to satisfy FK
            List<ModelPlugin> modelPlugins = db.modelPluginDao().getAll();
            int defaultModelId = modelPlugins.isEmpty() ? 0 : modelPlugins.get(0).getPluginId();

            try {
                ChatSession session = new ChatSession(sessionName, defaultModelId, null, System.currentTimeMillis());
                long id = db.chatSessionDao().insert(session);
                currentSessionId = (int) id;
                System.out.println("Auto-created session: " + sessionName + " (id=" + id + ")");
            } catch (Exception e) {
                System.out.println("Auto-create session failed: " + e.getMessage());
            }

            runOnUiThread(this::showPluginSelectionDialog);
        });
    }

    private void showPluginSelectionDialog() {
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

            // Built-in memory implementations (loaded from code, no upload required)
            for (Map.Entry<Integer, String> entry : BuiltInMemory.getAll().entrySet()) {
                memoryNames.add(entry.getValue());
                memoryIds.add(entry.getKey());
            }

            // User-uploaded memory plugins
            for (MemoryPlugin mem : memoryPlugins) {
                Plugin p = db.pluginDao().getById(mem.getPluginId());
                memoryNames.add(p != null ? p.getName() : "Unknown");
                memoryIds.add(mem.getPluginId());
            }

            runOnUiThread(() -> buildPluginSelectionDialog(modelNames, modelIds, memoryNames, memoryIds));
        });
    }

    private void buildPluginSelectionDialog(List<String> modelNames, List<Integer> modelIds,
                                             List<String> memoryNames, List<Integer> memoryIds) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_plugins, null);
        Spinner spinnerModel  = dialogView.findViewById(R.id.spinnerModelPlugin);
        Spinner spinnerMemory = dialogView.findViewById(R.id.spinnerMemoryPlugin);
        Button btnSettings    = dialogView.findViewById(R.id.btnGoToSettings);

        List<String>  displayModelNames = new ArrayList<>(modelNames);
        List<Integer> displayModelIds   = new ArrayList<>(modelIds);
        if (displayModelNames.isEmpty()) {
            displayModelNames.add("No model plugins — add in Settings");
            displayModelIds.add(-1);
        }

        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, displayModelNames);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);

        ArrayAdapter<String> memoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, memoryNames);
        memoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMemory.setAdapter(memoryAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Plugins for Session")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Confirm", (d, which) -> {
                    int modelPos  = spinnerModel.getSelectedItemPosition();
                    int memoryPos = spinnerMemory.getSelectedItemPosition();

                    int selectedModelId    = displayModelIds.get(modelPos);
                    Integer selectedMemoryId = memoryIds.get(memoryPos);

                    if (selectedModelId == -1) {
                        Toast.makeText(this, "No model plugin available. Add one in Settings first.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Store null in DB for built-in memory (FK constraint),
                    // but pass the sentinel ID to initAgent so it resolves correctly.
                    Integer memoryIdForDb    = (selectedMemoryId != null && BuiltInMemory.isBuiltIn(selectedMemoryId))
                            ? null : selectedMemoryId;
                    Integer memoryIdForAgent = selectedMemoryId;

                    int finalModelId = selectedModelId;
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (currentSessionId != -1) {
                            PluginDatabase.getInstance(this).chatSessionDao()
                                    .updatePlugins(currentSessionId, finalModelId, memoryIdForDb);
                        }
                        if (alpineReady) {
                            initAgent(finalModelId, memoryIdForAgent);
                        }
                    });
                })
                .setNegativeButton("Skip", null)
                .create();

        btnSettings.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });

        dialog.show();
    }

    /**
     * Re-initializes the agent based on the session's stored plugin IDs.
     */
    private void reinitAgentForSession(int sessionId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            PluginDatabase db = PluginDatabase.getInstance(this);
            ChatSession session = db.chatSessionDao().getById(sessionId);
            if (session == null || session.getModelPluginId() == 0) return;
            initAgent(session.getModelPluginId(), session.getMemoryPluginId());
        });
    }

    /**
     * Initializes the agent with the given model and memory plugin IDs.
     */
    private void initAgent(int modelPluginId, Integer memoryPluginId) {
        try {
            PluginDatabase db = PluginDatabase.getInstance(this);
            ChatMessageStore chatMessageStore = new ChatMessageStore(this, currentSessionId);

            // Resolve memory from memoryPluginId
            Memory memory;
            if (memoryPluginId == null || BuiltInMemory.isBuiltIn(memoryPluginId)) {
                // Use the built-in InMemory (default)
                int builtInId = (memoryPluginId != null) ? memoryPluginId : BuiltInMemory.IN_MEMORY_ID;
                memory = BuiltInMemory.getInstance(builtInId, chatMessageStore);
            } else {
                // TODO: Load custom memory plugin via DEX when supported
                // For now, fall back to built-in InMemory
                memory = new InMemory(chatMessageStore);
            }

            if (memory == null) {
                System.out.println("Failed to resolve memory for pluginId: " + memoryPluginId);
                return;
            }

            memory.setSystemPrompt(new SystemMessages(
                    "you are a root agent able to spawn subagents with skills using the tool ***spawn_agent*** . " +
                            "If the user asks about common question ,answer on your own without spawning agent/calling tools ."+
                    "subagents spawn with skills are specialized agent . At first search for suitable skill for the user input, " +
                    "if any skills match( relevent more than 50%) then you should spawn agent . If there is no suck skills then " +
                    "only look for other tools.IMPORTANT : MOSTLY TRY TO SPAWN AGENT WITH SPECILIZED SKILL."));

            ModelPluginWithFormatterPath defaultModel = db.modelPluginDao().getModelPluginWithFormatterPath(modelPluginId);
            if (defaultModel == null) {
                System.out.println("Model plugin not found for pluginId: " + modelPluginId);
                return;
            }

            // formatterId == null means the user chose the built-in formatter (stored as null
            // in DB to avoid FK violation). Resolve: null → BuiltInFormatters.OPENAI_FORMATTER_ID.
            int formatterId = (defaultModel.getModelPlugin().formatterId != null)
                    ? defaultModel.getModelPlugin().formatterId
                    : BuiltInFormatters.OPENAI_FORMATTER_ID;

            DexLoader dexLoader = new DexLoader(this);
            FormatterBuilder formatterBuilder = dexLoader.loadFormatter(formatterId);
            if (formatterBuilder == null) {
                System.out.println("Failed to load formatter plugin");
                return;
            }

            // Load config headers from DB and apply to formatter builder
            List<com.example.myapplication.DAOs.entities.ConfigHeader> configHeaders =
                    db.configHeaderDao().getByConfigId(modelPluginId);
            formatterBuilder
                    .baseURL(defaultModel.getModelPlugin().getApiUrl())
                    .model(defaultModel.getModelPlugin().getModelName());
            for (com.example.myapplication.DAOs.entities.ConfigHeader h : configHeaders) {
                formatterBuilder.addHeader(h.getHeaderKey(), h.getHeaderValue());
            }
            FormatterInterface formatterInterface = formatterBuilder.build();

            agent = new ModelInterface.Builder()
                    .setModel(formatterInterface)
                    .setMemory(memory)
                    .addTools(ToolsManager.getToolsBySkill("root"))
                    .build();

            System.out.println("Agent initiated for session " + currentSessionId);
        } catch (Exception e) {
            System.out.println("Agent init failed (non-fatal): " + e.getMessage());
        }
    }

    private void initiateTerminalEmulator(Consumer<String> logCallback) {

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);

        TerminalSynchronousSessionHandler headlessSession =
                (TerminalSynchronousSessionHandler) MkSession.INSTANCE.createSession(this, "1", 0, 0);

        if (isFirstRun) {
            logCallback.accept("Starting Alpine environment…");
            headlessSession.initializeAlpine(logCallback);

            try {
                moveSetupFiles();
                logCallback.accept("✓ Setup files copied.");
            } catch (IOException e) {
                logCallback.accept("⚠ Setup files error: " + e.getMessage());
                System.out.println("Error moving setup files: " + e.getMessage());
            }

            System.out.println("TerminalInitiated");
            prefs.edit().putBoolean("is_first_run", false).apply();
        }
    }

    private boolean moveSetupFiles() throws IOException {

        List<String> fileNames = Arrays.asList("llamaCppServerSetup.sh", "sshServerSetup.sh", "ToolsWrapper.sh");

        for (String assetName : fileNames) {

            String rootDirRelative = PropertiesReader.getProperty(context, "rootDirFromLocalDir");
            File rootDir = new File(context.getDataDir(), rootDirRelative);
            if (!rootDir.exists()) rootDir.mkdirs();

            File destinationFile = new File(rootDir, assetName);

            if (destinationFile.exists()) {
                System.out.println("File already exists locally. Skipping copy.");
                return true;
            }

            String setupFilePath = destinationFile.getAbsolutePath();

            for (String file : context.getAssets().list("")) {
                System.out.println(file + " equals: " + file.equals(assetName));
            }

            try (InputStream in = context.getAssets().open(assetName)) {
                try (OutputStream out = new FileOutputStream(setupFilePath)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    System.out.println("Asset copied successfully.");
                } catch (Exception e) {
                    System.out.println("destination folder error: " + e.getMessage());
                    return false;
                }
            } catch (IOException e) {
                System.out.println("Asset not found ");
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }
}
