package com.parkingmanage.common.exception;

import lombok.Data;

//自定义异常抛出
@Data
public class CustomException extends RuntimeException {
    //异常code码
    private String code;
    //异常信息
    private String msg;
    public CustomException(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
}
