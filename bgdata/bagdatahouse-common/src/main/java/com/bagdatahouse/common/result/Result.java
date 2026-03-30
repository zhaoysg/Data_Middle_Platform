package com.bagdatahouse.common.result;

import com.bagdatahouse.common.enums.ResponseCode;

import java.io.Serializable;

/**
 * 统一响应结果
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;

    /** 消息 */
    private String message;

    /** 数据 */
    private T data;

    /** 时间戳 */
    private long timestamp;

    /** 是否成功 */
    private boolean success;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResponseCode.SUCCESS.getCode());
        result.setMessage(ResponseCode.SUCCESS.getMessage());
        result.setData(data);
        result.setSuccess(true);
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(ResponseCode.SUCCESS.getCode());
        result.setMessage(message);
        result.setData(data);
        result.setSuccess(true);
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setSuccess(false);
        return result;
    }

    public static <T> Result<T> fail(ResponseCode responseCode) {
        return fail(responseCode.getCode(), responseCode.getMessage());
    }

    public static <T> Result<T> fail(ResponseCode responseCode, String message) {
        return fail(responseCode.getCode(), message);
    }

    public static <T> Result<T> fail(String message) {
        return fail(ResponseCode.SYSTEM_ERROR.getCode(), message);
    }

    /**
     * 分页结果封装
     */
    public static <T> PageResult<T> page(Long total, T data) {
        return PageResult.of(total, data);
    }

    /**
     * 分页结果封装
     */
    public static <T> PageResult<T> page(Long total, T data, Long pageNum, Long pageSize) {
        return PageResult.of(total, data, pageNum, pageSize);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
