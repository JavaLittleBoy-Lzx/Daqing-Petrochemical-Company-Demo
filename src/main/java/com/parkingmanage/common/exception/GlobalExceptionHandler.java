package com.parkingmanage.common.exception;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.parkingmanage.common.AIKEResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Log log = LogFactory.get();

    //统一异常处理@ExceptionHandler,主要用于Exception
    @ExceptionHandler(Exception.class)
    @ResponseBody//返回json串
    public AIKEResult<?> error(HttpServletRequest request, Exception e) {
        log.error("异常信息：", e);
        return AIKEResult.error(-1, "系统异常");
    }
    @ExceptionHandler(CustomException.class)
    @ResponseBody//返回json串
    public AIKEResult<?> customError(HttpServletRequest request, CustomException e) {
        return AIKEResult.error(1, e.getMsg());
    }
    @ExceptionHandler(ServiceException.class)
    @ResponseBody//返回json串
    public AIKEResult<?> serviceError(ServiceException e) {
        return AIKEResult.error(500,e.getMessage());
    }
}
