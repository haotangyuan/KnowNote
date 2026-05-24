package dev.haotangyuan.knownote.research.api.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 发送消息返回
 */
@Data
@Builder
public class SendMessageRespDTO {
    private String id;
    private String content;
}
