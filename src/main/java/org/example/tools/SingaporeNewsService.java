package org.example.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool: Returns latest Singapore news from Mothership.sg RSS feed.
 * Falls back to hardcoded news items if the feed is unavailable.
 */
@Slf4j
@Service
public class SingaporeNewsService {

    private static final String RSS_URL = "https://mothership.sg/feed/";
    private static final int MAX_ITEMS = 10;

    private final RestClient restClient;

    public SingaporeNewsService() {
        this.restClient = RestClient.builder().build();
    }

    public String getNews() {
        log.debug("TOOL [news]: Fetching Singapore news");

        try {
            String rssXml = restClient.get()
                    .uri(RSS_URL)
                    .retrieve()
                    .body(String.class);

            List<NewsItem> items = parseRss(rssXml);

            if (!items.isEmpty()) {
                return formatNews(items);
            }
        } catch (Exception e) {
            log.warn("TOOL [news]: Failed to fetch RSS feed: {}", e.getMessage());
        }

        log.debug("TOOL [news]: Using fallback news");
        return fallbackNews();
    }

    /**
     * Parse RSS XML and extract title + description from each <item>.
     */
    private List<NewsItem> parseRss(String xml) {
        List<NewsItem> items = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList itemNodes = doc.getElementsByTagName("item");
            int limit = Math.min(itemNodes.getLength(), MAX_ITEMS);

            for (int i = 0; i < limit; i++) {
                Element item = (Element) itemNodes.item(i);

                String title   = getElementText(item, "title");
                String snippet = stripHtml(getElementText(item, "description"));

                if (!title.isBlank()) {
                    items.add(new NewsItem(title, snippet));
                }
            }
        } catch (Exception e) {
            log.warn("TOOL [news]: RSS parse error: {}", e.getMessage());
        }
        return items;
    }

    private String getElementText(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() > 0 && nodes.item(0) != null) {
            return nodes.item(0).getTextContent().strip();
        }
        return "";
    }

    /**
     * Strip any HTML tags from description snippets.
     */
    private String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return html.replaceAll("<[^>]*>", "").strip();
    }

    private String formatNews(List<NewsItem> items) {
        StringBuilder sb = new StringBuilder("Latest Singapore news:\n\n");
        for (int i = 0; i < items.size(); i++) {
            NewsItem item = items.get(i);
            sb.append(i + 1).append(". ").append(item.title()).append("\n");
            if (!item.snippet().isBlank()) {
                sb.append("   ").append(item.snippet()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().strip();
    }

    private String fallbackNews() {
        return """
                Latest Singapore news:
                
                1. Local kopitiam wins best kopi award
                   Traditional coffee-making skills recognized nationally
                
                2. New MRT line to connect heartlands
                   Enhanced connectivity for residential areas
                
                3. Singapore weather: Monsoon season expected
                   Heavy rains forecasted for the coming weeks""";
    }

    private record NewsItem(String title, String snippet) {}
}

