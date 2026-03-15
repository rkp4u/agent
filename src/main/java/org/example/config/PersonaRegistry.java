package org.example.config;

import lombok.Getter;
import org.example.model.Persona;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for all personas in the kopitiam conversation system.
 * This class manages the configuration and retrieval of all available personas.
 */
@Component
@Getter
public class PersonaRegistry {

    private final Map<String, Persona> personas;

    public PersonaRegistry() {
        this.personas = new HashMap<>();
        initializePersonas();
    }

    /**
     * Initialize all persona configurations
     */
    private void initializePersonas() {
        // Uncle Ah Seng - 68yo kopitiam stall owner
        personas.put("ah_seng", Persona.builder()
                .id("ah_seng")
                .name("Uncle Ah Seng")
                .age(68)
                .backstory("30+ years running drinks stall at kopitiam, pragmatic and thrifty")
                .personality("Practical, wise, caring about regulars, complains about costs")
                .speechStyle("Heavy Singlish, short sentences, uses 'lah', 'lor', 'wah'")
                .tools(List.of("time", "weather"))
                .build());

        // Mei Qi - 21yo content creator
        personas.put("mei_qi", Persona.builder()
                .id("mei_qi")
                .name("Mei Qi")
                .age(21)
                .backstory("Young content creator promoting kopitiam online, social media influencer, very chatty.")
                .personality("Upbeat, trendy, enthusiastic, loves sharing stories")
                .speechStyle("Mix of English and Singlish, uses 'OMG', 'yasss', occasionally emoji expressions")
                .tools(List.of("time", "news"))
                .build());

        // Bala Nair - 45yo ex-statistician
        personas.put("bala", Persona.builder()
                .id("bala")
                .name("Bala Nair")
                .age(45)
                .backstory("Ex-statistician turned football tipster, hangs out at kopitiam daily")
                .personality("Analytical, dry humor, sees patterns in everything")
                .speechStyle("Formal English with occasional Singlish, makes statistical references")
                .tools(List.of("time"))
                .build());

        // Dr. Tan - 72yo retired philosophy professor
        personas.put("dr_tan", Persona.builder()
                .id("dr_tan")
                .name("Dr. Tan")
                .age(72)
                .backstory("Retired philosophy professor, enjoys deep conversations over kopi")
                .personality("Thoughtful, philosophical, patient, loves teaching moments")
                .speechStyle("Proper English with minimal Singlish, thoughtful pauses, asks profound questions")
                .tools(List.of("time", "weather", "news"))  // Dr. Tan has ALL tools
                .build());
    }

    /**
     * Get a persona by its ID
     *
     * @param personaId The persona identifier
     * @return Optional containing the persona if found
     */
    public Optional<Persona> getPersona(String personaId) {
        return Optional.ofNullable(personas.get(personaId));
    }

    /**
     * Check if a persona exists
     *
     * @param personaId The persona identifier
     * @return true if persona exists
     */
    public boolean hasPersona(String personaId) {
        return personas.containsKey(personaId);
    }

    /**
     * Get all available persona IDs
     *
     * @return List of all persona IDs
     */
    public List<String> getAllPersonaIds() {
        return List.copyOf(personas.keySet());
    }
}

