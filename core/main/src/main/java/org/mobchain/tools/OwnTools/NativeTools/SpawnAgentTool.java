package org.mobchain.tools.OwnTools.NativeTools;

import com.example.myapplication.DAOs.ChatMessageStore;
import com.example.myapplication.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.mobAgent.plugin.interfaces.Tool;
import org.mobchain.client.parsers.Parser;
import org.mobchain.memory.InMemory;
import org.mobchain.messages.HumanMessages;
import org.mobchain.messages.SystemMessages;
import org.mobchain.models.ModelInterface;
import org.mobchain.models.OpenAIFormatter;
import org.mobchain.skills.Skill;
import org.mobchain.tools.ToolsManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SpawnAgentTool implements Tool {

    String toolName = "spawn_agent";
    String description = "Tool to spawn a new agent with specific attributes and goals";
    String skillName = "root";

    JSONObject structuredTool;

    public SpawnAgentTool() {

        HashMap<String,List<String>> toolParams = new HashMap<>();

        toolParams.put("skillName" , Arrays.asList("string","name of the skill used to spawn the agent with"));
        toolParams.put("userMessage" , Arrays.asList("string" , "The last user message that requires this skill/sub-agent"));

        try{
            structuredTool = Parser.createOpenAICompatibleFunctionTool(toolName,description,toolParams, toolParams.keySet().toArray(new String[0]) );

        } catch (JSONException e){

        }

    }



    @Override
    public JSONObject runTool(JSONObject args) {


        JSONObject result = new JSONObject();

        try{

            if( args.has( "skillName" ) && args.has("userMessage") ) {

                String skillName = args.getString("skillName");

                System.out.println("spawning an agent : "+skillName);

                Skill skill = ToolsManager.skillsRegistry.get( skillName );

                List<Tool> privateToolsAssociateedWithSkill = ToolsManager.getToolsBySkill(skillName);
                List<Tool> getPublicToolsAssociatedWithSkill = ToolsManager.getPublicToolsBySkill( skill.getPublicToolNames() );

                ChatMessageStore chatMessageStore = new ChatMessageStore(MainActivity.getAppContext());
                InMemory memory = new InMemory(chatMessageStore);

                memory.setSystemPrompt( new SystemMessages(skill.getDescription()));

                OpenAIFormatter model = OpenAIFormatter.builder()
                        .baseURL("http://127.0.0.1:8080/v1/chat/completions")
                        .model("model")
                        .build();

                ModelInterface agentInterface = ModelInterface.builder()
                        .setModel(model)
                        .setMemory(memory)
                        .addTools(privateToolsAssociateedWithSkill)
                        .addTools(getPublicToolsAssociatedWithSkill)
                        .build();

                String agentOutput = agentInterface.chat( new HumanMessages( (String)args.get("userMessage") ));


                result.put("outputOfTheSkill", agentOutput );

                return result;

            }

            result.put("error" , "arguments not provided");

        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public String getToolName() {
        return toolName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public JSONObject getStructuredTool() {
        return structuredTool;
    }

    @Override
    public void setStructuredTool(JSONObject structuredTool) {
        this.structuredTool = structuredTool;
    }

    @Override
    public String getSkillName() {
        return skillName;
    }
}
