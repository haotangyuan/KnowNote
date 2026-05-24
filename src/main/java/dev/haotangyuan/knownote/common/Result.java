package dev.haotangyuan.knownote.common;

import lombok.Getter;

/**
 * 统一响应结果
 */
@Getter
public class Result<T> {

    private final String code;
    private final String message;
    private final T data;

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>("0", "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>("0", "success", data);
    }

    public static <T> Result<T> error(String code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
}
