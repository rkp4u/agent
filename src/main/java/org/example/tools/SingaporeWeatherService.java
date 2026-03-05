package org.example.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Tool: Returns Singapore weather from NEA's data.gov.sg APIs.
 * Primary station: S111 (Scotts Road), Fallback: S50 (Clementi Road)
 */
@Slf4j
@Service
public class SingaporeWeatherService {

    private static final String PRIMARY_STATION = "S111";
    private static final String FALLBACK_STATION = "S50";

    private static final Map<String, String> API_ENDPOINTS = Map.of(
            "temperature", "https://api-open.data.gov.sg/v2/real-time/api/air-temperature",
            "humidity",    "https://api-open.data.gov.sg/v2/real-time/api/relative-humidity",
            "rainfall",    "https://api-open.data.gov.sg/v2/real-time/api/rainfall",
            "wind_speed",  "https://api-open.data.gov.sg/v2/real-time/api/wind-speed"
    );

    private final RestClient restClient;

    public SingaporeWeatherService() {
        this.restClient = RestClient.builder().build();
    }

    public String getWeather() {
        log.debug("TOOL [weather]: Fetching Singapore weather");

        Map<String, String> weatherData = new java.util.HashMap<>();

        for (Map.Entry<String, String> entry : API_ENDPOINTS.entrySet()) {
            String metric = entry.getKey();
            String url = entry.getValue();
            try {
                Map<?, ?> response = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(Map.class);

                Double value = extractStationValue(response, List.of(PRIMARY_STATION, FALLBACK_STATION));

                if (value != null) {
                    weatherData.put(metric, formatMetric(metric, value));
                } else {
                    weatherData.put(metric, "N/A");
                }
            } catch (Exception e) {
                log.warn("TOOL [weather]: Failed to fetch {}: {}", metric, e.getMessage());
                weatherData.put(metric, "N/A");
            }
        }

        String result = "Weather in Singapore now:\n" +
                "Temperature: " + weatherData.getOrDefault("temperature", "N/A") + "\n" +
                "Humidity: "    + weatherData.getOrDefault("humidity", "N/A") + "\n" +
                "Rainfall: "    + weatherData.getOrDefault("rainfall", "N/A") + "\n" +
                "Wind Speed: "  + weatherData.getOrDefault("wind_speed", "N/A");

        log.debug("TOOL [weather]: {}", result);
        return result;
    }

    /**
     * Extract value from the API response for the first matching station.
     */
    @SuppressWarnings("unchecked")
    private Double extractStationValue(Map<?, ?> responseData, List<String> stations) {
        if (responseData == null) return null;

        Object code = responseData.get("code");
        if (code instanceof Number && ((Number) code).intValue() != 0) return null;

        Map<?, ?> data = (Map<?, ?>) responseData.get("data");
        if (data == null) return null;

        List<?> readings = (List<?>) data.get("readings");
        if (readings == null || readings.isEmpty()) return null;

        Map<?, ?> latestReading = (Map<?, ?>) readings.get(0);
        List<Map<String, Object>> stationData = (List<Map<String, Object>>) latestReading.get("data");
        if (stationData == null) return null;

        // Build a map of stationId -> value
        Map<String, Double> readingMap = new java.util.HashMap<>();
        for (Map<String, Object> entry : stationData) {
            String stationId = (String) entry.get("stationId");
            Object val = entry.get("value");
            if (stationId != null && val instanceof Number) {
                readingMap.put(stationId, ((Number) val).doubleValue());
            }
        }

        // Return value from first matching station
        for (String station : stations) {
            if (readingMap.containsKey(station)) {
                return readingMap.get(station);
            }
        }

        return null;
    }

    private String formatMetric(String metric, Double value) {
        return switch (metric) {
            case "temperature" -> value + "°C";
            case "humidity"    -> value + "%";
            case "rainfall"    -> value + " mm";
            case "wind_speed"  -> value + " km/h";
            default            -> String.valueOf(value);
        };
    }
}

