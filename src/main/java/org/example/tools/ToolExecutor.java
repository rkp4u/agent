package org.example.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Routes tool calls by name to the appropriate service.
 * Mirrors the execute_tool() function in the Python implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutor {

    private final SingaporeTimeService timeService;
    private final SingaporeWeatherService weatherService;
    private final SingaporeNewsService newsService;

    /**
     * Execute a tool by name and return its output as a String.
     *
     * @param toolName One of "time", "weather", "news"
     * @return Tool output string
     */
    public String execute(String toolName) {
        log.info("TOOL EXECUTOR: Executing tool '{}'", toolName);

        return switch (toolName.toLowerCase().strip()) {
            case "time"    -> timeService.getTime();
            case "weather" -> weatherService.getWeather();
            case "news"    -> newsService.getNews();
            default        -> {
                log.warn("TOOL EXECUTOR: Unknown tool '{}'", toolName);
                yield "Unknown tool: " + toolName;
            }
        };
    }
}

