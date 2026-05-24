package dev.haotangyuan.knownote.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    CLIENT_ERROR("A001", "请求错误"),
    SERVER_ERROR("B001", "系统错误"),
    THIRD_PARTY_ERROR("C001", "第三方服务异常");

    private final String code;
    private final String message;
}
