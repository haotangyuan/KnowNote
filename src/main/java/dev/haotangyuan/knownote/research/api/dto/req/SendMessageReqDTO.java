package dev.haotangyuan.knownote.research.api.dto.req;

import lombok.Getter;

/**
 * 研究消息请求
 */
@Getter
public class SendMessageReqDTO {
    private String content;
    private String modelId;
    private String budget;
}
