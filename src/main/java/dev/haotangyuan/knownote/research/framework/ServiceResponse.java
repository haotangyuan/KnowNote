package dev.haotangyuan.knownote.research.framework;

/**
 * Typed result record with status (OK / ERROR / QUOTA_EXCEEDED).
 */
public record ServiceResponse<T>(Status status, T content, String errorMessage) {

    public enum Status {
        OK,
        ERROR,
        QUOTA_EXCEEDED
    }

    public static <T> ServiceResponse<T> ok(T content) {
        return new ServiceResponse<>(Status.OK, content, null);
    }

    public static <T> ServiceResponse<T> error(String message) {
        return new ServiceResponse<>(Status.ERROR, null, message);
    }

    public static <T> ServiceResponse<T> quotaExceeded(String message) {
        return new ServiceResponse<>(Status.QUOTA_EXCEEDED, null, message);
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isQuotaExceeded() {
        return status == Status.QUOTA_EXCEEDED;
    }
}
