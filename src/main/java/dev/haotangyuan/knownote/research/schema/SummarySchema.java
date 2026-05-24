package dev.haotangyuan.knownote.research.schema;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 搜索内容摘要结构
 */
@Data
public class SummarySchema {
    @Description("A comprehensive summary of the webpage content, highlighting the main points and key information.")
    private String summary;

    @Description("Important direct quotes or excerpts from the content that support the summary.")
    private String keyExcerpts;
}
