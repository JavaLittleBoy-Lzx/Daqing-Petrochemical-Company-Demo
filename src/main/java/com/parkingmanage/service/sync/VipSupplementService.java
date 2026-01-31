package com.parkingmanage.service.sync;

import com.parkingmanage.dto.ake.OpenVipTicketRequest;
import com.parkingmanage.service.ake.AkeVipService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * VIP补开服务
 * 用于为指定车牌补开"请停车检查(化工西化肥西复合肥南)"VIP月票
 */
@Slf4j
@Service
public class VipSupplementService {

    @Autowired
    private AkeVipService akeVipService;

    @Value("${ake.default-operator:系统同步}")
    private String defaultOperator;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 目标VIP类型名称
    private static final String TARGET_VIP_TYPE = "请停车检查（化工西化肥西复合肥南）";
    // 源VIP类型名称
    private static final String SOURCE_VIP_TYPE = "停用tcjc";

    /**
     * 补开结果
     */
    @Data
    public static class SupplementResult {
        /** 处理的车牌总数 */
        private int totalPlateCount = 0;
        /** 成功补开的数量 */
        private int successCount = 0;
        /** 失败的数量 */
        private int failedCount = 0;
        /** 跳过的数量（未找到源VIP） */
        private int skipCount = 0;
        /** 错误信息列表 */
        private List<String> errors = new ArrayList<>();
        /** 补开详情列表 */
        private List<String> details = new ArrayList<>();
        /** 开始时间 */
        private String startTime;
        /** 结束时间 */
        private String endTime;

        public void addError(String error) {
            errors.add(error);
        }

        public void addDetail(String detail) {
            details.add(detail);
        }

        public String getSummary() {
            return String.format(
                    "VIP补开完成 - 处理车牌: %d个, 成功: %d个, 失败: %d个, 跳过: %d个",
                    totalPlateCount, successCount, failedCount, skipCount);
        }
    }

    /**
     * 为指定车牌列表补开VIP月票
     * 
     * 流程:
     * 1. 查询车牌的VIP记录
     * 2. 找到"停用tcjc"类型的VIP
     * 3. 使用相同的有效期开通"请停车检查(化工西化肥西复合肥南)"VIP
     *
     * @param plateNumbers 车牌号列表
     * @return 补开结果
     */
    public SupplementResult supplementVipForPlates(List<String> plateNumbers) {
        log.info("========== 开始为车牌列表补开VIP月票，车牌数量: {} ==========", 
                plateNumbers != null ? plateNumbers.size() : 0);
        
        SupplementResult result = new SupplementResult();
        result.setStartTime(getCurrentTime());
        result.setTotalPlateCount(plateNumbers != null ? plateNumbers.size() : 0);

        if (plateNumbers == null || plateNumbers.isEmpty()) {
            result.addError("车牌号码列表为空");
            result.setEndTime(getCurrentTime());
            return result;
        }

        for (String plateNumber : plateNumbers) {
            try {
                log.info("处理车牌: {}", plateNumber);

                // 查询该车牌的VIP记录
                List<AkeVipService.VipTicketInfo> vips = akeVipService.getVipTicket(plateNumber, null, null);
                log.info("车牌 {} 查询到 {} 条VIP记录", plateNumber, vips != null ? vips.size() : 0);

                if (vips == null || vips.isEmpty()) {
                    result.addError("车牌 " + plateNumber + " 未查询到VIP记录");
                    result.setSkipCount(result.getSkipCount() + 1);
                    continue;
                }

                // 查找"停用tcjc"类型的VIP
                AkeVipService.VipTicketInfo sourceVip = findSourceVip(vips);
                
                if (sourceVip == null) {
                    String error = String.format("车牌 %s 未找到'%s'类型的VIP", plateNumber, SOURCE_VIP_TYPE);
                    result.addError(error);
                    result.setSkipCount(result.getSkipCount() + 1);
                    log.warn(error);
                    continue;
                }

                // 补开VIP
                boolean success = supplementSingleVip(plateNumber, sourceVip, result);
                if (success) {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } else {
                    result.setFailedCount(result.getFailedCount() + 1);
                }

            } catch (Exception e) {
                result.addError("处理车牌 " + plateNumber + " 异常: " + e.getMessage());
                result.setFailedCount(result.getFailedCount() + 1);
                log.error("处理车牌 {} 异常", plateNumber, e);
            }
        }

        result.setEndTime(getCurrentTime());
        log.info("========== 车牌列表VIP补开完成 ==========");
        log.info(result.getSummary());

        return result;
    }

    /**
     * 从VIP列表中查找"停用tcjc"类型的VIP
     * 
     * @param vips VIP列表
     * @return 找到的VIP，如果没找到返回null
     */
    private AkeVipService.VipTicketInfo findSourceVip(List<AkeVipService.VipTicketInfo> vips) {
        for (AkeVipService.VipTicketInfo vip : vips) {
            if (SOURCE_VIP_TYPE.equals(vip.getVipTypeName())) {
                log.info("找到源VIP: 车牌={}, 类型={}, 有效期={} ~ {}", 
                        vip.getCarNo(), vip.getVipTypeName(), vip.getStartTime(), vip.getEndTime());
                return vip;
            }
        }
        return null;
    }

