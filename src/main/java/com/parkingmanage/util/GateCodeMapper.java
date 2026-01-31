package com.parkingmanage.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 厂区大门编码映射工具类
 * 根据大门名称查找对应的厂区编码(CQ)和大门编码(JCCDM/JCDM)
 * 
 * 注意：车辆表和人员表的编码略有不同
 */
@Slf4j
public class GateCodeMapper {

    /**
     * 车辆表厂区大门编码映射
     * 表：AENTRANCEGUARD.AUTOINOUTAKEINFO
     */
    private static final Map<String, GateCode> VEHICLE_GATE_MAP = new HashMap<>();
    
    /**
     * 人员表厂区大门编码映射
     * 表：PENTRANCEGUARD.PERSONINOUTAKEINFO
     */
    private static final Map<String, GateCode> PERSON_GATE_MAP = new HashMap<>();
    
    static {
        // 初始化车辆表编码映射
        // 01-炼油
        VEHICLE_GATE_MAP.put("炼油南门", new GateCode("01", "0101", "炼油", "炼油南门"));
        VEHICLE_GATE_MAP.put("炼油南一", new GateCode("01", "0102", "炼油", "炼油南一"));
        VEHICLE_GATE_MAP.put("炼油西一", new GateCode("01", "0104", "炼油", "炼油西一"));
        VEHICLE_GATE_MAP.put("炼油西门", new GateCode("01", "0105", "炼油", "炼油西门"));
        VEHICLE_GATE_MAP.put("炼油东门", new GateCode("01", "0106", "炼油", "炼油东门"));
        
        // 02-化肥
        VEHICLE_GATE_MAP.put("化肥西门", new GateCode("02", "0201", "化肥", "化肥西门"));
        
        // 03-化工
        VEHICLE_GATE_MAP.put("化工西门", new GateCode("03", "0301", "化工", "化工西门"));
        VEHICLE_GATE_MAP.put("化工北门", new GateCode("03", "0302", "化工", "化工北门"));
        VEHICLE_GATE_MAP.put("化工东门", new GateCode("03", "0304", "化工", "化工东门"));
        
        // 04-化三
        VEHICLE_GATE_MAP.put("化三南门", new GateCode("04", "0401", "化三", "化三南门"));
        VEHICLE_GATE_MAP.put("化三西二", new GateCode("04", "0402", "化三", "化三西二"));
        
        // 05-复合肥
        VEHICLE_GATE_MAP.put("复合肥南门", new GateCode("05", "0501", "复合肥", "复合肥南门"));
        
        // 06-生活水
        VEHICLE_GATE_MAP.put("生活水正门", new GateCode("06", "0601", "生活水", "生活水正门"));
        
        // 07-净化水
        VEHICLE_GATE_MAP.put("净化水正门", new GateCode("07", "0701", "净化水", "净化水正门"));
        
        // 08-机械厂
        VEHICLE_GATE_MAP.put("机械厂正门", new GateCode("08", "0801", "机械厂", "机械厂正门"));
        
        // 09-机修厂
        VEHICLE_GATE_MAP.put("机修厂西一", new GateCode("09", "0901", "机修厂", "机修厂西一"));
        
        // 10-指挥中心
        VEHICLE_GATE_MAP.put("指挥中心正门", new GateCode("10", "1001", "指挥中心", "指挥中心正门"));
        
        // 11-二罐区
        VEHICLE_GATE_MAP.put("二罐区正门", new GateCode("11", "1101", "二罐区", "二罐区正门"));
        
        // 初始化人员表编码映射
        // 01-炼油
        PERSON_GATE_MAP.put("炼油南门", new GateCode("01", "0101", "炼油", "炼油南门"));
        PERSON_GATE_MAP.put("炼油南一", new GateCode("01", "0102", "炼油", "炼油南一"));
        PERSON_GATE_MAP.put("炼油西一", new GateCode("01", "0104", "炼油", "炼油西一"));
        PERSON_GATE_MAP.put("炼油西门", new GateCode("01", "0105", "炼油", "炼油西门"));
        PERSON_GATE_MAP.put("炼油东门", new GateCode("01", "0106", "炼油", "炼油东门"));
        
        // 02-化肥
        PERSON_GATE_MAP.put("化肥西门", new GateCode("02", "0201", "化肥", "化肥西门"));
        PERSON_GATE_MAP.put("化肥北门", new GateCode("02", "0202", "化肥", "化肥北门"));
        
        // 03-化工
        PERSON_GATE_MAP.put("化工西门", new GateCode("03", "0301", "化工", "化工西门"));
        PERSON_GATE_MAP.put("化工北门", new GateCode("03", "0302", "化工", "化工北门"));
        PERSON_GATE_MAP.put("化工东门", new GateCode("03", "0304", "化工", "化工东门"));
        
        // 04-化三
        PERSON_GATE_MAP.put("化三南门", new GateCode("04", "0401", "化三", "化三南门"));
        PERSON_GATE_MAP.put("化三西二", new GateCode("04", "0402", "化三", "化三西二"));
        
        // 05-复合肥
        PERSON_GATE_MAP.put("复合肥南门", new GateCode("05", "0501", "复合肥", "复合肥南门"));
        
        // 09-机修厂
        PERSON_GATE_MAP.put("机修厂西一", new GateCode("09", "0901", "机修厂", "机修厂西一"));
        
        // 10-指挥内部
        PERSON_GATE_MAP.put("指挥内部正门", new GateCode("10", "1001", "指挥内部", "指挥内部正门"));
        
        // 11-二罐区
        PERSON_GATE_MAP.put("二罐区正门", new GateCode("11", "1101", "二罐区", "二罐区正门"));
        
        // 12-一罐区
        PERSON_GATE_MAP.put("一罐区正门", new GateCode("12", "1201", "一罐区", "一罐区正门"));
        
        // 13-北油库
        PERSON_GATE_MAP.put("北油库正门", new GateCode("13", "1301", "北油库", "北油库正门"));
        
        // 14-化肥北
        PERSON_GATE_MAP.put("化肥北北门", new GateCode("14", "1401", "化肥北", "化肥北北门"));
        
        // 15-指挥外部
        PERSON_GATE_MAP.put("指挥外部正门", new GateCode("15", "1501", "指挥外部", "指挥外部正门"));
    }
    
