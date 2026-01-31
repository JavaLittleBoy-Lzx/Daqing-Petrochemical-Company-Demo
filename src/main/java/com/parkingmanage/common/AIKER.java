package com.parkingmanage.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName R
 * @Description TODO
 * @Author lzx
 * @Date 2023/8/7 17:21:42
 * @Version 1.0
 **/
@Data
public class AIKER {
//    @ApiModelProperty(value = "是否成功")
//    private Boolean success;
    @ApiModelProperty(value = "返回码")
    private Integer resultCode;
    @ApiModelProperty(value = "返回消息")
    private String message;
    @ApiModelProperty(value = "状态码")
    private Integer status;
    @ApiModelProperty(value = "返回数据")
    private HashMap<Object, Object> data;

    public AIKER() {}

    public static AIKER ok() {
        AIKER r = new AIKER();
        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        r.setData(objectObjectHashMap);
//        r.setSuccess(true);
        r.setResultCode(0);
        r.setMessage("ok");
        r.setStatus(1);
        return r;
    }

    public static AIKER error() {
        AIKER r = new AIKER();
//        r.setSuccess(false);
        r.setResultCode(AIKEResultCode.ERROR);
        r.setMessage("业务失败");
        return r;
    }


    public AIKER message(String message) {
        this.setMessage(message);
        return this;
    }

    public AIKER code(Integer code) {
        this.setResultCode(code);
        return this;
    }

    public AIKER data() {
        return this.data();
    }

    public AIKER data(Map<String, Object> map) {
//        this.setData(map);
        return this;
    }
}