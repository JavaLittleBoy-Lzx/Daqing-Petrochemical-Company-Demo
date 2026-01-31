package com.parkingmanage.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    public static String mapToString(Map<String, Object> map) {
        if(map == null) {
            return "{}";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        for(String s : map.keySet()) {
            sb.append(s+":\""+map.get(s)+"\",");
        }
        sb.replace(sb.length()-1, sb.length(), "}");
        return sb.toString();
    }
    //定义jackson对象
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /**
     * 将对象转化为json字符串
     * @param data
     * @return
     */
    public static String ObjectToJson(Object data){
        try{
            String string = MAPPER.writeValueAsString(data);
             return string;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 将json字符串转化为对象
     * @param jsonData
     * @param beanType
     * @param <T>
     * @return
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType){
        try {
            T t = MAPPER.readValue(jsonData, beanType);
            return t;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 将json字符串数据转化为pojo对象list
     * @param jsonData
     * @param beanType
     * @param <T>
     * @return
     */
    public static <T> List<T> jsonToList(String jsonData, Class<T> beanType){
        JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
        try {
            List<T> list = MAPPER.readValue(jsonData, javaType);
            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
