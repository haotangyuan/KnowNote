package dev.haotangyuan.knownote.research.exception;

import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;

/**
 * 研究流程相关异常
 */
public class ResearchException extends BizException {

    public ResearchException(String message) {
        super(ErrorCode.CLIENT_ERROR, message);
    }

    public ResearchException(String message, Throwable cause) {
        super(ErrorCode.CLIENT_ERROR, message);
        initCause(cause);
    }
}
