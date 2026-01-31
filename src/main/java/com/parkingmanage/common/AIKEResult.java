package com.parkingmanage.common;

import lombok.Data;

import java.util.HashMap;

//TODO 响应头
@Data
public class AIKEResult<T> {
    private HashMap<Object, Object> data;
    private String message;
    private Integer resultCode;
    private Integer status;


    //文件上传参数返回必须字段 文档图片上传必要参数
    // private Integer errno = 0;
    public AIKEResult() {
        System.out.println();
    }

    public AIKEResult(HashMap<Object, Object> data) {
        this.data = data;
    }

    public static AIKEResult success() {
        AIKEResult result = new AIKEResult<>();
        result.setResultCode(0);
        result.setStatus(1);
        result.setMessage("ok");
        return result;
    }

    public static AIKEResult successOpenGate() {
        AIKEResult result = new AIKEResult<>();
        return result;
    }

    public static <T> AIKEResult<T> success(HashMap<Object, Object> data) {
        AIKEResult<T> result = new AIKEResult<>(data);
        result.setData(data);
//        result.setMessage("ok"); result.setMessage("ok);
        // result.setResultCode(0);
        result.setMessage("ok");
        result.setResultCode(0);
        result.setStatus(1);
        return result;
    }

    public static AIKEResult error(Integer resultCode, String message) {
        AIKEResult result = new AIKEResult();
        result.setResultCode(resultCode);
        result.setMessage(message);
        return result;
    }

    public static AIKEResult error(String message) {
        AIKEResult result = new AIKEResult();
        result.setResultCode(-1);
        result.setMessage(message);
        return result;
    }
}
