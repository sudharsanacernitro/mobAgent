package org.mobchain.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mobchain.tools.OwnTools.TerminalTool;
import org.mobAgent.plugin.interfaces.Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * ToolsScanner - Plug & Play tool discovery engine.
 *
 * Scans a root tools directory where each subdirectory represents a tool.
 * Each tool directory must contain:
 *   - config.json  → tool metadata (name, description, binary, structuredTool)
 *   - <binary>     → the executable referenced by config.json
 *
 * Example directory layout:
 *   alpine/root/tools/
 *   ├── web_search/
 *   │   ├── config.json
 *   │   └── web_search          ← binary
 *   └── file_reader/
 *       ├── config.json
 *       └── file_reader          ← binary
 *
 * Example config.json:
 * {
 *   "name": "web_search",
 *   "description": "Searches the web for a given query",
 *   "binary": "web_search",
 *   "structuredTool": {
 *       "type": "function",
 *       "function": {
 *           "name": "web_search",
 *           "description": "Searches the web",
 *           "parameters": { ... }
 *       }
 *   }
 * }
 */
public class ToolsScanner {

    private static final String TAG = "ToolsScanner";
    private static final String CONFIG_FILE = "config.json";

    private final File toolsRootDir;

    /**
     * @param toolsRootDir absolute path to the tools root directory
     *                      e.g. new File(filesDir, "alpine/root/tools")
     */
    public ToolsScanner(File toolsRootDir) {
        this.toolsRootDir = toolsRootDir;
    }

    /**
     * Convenience constructor that accepts a string path.
     */
    public ToolsScanner(String toolsRootPath) {
        this(new File(toolsRootPath));
    }


    // ─────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────

    /**
     * Scans the tools directory and registers every valid tool
     * directly into the static {@link ToolsManager}.
     *
     * @return the number of tools successfully registered
     */
    public int scanAndRegister() {

        if (!toolsRootDir.exists() || !toolsRootDir.isDirectory()) {
            System.err.println(TAG + ": Tools directory does not exist → " + toolsRootDir.getAbsolutePath());
            return 0;
        }

        File[] toolDirs = toolsRootDir.listFiles(File::isDirectory);

        if (toolDirs == null || toolDirs.length == 0) {
            System.out.println(TAG + ": No tool directories found in → " + toolsRootDir.getAbsolutePath());
            return 0;
        }

        String[] paths = toolsRootDir.getAbsolutePath().split("/");

        int registered = 0;

        for (File toolDir : toolDirs) {
            try {
                Tool tool = loadTool( paths[paths.length-2] , toolDir);

                if (tool != null) {
                    ToolsManager.addTools( paths[paths.length-2] , tool );
                    registered++;
                    System.out.println(TAG + ": Registered tool → " + tool.getToolName());
                }

            } catch (Exception e) {
                System.err.println(TAG + ": Failed to load tool from " + toolDir.getName() + " → " + e.getMessage());
            }
        }

        System.out.println(TAG + ": Scan complete. " + registered + "/" + toolDirs.length + " tools registered.");
        return registered;
    }

    /**
     * Scans the tools directory and returns a list of discovered tools
     * WITHOUT registering them (useful for preview / dry-run).
     */
//    public List<Tool> scanTools() {

//        List<Tool> tools = new ArrayList<>();
//
//        if (!toolsRootDir.exists() || !toolsRootDir.isDirectory()) {
//            return tools;
//        }
//
//        File[] toolDirs = toolsRootDir.listFiles(File::isDirectory);
//
//        if (toolDirs == null) return tools;
//
//        for (File toolDir : toolDirs) {
//            try {
//                Tool tool = loadTool(toolDir);
//                if (tool != null) {
//                    tools.add(tool);
//                }
//            } catch (Exception e) {
//                System.err.println(TAG + ": Skipping " + toolDir.getName() + " → " + e.getMessage());
//            }
//        }
//
//        return tools;
//    }


    // ─────────────────────────────────────────────
    //  Internal
    // ─────────────────────────────────────────────

    /**
     * Loads a single tool from its directory.
     *
     * @param toolDir directory that contains config.json + binary
     * @return a fully constructed {@link TerminalTool}, or null if invalid
     */
    private Tool loadTool(String skillName , File toolDir) throws Exception {

        File configFile = new File(toolDir, CONFIG_FILE);

        if (!configFile.exists()) {
            System.err.println(TAG + ": Missing " + CONFIG_FILE + " in → " + toolDir.getName());
            return null;
        }

        // ── Parse config.json ──
        String configContent = readFileToString(configFile);
        JSONObject config = new JSONObject(configContent);

        String toolName    = config.getString("name");
        String description = config.getString("description");
        JSONArray requiredParams = config.getJSONArray("requiredParams");

        JSONObject structuredTool = config.getJSONObject("structuredTool");

        // ── Validate binary exists ──
        File binaryFile = new File(toolDir, toolName);

        if (!binaryFile.exists()) {
            System.err.println(TAG + ": Binary '" + toolName + "' not found in → " + toolDir.getName());
            return null;
        }



        String toolPath = binaryFile.getAbsolutePath();

        return new TerminalTool(toolName, toolPath, description, structuredTool , requiredParams , skillName );
    }


    /**
     * Reads an entire file into a String.
     */
    private String readFileToString(File file) throws IOException {

        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }

}
