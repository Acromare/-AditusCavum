package com.heng.aditus.rag;

import java.util.List;

/** Retrieves relevant knowledge for a user question. */
public interface KnowledgeRetriever {
    List<KnowledgeDocument> retrieve(String query, int topK);

    /** Number of indexed chunks when known. */
    default int size() { return -1; }
}
