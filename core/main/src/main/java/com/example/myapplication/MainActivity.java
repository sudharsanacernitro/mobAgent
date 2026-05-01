package com.example.myapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

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
import org.mobchain.models.ModelInterface;

import org.mobchain.skills.SkillsScanner;
import org.mobchain.tools.OwnTools.NativeTools.SpawnAgentTool;
import org.mobchain.tools.ToolsManager;
import org.mobchain.tools.ToolsScanner;

import java.util.Map;




public class MainActivity extends AppCompatActivity {
    private final CountDownLatch serverReady = new CountDownLatch(1);

    private  ModelInterface agent = null;
    private volatile boolean alpineReady = false;

    EditText inputText;
    ImageButton checkServerStatus;
    Button sendMsg;

    ImageButton manageSettings;

    ImageButton btnSessions;
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


        inputText = findViewById(R.id.msg);
        sendMsg = findViewById(R.id.sendMsg);
        checkServerStatus = findViewById(R.id.checkServerStatus);
        manageSettings = findViewById(R.id.manageSettings);
        btnSessions = findViewById(R.id.btnSessions);

        // Session picker launcher
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

        recyclerChat = findViewById(R.id.recyclerChat);
        chatAdapter = new ChatMessageAdapter(chatMessages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerChat.setLayoutManager(lm);
        recyclerChat.setAdapter(chatAdapter);

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


        sendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String inputToLlm = inputText.getText().toString();
                inputText.setText("");

                // Add user message to chat UI
                chatAdapter.addMessage(new ChatMessageAdapter.ChatMsg("You", inputToLlm));
                recyclerChat.scrollToPosition(chatMessages.size() - 1);

                // Save user message to DB
                if (currentSessionId != -1) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        ChatMessageStore store = new ChatMessageStore(MainActivity.this, currentSessionId);
                        store.saveHumanMessage(inputToLlm);
                    });
                }

                if( agent == null ) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Popup Title")
                                    .setMessage("Default agent is not initialized")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });

                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        String output = agent.chat(new HumanMessages(inputToLlm));

                        // Save AI message to DB
                        if (currentSessionId != -1) {
                            ChatMessageStore store = new ChatMessageStore(MainActivity.this, currentSessionId);
                            store.saveAiMessage(output);
                        }

                        runOnUiThread(() -> {
                            chatAdapter.addMessage(new ChatMessageAdapter.ChatMsg("AI", output));
                            recyclerChat.scrollToPosition(chatMessages.size() - 1);
                        });

                        System.out.println("Button clicked! : " + output);
                    }
                }).start();

            }
        });

        manageSettings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));




        TerminalLogger.init(this);

        // Auto-create a new session on every app launch and show plugin selection dialog
        autoCreateSessionAndShowDialog();

        AlpineWrapper.setupAlpineAsync(

                // onProgress
                (Function1<Float, Unit>) progress -> {
                    System.out.println("Download progress: " + (int)(progress * 100) + "%");
                    return Unit.INSTANCE;
                },

                // onComplete
                (Function0<Unit>) () -> {

                    System.out.println("port: "+Integer.parseInt(PropertiesReader.getProperty( this , "LlamaServerport" )));

                    initiateTerminalEmulator();


                    //initializing sessions
                    TerminalSessionManager.getInstance(this);

                    //starting llama cpp server session
//                    llamaCppServerRepo.runServer(port,"model");



                    //adding Native public tools
                    ToolsManager.addTools("root", new SpawnAgentTool());

                    //scanning public tools
                    ToolsScanner toolsScanner = new ToolsScanner(new File(this.getDataDir(), "local/alpine/root/tools"));
                    toolsScanner.scanAndRegister();

                    //scanning skills
                    SkillsScanner skillsScanner = new SkillsScanner(new File(this.getDataDir(), "local/alpine/root/skills"));
                    skillsScanner.scanAndRegister();

                    System.out.println( ToolsManager.getToolsCountBySkill("web-crawler") );
                    System.out.println( ToolsManager.getToolsCountBySkill("root") );

                    alpineReady = true;

                    // If session already has plugins selected, init agent now
                    if (currentSessionId != -1) {
                        reinitAgentForSession(currentSessionId);
                    }


                    return Unit.INSTANCE;
                },

                // onError
                (Function1<Exception, Unit>) e -> {
                    System.out.println("Setup failed: " + e);
                    return Unit.INSTANCE;
                }
        );



    }



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

    /**
     * Auto-creates a new chat session and shows the plugin selection dialog.
     */
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

    /**
     * Shows a dialog for the user to select model and memory plugins for the current session.
     */
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
        Spinner spinnerModel = dialogView.findViewById(R.id.spinnerModelPlugin);
        Spinner spinnerMemory = dialogView.findViewById(R.id.spinnerMemoryPlugin);
        Button btnSettings = dialogView.findViewById(R.id.btnGoToSettings);

        List<String> displayModelNames = new ArrayList<>(modelNames);
        List<Integer> displayModelIds = new ArrayList<>(modelIds);
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
                    int modelPos = spinnerModel.getSelectedItemPosition();
                    int memoryPos = spinnerMemory.getSelectedItemPosition();

                    int selectedModelId = displayModelIds.get(modelPos);
                    Integer selectedMemoryId = memoryIds.get(memoryPos);

                    if (selectedModelId == -1) {
                        Toast.makeText(this, "No model plugin available. Add one in Settings first.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Store null in DB for built-in memory (FK constraint),
                    // but pass the sentinel ID to initAgent so it resolves correctly.
                    Integer memoryIdForDb = (selectedMemoryId != null && BuiltInMemory.isBuiltIn(selectedMemoryId))
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
                    "subagents spawn with skills are specialized agent . At first search for suitable skill for the user input, " +
                    "if any skills match( relevent more than 50%) then you should spawn agent . If there is no suck skills then " +
                    "only look for other tools.IMPORTANT : MOSTLY TRY TO SPAWN AGENT WITH SPECILIZED SKILL."));

            ModelPluginWithFormatterPath defaultModel = db.modelPluginDao().getModelPluginWithFormatterPath(modelPluginId);
            if (defaultModel == null || defaultModel.getModelPlugin().formatterId == null) {
                System.out.println("Model plugin or formatter not found for pluginId: " + modelPluginId);
                return;
            }

            DexLoader dexLoader = new DexLoader(this);
            FormatterBuilder formatterBuilder = dexLoader.loadFormatter(defaultModel.getModelPlugin().formatterId);
            if (formatterBuilder == null) {
                System.out.println("Failed to load formatter plugin");
                return;
            }

            FormatterInterface formatterInterface = formatterBuilder
                    .baseURL(defaultModel.getModelPlugin().getApiUrl())
                    .model(defaultModel.getModelPlugin().getModelName())
                    .build();

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

    private void initiateTerminalEmulator()  {

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        boolean isFirstRun = prefs.getBoolean("is_first_run", true);

        TerminalSynchronousSessionHandler headlessSession =
                (TerminalSynchronousSessionHandler) MkSession.INSTANCE.createSession( this, "1", 0 , 0);

        if (isFirstRun) {

            headlessSession.initializeAlpine();

            try {
                moveSetupFiles();
            } catch (IOException e) {
                System.out.println("Error moving setup files: " + e.getMessage());
            }

            System.out.println("TerminalInitiated");

            prefs.edit().putBoolean("is_first_run", false).apply();

        }

    }

    private boolean moveSetupFiles( ) throws IOException {

         List<String> fileNames = Arrays.asList("llamaCppServerSetup.sh","sshServerSetup.sh","ToolsWrapper.sh");

         for( String assetName : fileNames) {

             String rootDirPath = PropertiesReader.getProperty(context, "rootDirFromLocalDir");

             File destinationFile = new File(rootDirPath + assetName);

             // If already exists locally, skip copying
             if (destinationFile.exists()) {
                 System.out.println("File already exists locally. Skipping copy.");
                 return true;
             }

             String setupFilePath = rootDirPath + assetName;

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













//
//
//
//package com.example.myapplication;
//
//import android.os.Bundle;
//import android.util.Log;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.lang.reflect.Method;
//
//import dalvik.system.DexClassLoader;
//
//public class MainActivity extends AppCompatActivity {
//
//    private static final String TAG = "DEX_TEST";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        try {
//            // 1️⃣ Copy plugin.jar to internal storage
//            File pluginJar = copyPluginFromAssets("plugin.jar");
//
//            // 2️⃣ Load plugin
//            runPlugin(pluginJar);
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error running plugin", e);
//        }
//    }
//
//    // -------------------------------
//    // Copy plugin.jar into app storage
//    // -------------------------------
//    private File copyPluginFromAssets(String assetName) throws Exception {
//        File outFile = new File(getFilesDir(), assetName);
//
//        if (outFile.exists()) {
//            return outFile; // already copied
//        }
//
//        InputStream is = getAssets().open(assetName);
//        FileOutputStream fos = new FileOutputStream(outFile);
//
//        byte[] buffer = new byte[4096];
//        int read;
//        while ((read = is.read(buffer)) != -1) {
//            fos.write(buffer, 0, read);
//        }
//
//        is.close();
//        fos.close();
//
//        return outFile;
//    }
//
//    // -------------------------------
//    // Load & execute class from DEX
//    // -------------------------------
//    private void runPlugin(File pluginJar) throws Exception {
//
//        File optimizedDir = getDir("dex_opt", MODE_PRIVATE);
//
//        DexClassLoader classLoader = new DexClassLoader(
//                pluginJar.getAbsolutePath(),
//                optimizedDir.getAbsolutePath(),
//                null,
//                getClassLoader()
//        );
//
//        // 1️⃣ Load class
//        Class<?> clazz = classLoader.loadClass("org.example.Main");
//
//        // 2️⃣ Create instance (because method is NOT static)
//        Object instance = clazz.getDeclaredConstructor().newInstance();
//
//        // 3️⃣ Get method with parameter type
//        Method method = clazz.getMethod("Execute", String.class);
//
//        // 4️⃣ Invoke with instance + argument
//        String result = (String) method.invoke(instance, "Executed Successfully");
//
//        Log.d(TAG, "Plugin result: " + result);
//    }
//
//}
//
