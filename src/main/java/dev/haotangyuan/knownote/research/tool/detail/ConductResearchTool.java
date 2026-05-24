package dev.haotangyuan.knownote.research.tool.detail;

import dev.haotangyuan.knownote.research.tool.annotation.SupervisorTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * Supervisor 任务委派工具
 */
@SupervisorTool
public class ConductResearchTool {

    @Tool("Tool for delegating a research task to a specialized sub-agent.")
    public String conductResearch(
            @P(value = "The topic to research. Should be a single topic described in high detail (at least a paragraph).",
                    required = true)
            String researchTopic
    ) {
        return "Research delegated: " + researchTopic;
    }
}
