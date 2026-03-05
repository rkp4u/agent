package org.example.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tool: Returns current time in Singapore timezone.
 */
@Slf4j
@Service
public class SingaporeTimeService {

    private static final ZoneId SG_TIMEZONE = ZoneId.of("Asia/Singapore");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String getTime() {
        log.debug("TOOL [time]: Fetching Singapore time");
        ZonedDateTime sgTime = ZonedDateTime.now(SG_TIMEZONE);
        String result = "Time in Singapore now: " + sgTime.format(FORMATTER);
        log.debug("TOOL [time]: {}", result);
        return result;
    }
}

