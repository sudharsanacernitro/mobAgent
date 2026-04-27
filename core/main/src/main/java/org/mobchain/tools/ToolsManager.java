package org.mobchain.tools;


import org.mobAgent.plugin.interfaces.Tool;
import org.mobchain.skills.Skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


//=============[ TOOLS ]=====================


public class ToolsManager {

   public static HashMap< String , Tool> toolsRegistry = new HashMap<>();

   public static HashMap<String , List<Tool>> skillsToToolsMapping = new HashMap<>();

   public static HashMap<String , Skill> skillsRegistry = new HashMap<>();

   private static StringBuilder systemPrompt = new StringBuilder();



   public static HashMap<String , Tool> getToolsRegistry( ) {

       return new HashMap<>( toolsRegistry );

   }

   public static Tool getToolByName( String name ) {

       if( toolsRegistry.containsKey( name ) ) {

           return toolsRegistry.get( name );

       }

       return null;

   }


   public static  List<Tool> getToolsArray( ) {

       return new ArrayList<>(toolsRegistry.values());

   }



    public static void addTools( String skillName , String functionName , Tool newTool ) {

       toolsRegistry.put( functionName , newTool );

        List<Tool> toolsAssociatedWithSkills =   skillsToToolsMapping.getOrDefault(skillName,new ArrayList<>());
        toolsAssociatedWithSkills.add( newTool );
        skillsToToolsMapping.put(skillName ,  toolsAssociatedWithSkills);

   }

    public static void addTools( String skillName , Tool newTool ) {

       toolsRegistry.put(newTool.getToolName(), newTool);

        List<Tool> toolsAssociatedWithSkills =   skillsToToolsMapping.getOrDefault(skillName,new ArrayList<>());
        toolsAssociatedWithSkills.add( newTool );
        skillsToToolsMapping.put(skillName ,  toolsAssociatedWithSkills);

   }



   public static int toolsSize(){

       for( String key : toolsRegistry.keySet() ) {

           System.out.println(key);

       }

       return toolsRegistry.size();

   }

   public static boolean isToolAvailableInRoot( String toolName ) {

       if( toolsRegistry.containsKey( toolName ) ) {

           if( toolsRegistry.get( toolName ).getSkillName().equals("root") ) {

               return true;

           }

       }

       return false;

   }

   public static List<Tool> getToolsBySkill( String skillName ) {

       return skillsToToolsMapping.getOrDefault(skillName, new ArrayList<>());

   }

   public static int getToolsCountBySkill( String skillName ) {

       return skillsToToolsMapping.getOrDefault(skillName, new ArrayList<>()).size();

   }


   // ─────────────────────────────────────────────
   //  Skill Management
   // ─────────────────────────────────────────────


    public static List<Tool> getPublicToolsBySkill( List<String> publicToolNames ) {

        List<Tool> publicTools = new ArrayList<>();

        for( String toolName : publicToolNames ) {

            if( toolsRegistry.containsKey( toolName ) ) {

                publicTools.add( toolsRegistry.get( toolName ) );

            }

        }

        return publicTools;

    }

   public static void addSkill( Skill skill ) {

       skillsRegistry.put( skill.getName() , skill );


   }

   public static String getSystemPromptForSkills() {

       systemPrompt = new StringBuilder("The following skills are available to you:\n\n");

       for(Skill skill : skillsRegistry.values()) {
           systemPrompt.append("- ").append(skill.getName()).append(": ").append(skill.getOverview()).append("\n");
       }

       return systemPrompt.toString();
   }

   public static Skill getSkill( String skillName ) {

       return skillsRegistry.getOrDefault( skillName , null );

   }

   public static List<Skill> getAllSkills() {

       return new ArrayList<>( skillsRegistry.values() );

   }

   public static boolean isSkillRegistered( String skillName ) {

       return skillsRegistry.containsKey( skillName );

   }

   public static int skillsSize() {

       return skillsRegistry.size();

   }



}


