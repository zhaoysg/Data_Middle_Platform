package com.bagdatahouse.common.result;

import com.bagdatahouse.common.enums.ResponseCode;

import java.io.Serializable;

/**
 * 分页结果
 */
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private boolean success;
    private long timestamp = System.currentTimeMillis();
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private T data;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getPageNum() { return pageNum; }
    public void setPageNum(Long pageNum) { this.pageNum = pageNum; }
    public Long getPageSize() { return pageSize; }
    public void setPageSize(Long pageSize) { this.pageSize = pageSize; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> PageResult<T> of(Long total, T data) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setData(data);
        result.setCode(ResponseCode.SUCCESS.getCode());
        result.setSuccess(true);
        return result;
    }

    public static <T> PageResult<T> of(Long total, T data, Long pageNum, Long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setData(data);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setCode(ResponseCode.SUCCESS.getCode());
        result.setSuccess(true);
        return result;
    }
}
