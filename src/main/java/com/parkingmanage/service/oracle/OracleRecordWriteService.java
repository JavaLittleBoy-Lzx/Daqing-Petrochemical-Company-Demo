package com.parkingmanage.service.oracle;

import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.dto.well.WellGateRecordResponse;
import com.parkingmanage.util.GateCodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Oracle进出场记录写入服务
 * 负责将AKE车辆记录和威尔门禁人员记录写入Oracle数据库
 * 
 * 车辆表：AENTRANCEGUARD.AUTOINOUTAKEINFO
 * 人员表：PENTRANCEGUARD.PERSONINOUTAKEINFO
 */
@Slf4j
@Service
public class OracleRecordWriteService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 人员照片URL前缀
    private static final String PERSON_PHOTO_PREFIX = "http://11.114.34.25:8000";
    
    // 车辆照片URL前缀
    private static final String VEHICLE_PHOTO_PREFIX = "http://11.114.34.28:8092";
    
    // 记录号序列计数器（用于同一秒内的多条记录）
    private static final AtomicInteger recordSequence = new AtomicInteger(0);
    private static volatile long lastTimestamp = 0;

    /**
     * 车辆权限信息（从 view_autovalidinfo 视图查询）
     */
    @lombok.Data
    public static class VehicleAuthInfo {
        /** 登记记录号 */
        private String recordnoL;
        /** 卡号 */
        private String kh;
        /** 卡类型（KLX）- A=长期卡，D=临时卡 */
        private String klx;
        /** 车辆种类代码 */
        private String clzl;
        /** 车辆类型代码 */
        private String cllx;
        /** 号牌颜色 */
        private String hpys;
        /** 品牌型号 */
        private String ppxh;
        /** 单位名称 */
        private String dwmc;
    }

    /**
     * 写入AKE车辆进场记录
     * 
     * @param data AKE进场记录JSON数据
     * @return 是否写入成功
     */
    public boolean writeVehicleInRecord(JSONObject data) {
        try {
            if (data == null || !data.containsKey("biz_content")) {
                log.warn("AKE进场记录数据为空或格式错误");
                return false;
            }
            
            JSONObject bizContent = data.getJSONObject("biz_content");
            
            // 提取关键字段
            String carLicenseNumber = bizContent.getString("car_license_number");
            String enterChannelName = bizContent.getString("enter_channel_name");
            String enterTime = bizContent.getString("enter_time");
            String enterCarLicenseColor = bizContent.getString("enter_car_license_color");
            String enterCarFullPicture = bizContent.getString("enter_car_full_picture");
            String inOperatorName = bizContent.getString("in_operator_name");
            String enterCustomVipName = bizContent.getString("enter_custom_vip_name");
            
            // 根据大门名称获取厂区和大门编码
            GateCodeMapper.GateCode gateCode = GateCodeMapper.getVehicleGateCode(enterChannelName);
            if (gateCode == null) {
                log.warn("未找到大门编码，跳过写入: 大门名称={}", enterChannelName);
                return false;
            }
            
            // 检查是否重复（车牌号+通行时间相同）
            if (isDuplicateVehicleRecord(carLicenseNumber, enterTime)) {
                log.info("车辆进场记录已存在，跳过写入: 车牌={}, 时间={}", carLicenseNumber, enterTime);
                return false;
            }
            
            // 生成记录号（14位数字）
            String recordNo = generateRecordNo();

            // 判断放行类别：VIP类型名称为"请停车检查"时设置为"01"，否则为空
            String fxlb = "请停车检查".equals(enterCustomVipName) ? "01" : null;

            // 处理照片URL（添加前缀）
            String photoUrl = null;
            if (enterCarFullPicture != null && !enterCarFullPicture.trim().isEmpty()) {
                photoUrl = VEHICLE_PHOTO_PREFIX + enterCarFullPicture;
            }

            // 通过车牌号查询车辆权限信息
            VehicleAuthInfo authInfo = queryVehicleAuthInfo(carLicenseNumber);

            // 卡类型：优先使用权限视图中的值，否则默认设置为长期卡A
            String klx = (authInfo != null && authInfo.getKlx() != null)
                    ? authInfo.getKlx()
                    : "A";

            // 转换车牌颜色：优先使用权限视图中的值，否则使用AKE返回的值
            String hpys = (authInfo != null && authInfo.getHpys() != null)
                    ? authInfo.getHpys()
                    : convertPlateColor(enterCarLicenseColor);

            // 从权限视图获取的字段（如果视图中有数据则使用，否则为null）
            String recordnoL = authInfo != null ? authInfo.getRecordnoL() : null;
            String kh = authInfo != null ? authInfo.getKh() : null;
            String clzl = authInfo != null ? authInfo.getClzl() : null;
            String cllx = authInfo != null ? authInfo.getCllx() : null;
            String ppxh = authInfo != null ? authInfo.getPpxh() : null;
            String dwmc = authInfo != null ? authInfo.getDwmc() : null;

            // 写入数据库
            String sql = "INSERT INTO AENTRANCEGUARD.AUTOINOUTAKEINFO " +
                        "(RECORDNO, KLX, CPHM, CQ, JCCDM, JCCSJ, JCCSBCPHM, FXMWXM, JCCBZ, FXLB, HPYS, ZPURL, " +
                        "RECORDNOL, KH, CLZL, CLLX, PPXH, DWMC) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    recordNo,                    // RECORDNO - 记录号
                    klx,                         // KLX - 卡类型（A-长期卡）
                    carLicenseNumber,            // CPHM - 车牌号码
                    gateCode.getAreaCode(),      // CQ - 厂区编码
                    gateCode.getGateCode(),      // JCCDM - 大门编码
                    enterTime,                   // JCCSJ - 进场时间
                    carLicenseNumber,            // JCCSBCPHM - 识别车牌号码
                    inOperatorName,              // FXMWXM - 放行人姓名
                    "1",                         // JCCBZ - 进场标志（1-进场）
                    fxlb,                        // FXLB - 放行类别（VIP名称="请停车检查"时为"01"）
                    hpys,                        // HPYS - 号牌颜色（优先使用权限视图）
                    photoUrl,                    // ZPURL - 照片路径（带前缀）
                    recordnoL,                   // RECORDNOL - 登记记录号（从权限视图查询）
                    kh,                          // KH - 卡号（从权限视图查询）
                    clzl,                        // CLZL - 车辆种类代码（从权限视图查询）
                    cllx,                        // CLLX - 车辆类型代码（从权限视图查询）
                    ppxh,                        // PPXH - 品牌型号（从权限视图查询）
                    dwmc                         // DWMC - 单位名称（从权限视图查询）
            );

            log.info("✅ 车辆进场记录写入成功: 车牌={}, 大门={}, 时间={}, VIP类型={}, 放行类别={}, KLX={}, RECORDNOL={}, KH={}, DWMC={}",
                    carLicenseNumber, enterChannelName, enterTime, enterCustomVipName, fxlb, klx, recordnoL, kh, dwmc);
            return true;
            
        } catch (DataAccessException e) {
            log.error("❌ 车辆进场记录写入失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 写入AKE车辆离场记录
     * 
     * @param data AKE离场记录JSON数据
     * @return 是否写入成功
     */
    public boolean writeVehicleOutRecord(JSONObject data) {
        try {
            if (data == null || !data.containsKey("biz_content")) {
                log.warn("AKE离场记录数据为空或格式错误");
                return false;
            }
            
            JSONObject bizContent = data.getJSONObject("biz_content");
            
            // 提取关键字段
            String carLicenseNumber = bizContent.getString("car_license_number");
            String leaveChannelName = bizContent.getString("leave_channel_name");
            String leaveTime = bizContent.getString("leave_time");
            String leaveCarLicenseColor = bizContent.getString("leave_car_license_color");
            String leaveCarFullPicture = bizContent.getString("leave_car_full_picture");
            String outOperatorName = bizContent.getString("out_operator_name");
            String leaveCustomVipName = bizContent.getString("leave_custom_vip_name");
            
            // 根据大门名称获取厂区和大门编码
            GateCodeMapper.GateCode gateCode = GateCodeMapper.getVehicleGateCode(leaveChannelName);
            if (gateCode == null) {
                log.warn("未找到大门编码，跳过写入: 大门名称={}", leaveChannelName);
                return false;
            }
            
            // 检查是否重复（车牌号+通行时间相同）
            if (isDuplicateVehicleRecord(carLicenseNumber, leaveTime)) {
                log.info("车辆离场记录已存在，跳过写入: 车牌={}, 时间={}", carLicenseNumber, leaveTime);
                return false;
            }
            
            // 生成记录号（14位数字）
            String recordNo = generateRecordNo();

            // 判断放行类别：VIP类型名称为"请停车检查"时设置为"01"，否则为空
            String fxlb = "请停车检查".equals(leaveCustomVipName) ? "01" : null;

            // 处理照片URL（添加前缀）
            String photoUrl = null;
            if (leaveCarFullPicture != null && !leaveCarFullPicture.trim().isEmpty()) {
                photoUrl = VEHICLE_PHOTO_PREFIX + leaveCarFullPicture;
            }

            // 通过车牌号查询车辆权限信息
            VehicleAuthInfo authInfo = queryVehicleAuthInfo(carLicenseNumber);

            // 卡类型：优先使用权限视图中的值，否则默认设置为长期卡A
            String klx = (authInfo != null && authInfo.getKlx() != null)
                    ? authInfo.getKlx()
                    : "A";

            // 转换车牌颜色：优先使用权限视图中的值，否则使用AKE返回的值
            String hpys = (authInfo != null && authInfo.getHpys() != null)
                    ? authInfo.getHpys()
                    : convertPlateColor(leaveCarLicenseColor);

            // 从权限视图获取的字段（如果视图中有数据则使用，否则为null）
            String recordnoL = authInfo != null ? authInfo.getRecordnoL() : null;
            String kh = authInfo != null ? authInfo.getKh() : null;
            String clzl = authInfo != null ? authInfo.getClzl() : null;
            String cllx = authInfo != null ? authInfo.getCllx() : null;
            String ppxh = authInfo != null ? authInfo.getPpxh() : null;
            String dwmc = authInfo != null ? authInfo.getDwmc() : null;

            // 写入数据库
            String sql = "INSERT INTO AENTRANCEGUARD.AUTOINOUTAKEINFO " +
                        "(RECORDNO, KLX, CPHM, CQ, JCCDM, JCCSJ, JCCSBCPHM, FXMWXM, JCCBZ, FXLB, HPYS, ZPURL, " +
                        "RECORDNOL, KH, CLZL, CLLX, PPXH, DWMC) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    recordNo,                    // RECORDNO - 记录号
                    klx,                         // KLX - 卡类型（A-长期卡）
                    carLicenseNumber,            // CPHM - 车牌号码
                    gateCode.getAreaCode(),      // CQ - 厂区编码
                    gateCode.getGateCode(),      // JCCDM - 大门编码
                    leaveTime,                   // JCCSJ - 离场时间
                    carLicenseNumber,            // JCCSBCPHM - 识别车牌号码
                    outOperatorName,             // FXMWXM - 放行人姓名
                    "2",                         // JCCBZ - 离场标志（2-离场）
                    fxlb,                        // FXLB - 放行类别（VIP名称="请停车检查"时为"01"）
                    hpys,                        // HPYS - 号牌颜色（优先使用权限视图）
                    photoUrl,                    // ZPURL - 照片路径（带前缀）
                    recordnoL,                   // RECORDNOL - 登记记录号（从权限视图查询）
                    kh,                          // KH - 卡号（从权限视图查询）
                    clzl,                        // CLZL - 车辆种类代码（从权限视图查询）
                    cllx,                        // CLLX - 车辆类型代码（从权限视图查询）
                    ppxh,                        // PPXH - 品牌型号（从权限视图查询）
                    dwmc                         // DWMC - 单位名称（从权限视图查询）
            );

            log.info("✅ 车辆离场记录写入成功: 车牌={}, 大门={}, 时间={}, VIP类型={}, 放行类别={}, KLX={}, RECORDNOL={}, KH={}, DWMC={}",
                    carLicenseNumber, leaveChannelName, leaveTime, leaveCustomVipName, fxlb, klx, recordnoL, kh, dwmc);
            return true;
            
        } catch (DataAccessException e) {
            log.error("❌ 车辆离场记录写入失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 写入威尔门禁人员记录
     * 
     * @param record 威尔门禁记录
     * @return 是否写入成功
     */
    public boolean writePersonRecord(WellGateRecordResponse record) {
        try {
            if (record == null) {
                log.warn("威尔门禁记录为空");
                return false;
            }
            
            // 提取关键字段
            String userName = record.getUserName();
            String doorName = record.getDoorName();
            String deviceName = record.getDeviceName();
            String recTime = record.getRecTime();
            String recDic = record.getRecDic();
            String userNo = record.getUserNo();
            String recPhoto = record.getRecPhoto();
            String recType = record.getRecType();
            
            // 根据大门名称获取厂区和大门编码
            GateCodeMapper.GateCode gateCode = GateCodeMapper.getPersonGateCode(doorName);
            if (gateCode == null) {
                log.warn("未找到大门编码，跳过写入: 大门名称={}", doorName);
                return false;
            }
            
            // 检查是否重复（姓名+通行时间相同）
            if (isDuplicatePersonRecord(userName, recTime)) {
                log.info("人员进出记录已存在，跳过写入: 姓名={}, 时间={}", userName, recTime);
                return false;
            }
            
            // 生成记录号（14位数字）
            String recordNo = generateRecordNo();
            
            // 处理照片URL（添加前缀）
            String photoUrl = null;
            if (recPhoto != null && !recPhoto.trim().isEmpty()) {
                photoUrl = PERSON_PHOTO_PREFIX + recPhoto;
            }
            
            // 判断进出标志（0-进门 1-出门）
            String jccbz = "0".equals(recDic) ? "1" : "2";
            
            // 判断开门方式：记录类型为"人脸识别"时设置为199，其他方式为空
            String kmfs = "人脸识别".equals(recType) ? "199" : null;

            // 卡类型：默认设置为长期卡A
            String klx = "A";
            
            // 人员类型：默认设置为外来人员5（因为威尔数据中没有人员类型信息）
            String rylx = "5";
            
            // 人员ID：直接使用工号
            String ryid = userNo;
            
            // 处理标志：默认为0（未处理）
            String clbz = "0";
            
            // 进出通道：将设备名称格式化（如：化工西门1号入口 -> 化工西进1）
            String jctd = formatChannelName(deviceName);
            
            // 写入数据库（根据Oracle实际字段）
            String sql = "INSERT INTO PENTRANCEGUARD.PERSONINOUTAKEINFO " +
                        "(RECORDNO, KLX, XM, YXM, RYLX, RYID, DWMC, JCCBZ, JCDM, JCSJ, KMFS, CQ, XB, JCTD, CLBZ, ZPURL) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            jdbcTemplate.update(sql,
                    recordNo,                    // RECORDNO - 记录号
                    klx,                         // KLX - 卡类型（A-长期卡）
                    userName,                    // XM - 姓名
                    null,                        // YXM - 音序码（暂无）
                    rylx,                        // RYLX - 人员类型（5-外来人员）
                    ryid,                        // RYID - 人员ID（使用工号）
                    null,                        // DWMC - 单位名称（暂无）
                    jccbz,                       // JCCBZ - 进出标志（1-进 2-出）
                    gateCode.getGateCode(),      // JCDM - 大门编码
                    recTime,                     // JCSJ - 进出时间
                    kmfs,                        // KMFS - 开门方式（199-人脸）
                    gateCode.getAreaCode(),      // CQ - 厂区编码
                    null,                        // XB - 性别（暂无）
                    jctd,                        // JCTD - 进出通道（使用大门名称）
                    clbz,                        // CLBZ - 处理标志（0-未处理）
                    photoUrl                     // ZPURL - 照片路径
            );
            
            log.info("✅ 人员进出记录写入成功: 姓名={}, 工号={}, 大门={}, 时间={}, 方向={}", 
                    userName, userNo, doorName, recTime, jccbz.equals("1") ? "进" : "出");
            return true;
            
        } catch (DataAccessException e) {
            log.error("❌ 人员进出记录写入失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查车辆记录是否重复
     * 重复规则：车牌号+通行时间相同
     * 
     * @param plateNumber 车牌号
     * @param passTime 通行时间
     * @return true表示重复
     */
    private boolean isDuplicateVehicleRecord(String plateNumber, String passTime) {
        try {
            String sql = "SELECT COUNT(*) FROM AENTRANCEGUARD.AUTOINOUTAKEINFO " +
                        "WHERE CPHM = ? AND JCCSJ = ?";
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, plateNumber, passTime);
            return count != null && count > 0;
            
        } catch (DataAccessException e) {
            log.error("检查车辆记录重复失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查人员记录是否重复
     * 重复规则：姓名+通行时间相同
     * 
     * @param userName 姓名
     * @param passTime 通行时间
     * @return true表示重复
     */
    private boolean isDuplicatePersonRecord(String userName, String passTime) {
        try {
            String sql = "SELECT COUNT(*) FROM PENTRANCEGUARD.PERSONINOUTAKEINFO " +
                        "WHERE XM = ? AND JCSJ = ?";
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userName, passTime);
            return count != null && count > 0;
            
        } catch (DataAccessException e) {
            log.error("检查人员记录重复失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 生成唯一记录号（14位数字）
     * 格式：yyyyMMddHHmmss
     * 
     * 为确保唯一性：
     * 1. 如果在同一秒内生成多个记录号，会自动递增秒数
     * 2. 使用synchronized确保线程安全
     * 
     * @return 14位唯一记录号
     */
    private synchronized String generateRecordNo() {
        long currentTimestamp = System.currentTimeMillis() / 1000; // 秒级时间戳
        
        // 如果是同一秒，序列号递增
        if (currentTimestamp == lastTimestamp) {
            int seq = recordSequence.incrementAndGet();
            // 如果序列号超过999，等待下一秒
            if (seq > 999) {
                try {
                    Thread.sleep(1000);
                    currentTimestamp = System.currentTimeMillis() / 1000;
                    lastTimestamp = currentTimestamp;
                    recordSequence.set(0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            // 新的一秒，重置序列号
            lastTimestamp = currentTimestamp;
            recordSequence.set(0);
        }
        
        // 生成14位记录号
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 转换车牌颜色
     * 
     * @param colorName 颜色名称（中文或英文）
     * @return 颜色代码（blue/green/white/yellow）
     */
    private String convertPlateColor(String colorName) {
        if (colorName == null || colorName.trim().isEmpty()) {
            return null;
        }

        String color = colorName.trim().toLowerCase();
        if (color.contains("蓝") || color.contains("blue")) {
            return "blue";
        } else if (color.contains("绿") || color.contains("green")) {
            return "green";
        } else if (color.contains("白") || color.contains("white")) {
            return "white";
        } else if (color.contains("黄") || color.contains("yellow")) {
            return "yellow";
        }

        return null;
    }

    /**
     * 格式化通道名称
     * 将威尔门禁的设备名称转换为简化的通道名称
     *
     * 转换规则：
     * - 化工西门1号入口 -> 化工西进1
     * - 化工西门1号出口 -> 化工西出1
     * - 炼油南门2号入口 -> 炼油南进2
     * - 化肥北门1号出口 -> 化肥北出1
     *
     * @param deviceName 威尔门禁返回的设备名称（如：化工西门1号入口）
     * @return 格式化后的通道名称（如：化工西进1），如果无法解析返回原名称
     */
    private String formatChannelName(String deviceName) {
        if (deviceName == null || deviceName.trim().isEmpty()) {
            return null;
        }

        String name = deviceName.trim();

        try {
            // 提取数字（通道号）
            String channelNo = "";
            for (char c : name.toCharArray()) {
                if (Character.isDigit(c)) {
                    channelNo += c;
                }
            }

            // 判断进出方向
            String direction = "";
            if (name.contains("入口")) {
                direction = "进";
            } else if (name.contains("出口")) {
                direction = "出";
            }

            // 简化厂区+大门名称
            String baseName = name
                    .replace("号", "")
                    .replace("入口", "")
                    .replace("出口", "")
                    .replace("1", "")
                    .replace("2", "")
                    .replace("3", "")
                    .replace("4", "")
                    .trim();

            // 进一步简化：去掉最后一个字（门）
            String shortName = "";
            if (baseName.endsWith("门") && baseName.length() > 1) {
                shortName = baseName.substring(0, baseName.length() - 1);
            } else {
                shortName = baseName;
            }

            // 如果没有提取到通道号或方向，返回原名称
            if (channelNo.isEmpty() || direction.isEmpty()) {
                log.warn("无法解析通道名称，返回原值: {}", deviceName);
                return deviceName;
            }

            return shortName + direction + channelNo;

        } catch (Exception e) {
            log.error("格式化通道名称异常: {}, 返回原值", deviceName, e);
            return deviceName;
        }
    }

    /**
     * 通过车牌号查询车辆权限信息
     * 从车辆权限视图 aentranceguard.view_autovalidinfo 中查询
     *
     * @param plateNumber 车牌号
     * @return 车辆权限信息，查询不到返回null
     */
    private VehicleAuthInfo queryVehicleAuthInfo(String plateNumber) {
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            return null;
        }

        try {
            // 使用 LIKE 模糊查询（去除汉字）
            // 例如：黑E12345 -> %E12345
            String likePattern = "%" + plateNumber.trim().replaceAll("[\\u4e00-\\u9fa5]", "");

            String sql = "SELECT RECORDNOL, KH, KLX, CLZL, CLLX, HPYS, PPXH, DWMCNAME " +
                        "FROM aentranceguard.view_autovalidinfo " +
                        "WHERE CPHM LIKE ? AND ROWNUM = 1 " +
                        "ORDER BY CZSJ DESC";  // 取最近操作的记录

            VehicleAuthInfo authInfo = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                VehicleAuthInfo info = new VehicleAuthInfo();
                info.setRecordnoL(rs.getString("RECORDNOL"));
                info.setKh(rs.getString("KH"));
                info.setKlx(rs.getString("KLX"));
                info.setClzl(rs.getString("CLZL"));
                info.setCllx(rs.getString("CLLX"));
                info.setHpys(rs.getString("HPYS"));
                info.setPpxh(rs.getString("PPXH"));
                info.setDwmc(rs.getString("DWMCNAME"));
                return info;
            }, likePattern);

            if (authInfo != null) {
                log.debug("通过车牌号查询到权限信息: 车牌={}, RECORDNOL={}, KH={}, KLX={}, DWMC={}",
                        plateNumber, authInfo.getRecordnoL(), authInfo.getKh(), authInfo.getKlx(), authInfo.getDwmc());
            }

            return authInfo;

        } catch (Exception e) {
            // 查询不到或查询异常都返回null
            log.debug("通过车牌号查询权限信息失败: 车牌={}, {}", plateNumber, e.getMessage());
            return null;
        }
    }
}
