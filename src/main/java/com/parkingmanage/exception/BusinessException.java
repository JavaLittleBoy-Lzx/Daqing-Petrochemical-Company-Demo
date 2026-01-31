package com.parkingmanage.exception;

/**
 * 业务异常类
 * 用于处理业务逻辑中的可预期异常，不打印完整堆栈跟踪
 */
public class BusinessException extends RuntimeException {
    
    private String code;
    
    public BusinessException(String message) {
        super(message);
        this.code = "1";
    }
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 重写fillInStackTrace方法，不填充堆栈信息，提高性能并减少日志输出
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}

