package dev.haotangyuan.knownote.post.mq.review;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审核结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {
    @Description("Whether the content is approved.")
    private Boolean approved;
    @Description("Reason for rejection if not approved; otherwise leave empty.")
    private String rejectedReason;
    @Description("A concise Chinese summary of the article for description field; leave empty if rejected.")
    private String description;
}
