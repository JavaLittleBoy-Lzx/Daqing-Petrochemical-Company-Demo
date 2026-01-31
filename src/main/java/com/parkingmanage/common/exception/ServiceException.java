package com.parkingmanage.common.exception;

/**
 * @program: ParkManage
 * @description: 登录异常
 * @author: lzx
 * @create: 2023-11-17 14:40
 **/
public class ServiceException extends RuntimeException {
    public ServiceException(String msg) {
        super(msg);
    }
}
