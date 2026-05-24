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
 * 研究对话消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessageDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String researchId;
    private String role;
    private String content;
    private Integer sequenceNo;
    private LocalDateTime createTime;
}
