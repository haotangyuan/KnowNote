package dev.haotangyuan.knownote.research.tool.detail;

import dev.haotangyuan.knownote.research.tool.annotation.SupervisorTool;
import dev.langchain4j.agent.tool.Tool;

/**
 * 研究完成标记工具
 */
@SupervisorTool
public class ResearchCompleteTool {
    @Tool("Tool for indicating that the research process is complete.")
    public String researchComplete() {
        return "Research process marked complete.";
    }
}