    /**
     * 为单个车牌补开VIP
     * 
     * @param plateNumber 车牌号
     * @param sourceVip 源VIP信息（停用tcjc）
     * @param result 补开结果
     * @return 是否成功
     */
    private boolean supplementSingleVip(String plateNumber, AkeVipService.VipTicketInfo sourceVip, 
                                       SupplementResult result) {
        log.info("开始补开VIP: 车牌={}, 源类型={}, 目标类型={}, 有效期={} ~ {}",
                plateNumber, SOURCE_VIP_TYPE, TARGET_VIP_TYPE, 
                sourceVip.getStartTime(), sourceVip.getEndTime());

        try {
            // 构建开通VIP请求
            OpenVipTicketRequest request = buildOpenVipTicketRequest(sourceVip);
            
            // 调用开通VIP接口
            boolean openSuccess = akeVipService.openVipTicket(request);

            if (openSuccess) {
                String detail = String.format("补开成功: 车牌=%s, VIP类型=%s, 有效期=%s ~ %s",
                        plateNumber, TARGET_VIP_TYPE, sourceVip.getStartTime(), sourceVip.getEndTime());
                result.addDetail(detail);
                log.info(detail);
                return true;
            } else {
                String error = String.format("补开VIP失败: 车牌=%s, VIP类型=%s", plateNumber, TARGET_VIP_TYPE);
                result.addError(error);
                log.warn(error);
                return false;
            }

        } catch (Exception e) {
            String error = String.format("补开VIP异常: 车牌=%s, 错误=%s", plateNumber, e.getMessage());
            result.addError(error);
            log.error("补开VIP异常: 车牌={}", plateNumber, e);
            return false;
        }
    }

    /**
     * 根据源VIP信息构建开通VIP票请求
     * 
     * @param sourceVip 源VIP信息
     * @return 开通VIP票请求
     */
    private OpenVipTicketRequest buildOpenVipTicketRequest(AkeVipService.VipTicketInfo sourceVip) {
        OpenVipTicketRequest request = new OpenVipTicketRequest();

        // 基本信息
        request.setVipTypeName(TARGET_VIP_TYPE);
        request.setTicketNo(generateTicketNo(sourceVip.getCarNo()));
        
        // 车主信息：使用源VIP的车主信息
        String carOwner = sourceVip.getCarOwner();
        if (carOwner == null || carOwner.trim().isEmpty() || "无".equals(carOwner)) {
            carOwner = sourceVip.getCarNo(); // 如果没有车主姓名，使用车牌号
        }
        request.setCarOwner(carOwner);
        
        // 电话：使用源VIP的电话，如果没有则生成
        String telphone = sourceVip.getTelphone();
        if (telphone == null || telphone.trim().isEmpty()) {
            telphone = "13" + String.valueOf(System.currentTimeMillis()).substring(4);
        }
        request.setTelphone(telphone);
        
        request.setCompany("");
        request.setDepartment("");
        request.setSex("0");
        request.setOperator(defaultOperator);
        request.setOperateTime(getCurrentTime());
        request.setOriginalPrice("0");
        request.setDiscountPrice("0");
        request.setOpenValue("1");
        request.setOpenCarCount("1");

        // 车辆列表
        List<OpenVipTicketRequest.CarInfo> carList = new ArrayList<>();
        OpenVipTicketRequest.CarInfo carInfo = new OpenVipTicketRequest.CarInfo();
        carInfo.setCarNo(sourceVip.getCarNo());
        carList.add(carInfo);
        request.setCarList(carList);

        // 时间段列表（使用源VIP的有效期）
        List<OpenVipTicketRequest.TimePeriod> timePeriodList = new ArrayList<>();
        OpenVipTicketRequest.TimePeriod timePeriod = new OpenVipTicketRequest.TimePeriod();
        
        // 使用源VIP的开始时间和结束时间
        String startTime = sourceVip.getStartTime();
        String endTime = sourceVip.getEndTime();
        
        if (startTime != null && !startTime.isEmpty()) {
            timePeriod.setStartTime(startTime);
        } else {
            timePeriod.setStartTime(LocalDateTime.now().format(FORMATTER));
        }
        
        if (endTime != null && !endTime.isEmpty()) {
            timePeriod.setEndTime(endTime);
        } else {
            // 默认一年有效期
            timePeriod.setEndTime(LocalDateTime.now().plusYears(1).format(FORMATTER));
        }
        
        timePeriodList.add(timePeriod);
        request.setTimePeriodList(timePeriodList);

        return request;
    }

    /**
     * 生成票号
     */
    private String generateTicketNo(String plateNumber) {
        return plateNumber + "_SUPPLEMENT_" + System.currentTimeMillis();
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
