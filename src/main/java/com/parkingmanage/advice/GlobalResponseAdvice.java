package com.parkingmanage.advice;


import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.AIKEResult;
import com.parkingmanage.common.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 捕获异常统一处理
 * 全局返回参数处理
 */
@ControllerAdvice(basePackages = {"com.parkingmanage.controller"})
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    /**
     * @param o
     * @param methodParameter
     * @param mediaType
     * @param aClass
     * @param serverHttpRequest
     * @param serverHttpResponse
     * @return {@link Object}
     */
    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // 微信接口需要返回原始字符串，不能包装
        String requestPath = serverHttpRequest.getURI().getPath();
        if (requestPath != null && requestPath.contains("/wechat/")) {
            return o;
        }
        
        // AKE接口需要返回原始JSON格式，不能包装
        if (requestPath != null && (requestPath.contains("/reportCarIn") || requestPath.contains("/reportCarOut"))) {
            return o;
        }
        
        // JSONObject类型直接返回，不包装
        if (o instanceof JSONObject) {
            return o;
        }
        
        if (o instanceof AIKEResult) {
            return o;
        }
        
        // 如果已经是Result类型，直接返回，避免双层嵌套
        if (o instanceof Result) {
            return o;
        }
        
        return Result.success(o);
    }
}
