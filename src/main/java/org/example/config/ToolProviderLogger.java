package org.example.config;

import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decorator for ToolProvider that tracks tool execution statistics
 */
public class ToolProviderLogger implements ToolProvider {

    private static final Logger log = LoggerFactory.getLogger(ToolProviderLogger.class);
    private static final ThreadLocal<AtomicInteger> toolCallCount = ThreadLocal.withInitial(AtomicInteger::new);

    private final ToolProvider delegate;

    public ToolProviderLogger(ToolProvider delegate) {
        this.delegate = delegate;
        log.debug("ToolProviderLogger initialized");
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        int currentCount = toolCallCount.get().incrementAndGet();

        log.info("Tool request #{} from agent", currentCount);
        log.debug("Tool request details: {}", request);

        long startTime = System.currentTimeMillis();

        try {
            ToolProviderResult result = delegate.provideTools(request);
            long duration = System.currentTimeMillis() - startTime;

            if (result != null && result.tools() != null) {
                log.info("Tool request #{} completed successfully in {}ms - {} tool(s) provided",
                        currentCount, duration, result.tools().size());
                log.debug("Tools provided: {}", result.tools());
            } else {
                log.info("Tool request #{} completed successfully in {}ms - no tools provided",
                        currentCount, duration);
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Tool request #{} failed after {}ms", currentCount, duration, e);
            throw e;
        }
    }

    /**
     * Get the current tool call count for this thread
     */
    public static int getToolCallCount() {
        return toolCallCount.get().get();
    }

    /**
     * Reset the tool call count for this thread
     */
    public static void resetToolCallCount() {
        toolCallCount.get().set(0);
    }
}


