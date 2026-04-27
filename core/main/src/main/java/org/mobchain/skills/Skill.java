package org.mobchain.skills;

import org.mobAgent.plugin.interfaces.Tool;
import org.mobchain.tools.ToolsManager;

import java.util.ArrayList;
import java.util.List;


/**
 * Skill - Represents a single skill parsed from skills.json.
 *
 * Example skills.json:
 * {
 *     "name" : "researcher",
 *     "overview" : "can research about topic from the internet ...",
 *     "privateTools" : ["summarizer"],
 *     "publicTools" : ["searchTool"],
 *     "description" : "You are an excellent researcher ..."
 * }
 *
 * - privateTools → tools that live inside the skill's own "tools" folder.
 * - publicTools  → tools already registered under the "root" skill in ToolsManager.
 */
public class Skill {

    private final String name;
    private final String overview;
    private final String description;

    private final List<String> privateToolNames;
    private final List<String> publicToolNames;

    // Resolved tool references (populated after scanning)
    private final List<String> resolvedPublicToolNames  = new ArrayList<>();


    public Skill(String name,
                 String overview,
                 String description,
                 List<String> privateToolNames,
                 List<String> publicToolNames) {

        this.name             = name;
        this.overview         = overview;
        this.description      = description;
        this.privateToolNames = privateToolNames;
        this.publicToolNames  = publicToolNames;
    }


    // ── Getters ──────────────────────────────────

    public String getName()        { return name; }
    public String getOverview()    { return overview; }
    public String getDescription() { return description; }

    public List<String> getPrivateToolNames() { return privateToolNames; }
    public List<String> getPublicToolNames()  { return publicToolNames; }

    public List<Tool> getResolvedPrivateTools() { return ToolsManager.getToolsBySkill(name); }
    public List<String> getResolvedPublicTools()  { return publicToolNames; }

    /** All tools (private + public) available to this skill. */
    public List<Tool> getAllTools() {
        List<Tool> all = ToolsManager.getToolsBySkill(name);

        for(String publicToolName : resolvedPublicToolNames) {
                Tool publicTool = ToolsManager.getToolByName(publicToolName);
            if(publicTool != null) {
                all.add(publicTool);
            }
        }

        return all;
    }


    // ── Mutators (used by SkillsScanner during resolution) ──

    public void addResolvedPublicTool(String toolName)  { resolvedPublicToolNames.add(toolName);  }


    @Override
    public String toString() {
        return "Skill{" +
                "name='" + name + '\'' +
                ", privateTools=" + privateToolNames +
                ", publicTools=" + publicToolNames +
                '}';
    }
}

