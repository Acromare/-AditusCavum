package com.heng.aditus.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Lightweight lexical retriever with no embedding or database dependency. */
public final class InMemoryKnowledgeRetriever implements KnowledgeRetriever {
    private final List<KnowledgeDocument> documents;

    public InMemoryKnowledgeRetriever(List<KnowledgeDocument> documents) {
        this.documents = List.copyOf(documents);
    }

    @Override
    public int size() { return documents.size(); }

    @Override
    public List<KnowledgeDocument> retrieve(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) return List.of();
        Set<String> terms = terms(query);
        return documents.stream()
                .map(document -> new Scored(document, score(document.text(), terms)))
                .filter(scored -> scored.score > 0)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(topK)
                .map(Scored::document)
                .toList();
    }

    private static double score(String text, Set<String> terms) {
        Set<String> words = terms(text);
        long matches = terms.stream().filter(words::contains).count();
        return terms.isEmpty() ? 0 : (double) matches / terms.size();
    }

    private static Set<String> terms(String text) {
        return Set.of(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+"));
    }

    private record Scored(KnowledgeDocument document, double score) { }
}