    /**
     * 根据大门名称获取车辆表的厂区大门编码
     * 
     * @param gateName 大门名称（如：化工西门、炼油南门）
     * @return 厂区大门编码，如果未找到返回null
     */
    public static GateCode getVehicleGateCode(String gateName) {
        if (gateName == null || gateName.trim().isEmpty()) {
            log.warn("大门名称为空，无法获取车辆表编码");
            return null;
        }
        
        // 清理大门名称：
        // 1. 去除"入口"、"出口"等后缀
        // 2. 去除末尾的"进"或"出"加数字（如"进1"、"出2"等新格式）
        // 3. 去除数字编号（如"9号"、"1号"等）
        String cleanName = gateName.trim()
                .replace("入口", "")
                .replace("出口", "")
                .replaceAll("[进出]\\d*$", "")  // 去除末尾的"进"/"出"+数字（如：进1、出2）
                .replaceAll("\\d+号", "")       // 去除数字+号（如：9号、1号）
                .trim();
        
        GateCode code = VEHICLE_GATE_MAP.get(cleanName);
        if (code == null) {
            log.warn("未找到车辆表大门编码: {} (清理后: {})", gateName, cleanName);
        }
        return code;
    }
    
    /**
     * 根据大门名称获取人员表的厂区大门编码
     * 
     * @param gateName 大门名称（如：化工西门、炼油南门）
     * @return 厂区大门编码，如果未找到返回null
     */
    public static GateCode getPersonGateCode(String gateName) {
        if (gateName == null || gateName.trim().isEmpty()) {
            log.warn("大门名称为空，无法获取人员表编码");
            return null;
        }
        
        // 清理大门名称：
        // 1. 去除"入口"、"出口"等后缀
        // 2. 去除末尾的"进"或"出"加数字（如"进1"、"出2"等新格式）
        // 3. 去除数字编号（如"9号"、"1号"等）
        String cleanName = gateName.trim()
                .replace("入口", "")
                .replace("出口", "")
                .replaceAll("[进出]\\d*$", "")  // 去除末尾的"进"/"出"+数字（如：进1、出2）
                .replaceAll("\\d+号", "")       // 去除数字+号（如：9号、1号）
                .trim();
        
        GateCode code = PERSON_GATE_MAP.get(cleanName);
        if (code == null) {
            log.warn("未找到人员表大门编码: {} (清理后: {})", gateName, cleanName);
        }
        return code;
    }
    
    /**
     * 厂区大门编码实体
     */
    public static class GateCode {
        /** 厂区编码（2位） */
        private final String areaCode;
        
        /** 大门编码（4位） */
        private final String gateCode;
        
        /** 厂区名称 */
        private final String areaName;
        
        /** 大门名称 */
        private final String gateName;
        
        public GateCode(String areaCode, String gateCode, String areaName, String gateName) {
            this.areaCode = areaCode;
            this.gateCode = gateCode;
            this.areaName = areaName;
            this.gateName = gateName;
        }
        
        public String getAreaCode() {
            return areaCode;
        }
        
        public String getGateCode() {
            return gateCode;
        }
        
        public String getAreaName() {
            return areaName;
        }
        
        public String getGateName() {
            return gateName;
        }
        
        @Override
        public String toString() {
            return String.format("GateCode{厂区=%s(%s), 大门=%s(%s)}", 
                    areaName, areaCode, gateName, gateCode);
        }
    }
}
