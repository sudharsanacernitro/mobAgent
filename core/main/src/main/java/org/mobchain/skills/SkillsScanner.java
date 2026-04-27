package org.mobchain.skills;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.mobAgent.plugin.interfaces.Tool;
import org.mobchain.tools.ToolsManager;
import org.mobchain.tools.ToolsScanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * SkillsScanner - Discovers and registers skills from a skills directory.
 *
 * Expected folder layout:
 *   skills/
 *   ├── researcher/
 *   │   ├── skills.json
 *   │   └── tools/
 *   │       ├── summarizer/
 *   │       │   ├── config.json
 *   │       │   └── summarizer        ← binary
 *   │       └── ...
 *   └── coder/
 *       ├── skills.json
 *       └── tools/
 *           └── ...
 *
 * Each skills.json:
 * {
 *     "name"         : "researcher",
 *     "overview"     : "...",
 *     "privateTools" : ["summarizer"],
 *     "publicTools"  : ["searchTool"],
 *     "description"  : "..."
 * }
 *
 * - privateTools are scanned from the skill's own "tools/" folder using {@link ToolsScanner}.
 * - publicTools must already be registered under the "root" skill and are verified via
 *   {@link ToolsManager#isToolAvailableInRoot(String)}.
 */
public class SkillsScanner {

    private static final String TAG          = "SkillsScanner";
    private static final String SKILLS_JSON  = "skill.json";
    private static final String TOOLS_FOLDER = "tools";

    private final File skillsRootDir;


    public SkillsScanner(File skillsRootDir) {
        this.skillsRootDir = skillsRootDir;
    }

    public SkillsScanner(String skillsRootPath) {
        this(new File(skillsRootPath));
    }


    // ─────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────

    /**
     * Scans every sub-directory inside the skills root folder.
     * For each valid skill:
     *   1. Parses skills.json
     *   2. Scans private tools via ToolsScanner  → registers in ToolsManager
     *   3. Resolves public tools from root        → validates via ToolsManager
     *   4. Registers the Skill in ToolsManager.skillsRegistry
     *
     * @return number of skills successfully registered
     */
    public int scanAndRegister() {

        if (!skillsRootDir.exists() || !skillsRootDir.isDirectory()) {
            System.err.println(TAG + ": Skills directory does not exist → " + skillsRootDir.getAbsolutePath());
            return 0;
        }

        File[] skillDirs = skillsRootDir.listFiles(File::isDirectory);

        if (skillDirs == null || skillDirs.length == 0) {
            System.out.println(TAG + ": No skill directories found in → " + skillsRootDir.getAbsolutePath());
            return 0;
        }

        int registered = 0;

        for (File skillDir : skillDirs) {
            try {
                Skill skill = processSkillDirectory(skillDir);
                if (skill != null) {
                    ToolsManager.addSkill(skill);
                    registered++;
                    System.out.println(TAG + ": Registered skill → " + skill.getName()
                            + " (private=" + skill.getResolvedPrivateTools().size()
                            + ", public=" + skill.getResolvedPublicTools().size() + ")");
                }
            } catch (Exception e) {
                System.err.println(TAG + ": Failed to load skill from " + skillDir.getName() + " → " + e.getMessage());
            }
        }

        System.out.println(TAG + ": Scan complete. " + registered + "/" + skillDirs.length + " skills registered.");
        return registered;
    }


    // ─────────────────────────────────────────────
    //  Internal
    // ─────────────────────────────────────────────

    /**
     * Processes a single skill directory.
     *
     * @param skillDir e.g. skills/researcher/
     * @return fully resolved Skill, or null if invalid
     */
    private Skill processSkillDirectory(File skillDir) throws Exception {

        String dirName = skillDir.getName();

        // ── 1. Parse skills.json ──
        File configFile = new File(skillDir, SKILLS_JSON);
        if (!configFile.exists()) {
            System.err.println(TAG + ": Missing " + SKILLS_JSON + " in → " + dirName);
            return null;
        }

        String configContent = readFileToString(configFile);
        JSONObject config    = new JSONObject(configContent);

        String skillName   = config.getString("name");
        String overview    = config.optString("overview", "");
        String description = config.optString("description", "");

        List<String> privateToolNames = jsonArrayToList(config.optJSONArray("privateTools"));
        List<String> publicToolNames  = jsonArrayToList(config.optJSONArray("publicTools"));

        Skill skill = new Skill(skillName, overview, description, privateToolNames, publicToolNames);


        // ── 2. Scan private tools (inside skill's tools/ folder) ──
        File toolsDir = new File(skillDir, TOOLS_FOLDER);

        if (toolsDir.exists() && toolsDir.isDirectory()) {

            ToolsScanner toolsScanner = new ToolsScanner(toolsDir);
            int toolsRegistered = toolsScanner.scanAndRegister();
            System.out.println(TAG + ": [" + skillName + "] Scanned " + toolsRegistered + " private tool(s)");

            // Resolve private tool references
            for (String privateName : privateToolNames) {
                Tool tool = ToolsManager.getToolByName(privateName);
                if (tool == null)  {
                    System.err.println(TAG + ": [" + skillName + "] Private tool '" + privateName
                            + "' declared in skills.json but not found after scanning tools/ folder");
                }
            }

        } else if (!privateToolNames.isEmpty()) {
            System.err.println(TAG + ": [" + skillName + "] Declares private tools but no tools/ folder found");
        }


        // ── 3. Resolve public tools (must already exist in root) ──
        for (String publicName : publicToolNames) {

            if (ToolsManager.isToolAvailableInRoot(publicName)) {
                Tool tool = ToolsManager.getToolByName(publicName);
                if (tool != null) {
                    skill.addResolvedPublicTool(publicName);
                }
            } else {
                System.err.println(TAG + ": [" + skillName + "] Public tool '" + publicName
                        + "' declared in skills.json but not found in root tools");
            }
        }

        return skill;
    }


    // ─────────────────────────────────────────────
    //  Utilities
    // ─────────────────────────────────────────────

    private List<String> jsonArrayToList(JSONArray array) throws JSONException {

        List<String> list = new ArrayList<>();

        if (array == null) return list;

        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }

        return list;
    }

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



