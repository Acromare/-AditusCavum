/**
 * Simple Spring conversation APIs with pluggable models, memory, and business tools.
 *
 * <p>Start with {@link com.heng.aditus.AditusOperations} for method contracts. Applications may
 * inject {@link com.heng.aditus.AditusAssistant}, extend {@link com.heng.aditus.AditusService},
 * or declare an interface annotated with {@link com.heng.aditus.annotation.AditusClient}.
 * All three entry points share the same assistant behavior.
 *
 * <p>At startup, {@link com.heng.aditus.config.AditusConfiguration} registers the components.
 * During a request, the assistant combines the system prompt and
 * {@link com.heng.aditus.memory.ChatMemory} history before calling
 * {@link com.heng.aditus.model.ChatModel}. The default model adapter uses
 * {@link com.heng.aditus.tool.ToolRegistry} to execute annotated business methods when requested
 * by the model. This package provides conversation services; applications define their own HTTP endpoints.
 */
package com.heng.aditus;
