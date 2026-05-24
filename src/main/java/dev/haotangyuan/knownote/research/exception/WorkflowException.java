package dev.haotangyuan.knownote.research.exception;

/**
 * 研究工作流内部异常
 */
public class WorkflowException extends RuntimeException {

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
