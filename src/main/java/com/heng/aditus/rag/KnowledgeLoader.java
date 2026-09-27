package com.heng.aditus.rag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Loads classpath knowledge files and splits them into bounded documents. */
public final class KnowledgeLoader {
    private KnowledgeLoader() { }

    public static List<KnowledgeDocument> load(String location, int chunkSize) {
        if (chunkSize < 1) throw new IllegalArgumentException("chunkSize must be positive");
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(normalize(location));
            List<KnowledgeDocument> documents = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.isReadable()) continue;
                String text;
                try (InputStream input = resource.getInputStream()) {
                    text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
                for (int offset = 0, part = 0; offset < text.length(); offset += chunkSize, part++) {
                    int end = Math.min(text.length(), offset + chunkSize);
                    String chunk = text.substring(offset, end).trim();
                    if (!chunk.isEmpty()) documents.add(new KnowledgeDocument(
                            resource.getDescription() + "#" + part, chunk));
                }
            }
            return documents;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load Aditus knowledge from " + location, exception);
        }
    }

    private static String normalize(String location) {
        if (location.startsWith("classpath:") && !location.contains("*")) {
            String path = location.substring("classpath:".length());
            if (path.endsWith("/")) path += "**/*";
            else if (!path.contains(".")) path += "/**/*";
            return "classpath*:" + path;
        }
        return location;
    }
}
