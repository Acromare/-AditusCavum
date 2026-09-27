package com.heng.aditus.web;

import com.heng.aditus.config.AditusProperties;
import com.heng.aditus.rag.KnowledgeRetriever;
import com.heng.aditus.tool.AditusTool;
import com.heng.aditus.tool.ToolRegistry;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only runtime information for the bundled developer console. */
@RestController
@RequestMapping("${aditus-cavum.web.path:/aditus}-console/api")
public class AditusConsoleController {
    private final AditusProperties properties;
    private final ToolRegistry tools;
    private final KnowledgeRetriever knowledge;

    public AditusConsoleController(AditusProperties properties, ToolRegistry tools,
                                   KnowledgeRetriever knowledge) {
        this.properties = properties;
        this.tools = tools;
        this.knowledge = knowledge;
    }

    @GetMapping("/status")
    public Status status() {
        return new Status(properties.getModel().getModel(), knowledge.size(),
                tools.all().stream().map(AditusTool::name).toList(),
                properties.getChat().getKnowledgeTopK());
    }

    public record Status(String model, int knowledgeChunks, List<String> tools, int knowledgeTopK) { }
}
