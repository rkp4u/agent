package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.PersonaRegistry;
import org.example.model.Persona;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller to demonstrate and test the persona system.
 */
@Slf4j
@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaRegistry personaRegistry;

    /**
     * Get all available personas
     */
    @GetMapping
    public Map<String, Object> getAllPersonas() {
        log.info("Fetching all personas");

        List<String> personaIds = personaRegistry.getAllPersonaIds();
        Map<String, Persona> personas = personaRegistry.getPersonas();

        return Map.of(
                "count", personaIds.size(),
                "personaIds", personaIds,
                "personas", personas
        );
    }

    /**
     * Get a specific persona by ID
     */
    @GetMapping("/{personaId}")
    public Map<String, Object> getPersona(@PathVariable String personaId) {
        log.info("Fetching persona: {}", personaId);

        return personaRegistry.getPersona(personaId)
                .map(persona -> Map.of(
                        "found", true,
                        "persona", persona
                ))
                .orElse(Map.of(
                        "found", false,
                        "message", "Persona not found: " + personaId
                ));
    }

    /**
     * Get detailed information about a persona including their tools
     */
    @GetMapping("/{personaId}/details")
    public Map<String, Object> getPersonaDetails(@PathVariable String personaId) {
        log.info("Fetching detailed info for persona: {}", personaId);

        return personaRegistry.getPersona(personaId)
                .map(persona -> {
                    String toolsDesc = persona.getAvailableToolsDescription();

                    return Map.of(
                            "found", true,
                            "persona", persona,
                            "toolsDescription", toolsDesc,
                            "systemPromptPreview", buildSystemPrompt(persona)
                    );
                })
                .orElse(Map.of(
                        "found", false,
                        "message", "Persona not found: " + personaId
                ));
    }

    /**
     * Build a sample system prompt for a persona (preview of what will be used in ReAct)
     */
    private String buildSystemPrompt(Persona persona) {
        return String.format("""
                You are %s, %d years old.
                Background: %s
                Personality: %s
                Speech style: %s
                
                You are at a Singapore kopitiam having a casual conversation.
                
                Available tools: %s
                """,
                persona.getName(),
                persona.getAge(),
                persona.getBackstory(),
                persona.getPersonality(),
                persona.getSpeechStyle(),
                persona.getTools()
        );
    }
}

