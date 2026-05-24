package dev.haotangyuan.knownote.research.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 研究工作流事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("workflow_event")
public class WorkflowEventDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String researchId;
    private String type;
    private String title;
    private String content;
    private Long parentEventId;
    private Integer sequenceNo;
    private LocalDateTime createTime;
}
