package com.heng.aditus;

import com.heng.aditus.model.ChatModel;
import com.heng.aditus.config.AditusProperties;
import com.heng.aditus.memory.ChatMemory;
import com.heng.aditus.model.ChatMessage;
import com.heng.aditus.rag.KnowledgeDocument;
import com.heng.aditus.rag.KnowledgeRetriever;
import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Coordinates conversations by combining the system prompt and history before calling {@link ChatModel}.
 * <p>Registered by auto-configuration for direct injection; service subclasses and client proxies
 * also delegate here. The model adapter handles tool calls, while this class manages history.
 */
public class AditusAssistant implements AditusOperations {
    private final ChatModel model;
    private final ChatMemory memory;
    private final AditusProperties properties;
    private final KnowledgeRetriever knowledgeRetriever;
    private final ConcurrentHashMap<String, Semaphore> conversationLocks = new ConcurrentHashMap<>();

    /**
     * Creates an assistant, normally through Spring auto-configuration.
     * @param model the model invocation implementation
     * @param memory the message store indexed by conversation ID
     * @param properties framework settings, including the system prompt
     */
    public AditusAssistant(ChatModel model, ChatMemory memory, AditusProperties properties,
                            KnowledgeRetriever knowledgeRetriever) {
        this.model = model;
        this.memory = memory;
        this.properties = properties;
        this.knowledgeRetriever = knowledgeRetriever;
    }

    /** Backward-compatible constructor without knowledge retrieval. */
    public AditusAssistant(ChatModel model, ChatMemory memory, AditusProperties properties) {
        this(model, memory, properties, (query, topK) -> java.util.List.of());
    }

    public String chat(String message) {
        java.util.List<ChatMessage> messages = new java.util.ArrayList<>();
        if (!properties.getChat().getSystemPrompt().isBlank()) {
            messages.add(ChatMessage.system(properties.getChat().getSystemPrompt()));
        }
        addRetrievedContext(messages, message);
        messages.add(ChatMessage.user(message));
        return model.chat(messages);
    }

    public Flux<String> stream(String message) {
        return stream(null, message);
    }

    /**
     * {@inheritDoc}
     * <p>Reads history on each subscription and appends both messages only on normal completion.
     * Cancellation or failure saves neither message, even if the caller has received partial text.
     */
    public Flux<String> stream(String conversationId, String message) {
        return Flux.defer(() -> {
            boolean remember = conversationId != null && !conversationId.isBlank();
            Semaphore lock = remember ? conversationLocks.computeIfAbsent(conversationId,
                    ignored -> new Semaphore(1)) : null;
            if (lock != null) {
                try {
                    lock.acquire();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return Flux.error(exception);
                }
            }
            java.util.List<ChatMessage> messages = new java.util.ArrayList<>();
            if (!properties.getChat().getSystemPrompt().isBlank()) {
                messages.add(ChatMessage.system(properties.getChat().getSystemPrompt()));
            }
            StringBuilder answer = new StringBuilder();
            try {
                if (remember) messages.addAll(memory.messages(conversationId));
                addRetrievedContext(messages, message);
                messages.add(ChatMessage.user(message));
                return model.stream(messages)
                        .doOnNext(answer::append)
                        .doOnComplete(() -> {
                            if (remember) memory.appendTurn(conversationId, ChatMessage.user(message),
                                    ChatMessage.assistant(answer.toString()));
                        })
                        .doFinally(signal -> {
                            if (lock != null) {
                                lock.release();
                                conversationLocks.remove(conversationId, lock);
                            }
                        });
            } catch (RuntimeException exception) {
                if (lock != null) {
                    lock.release();
                    conversationLocks.remove(conversationId, lock);
                }
                throw exception;
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>Both blocking and streaming paths save only completed turns.
     */
    public String chat(String conversationId, String message) {
        if (conversationId == null || conversationId.isBlank()) return chat(message);
        Semaphore lock = conversationLocks.computeIfAbsent(conversationId, ignored -> new Semaphore(1));
        lock.acquireUninterruptibly();
        try {
            java.util.List<ChatMessage> messages = new java.util.ArrayList<>(memory.messages(conversationId));
            if (!properties.getChat().getSystemPrompt().isBlank()) {
                messages.add(0, ChatMessage.system(properties.getChat().getSystemPrompt()));
            }
            addRetrievedContext(messages, message);
            messages.add(ChatMessage.user(message));
            String answer = model.chat(messages);
            memory.appendTurn(conversationId, ChatMessage.user(message), ChatMessage.assistant(answer));
            return answer;
        } finally {
            lock.release();
            conversationLocks.remove(conversationId, lock);
        }
    }

    public void clearConversation(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return;
        Semaphore lock = conversationLocks.computeIfAbsent(conversationId, ignored -> new Semaphore(1));
        lock.acquireUninterruptibly();
        try {
            memory.clear(conversationId);
        } finally {
            lock.release();
            conversationLocks.remove(conversationId, lock);
        }
    }

    private void addRetrievedContext(java.util.List<ChatMessage> messages, String query) {
        java.util.List<KnowledgeDocument> documents = knowledgeRetriever.retrieve(query,
                properties.getChat().getKnowledgeTopK());
        if (documents.isEmpty()) return;
        String context = documents.stream()
                .map(document -> "[" + document.id() + "]\n" + document.text())
                .collect(java.util.stream.Collectors.joining("\n\n"));
        messages.add(ChatMessage.system("Relevant knowledge retrieved for this request:\n" + context));
    }
}
