package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a persona/character in the kopitiam conversation system.
 * Each persona has unique characteristics, personality traits, and available tools.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    /**
     * Unique identifier for the persona (e.g., "ah_seng", "mei_qi")
     */
    private String id;

    /**
     * Display name of the persona (e.g., "Uncle Ah Seng")
     */
    private String name;

    /**
     * Age of the persona
     */
    private int age;

    /**
     * Background story of the persona
     */
    private String backstory;

    /**
     * Personality traits and characteristics
     */
    private String personality;

    /**
     * Speech style and patterns (e.g., "Heavy Singlish, uses 'lah', 'lor'")
     */
    private String speechStyle;

    /**
     * List of tool names available to this persona (e.g., ["time", "weather"])
     */
    private List<String> tools;

    /**
     * Get a formatted description of available tools for the persona
     */
    public String getAvailableToolsDescription() {
        if (tools == null || tools.isEmpty()) {
            return "No tools available";
        }

        StringBuilder sb = new StringBuilder();
        for (String tool : tools) {
            sb.append("\n\n").append(tool).append(":\n");
            sb.append(getToolDescription(tool));
        }
        return sb.toString();
    }

    /**
     * Get description for a specific tool
     */
    private String getToolDescription(String tool) {
        return switch (tool.toLowerCase()) {
            case "time" -> "Returns current time in Singapore";
            case "weather" -> "Returns current weather in Singapore";
            case "news" -> "Returns latest Singapore news";
            default -> "Unknown tool";
        };
    }

    /**
     * Check if this persona has access to a specific tool
     */
    public boolean hasTool(String toolName) {
        return tools != null && tools.contains(toolName.toLowerCase());
    }
}

