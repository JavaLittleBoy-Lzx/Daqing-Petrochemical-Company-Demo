package com.parkingmanage.common;

import lombok.Data;

//响应头
@Data
public class AIKEResultOpenGates<T> {
    private T data;
    //文件上传参数返回必须字段 文档图片上传必要参数
    // private Integer errno = 0;
    public AIKEResultOpenGates() {
    }

    public AIKEResultOpenGates(T data) {
        this.data = data;
    }
}
