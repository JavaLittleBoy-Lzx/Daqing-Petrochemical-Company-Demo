package com.parkingmanage.service.sync;

import com.parkingmanage.dto.ake.OpenVipTicketRequest;
import com.parkingmanage.service.ake.AkeVipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * VIP时间修复服务
 * 负责将time_period结束时间为9999开头的VIP月票修复为2099开头
 */
@Slf4j
@Service
public class VipTimeFixService {

    @Autowired
    private AkeVipService akeVipService;

    @Value("${ake.default-operator:系统同步}")
    private String defaultOperator;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 时间修复结果
     */
    @lombok.Data
    public static class TimeFixResult {
        /** 处理的车牌总数 */
        private int totalPlateCount = 0;
        /** 查询到的VIP总数 */
        private int totalVipCount = 0;
        /** 需要修复的VIP数量（9999开头的） */
        private int needFixCount = 0;
        /** 成功修复的VIP数量 */
        private int successCount = 0;
        /** 修复失败的VIP数量 */
        private int failedCount = 0;
        /** 无需修复的VIP数量（非9999开头或非生效中） */
        private int skipCount = 0;
        /** 错误信息列表 */
        private List<String> errors = new ArrayList<>();
        /** 修复详情列表 */
        private List<String> details = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addDetail(String detail) {
            details.add(detail);
        }

        public String getSummary() {
            return String.format(
                    "VIP时间修复完成 - 处理车牌: %d个, 查询VIP: %d条, 需要修复: %d条, 成功: %d条, 失败: %d条, 跳过: %d条",
                    totalPlateCount, totalVipCount, needFixCount, successCount, failedCount, skipCount);
        }
    }

    /**
     * 根据车牌号码列表修复VIP时间
     * 直接对传入的车牌号码进行退费并重新开通（无需筛选）
     *
     * @param plateNumbers 车牌号码列表
     * @return 修复结果
     */
    public TimeFixResult fixVipTimeByPlateNumbers(List<String> plateNumbers) {
        log.info("========== 开始根据车牌列表修复VIP时间，车牌数量: {} ==========", plateNumbers != null ? plateNumbers.size() : 0);
        TimeFixResult result = new TimeFixResult();
        result.setTotalPlateCount(plateNumbers != null ? plateNumbers.size() : 0);

        if (plateNumbers == null || plateNumbers.isEmpty()) {
            result.addError("车牌号码列表为空");
            return result;
        }

        for (String plateNumber : plateNumbers) {
            try {
                log.info("处理车牌: {}", plateNumber);

                // 查询该车牌的VIP
                List<AkeVipService.VipTicketInfo> vips = akeVipService.getVipTicket(plateNumber, null, null);
                result.setTotalVipCount(result.getTotalVipCount() + (vips != null ? vips.size() : 0));
                log.info("车牌 {} 查询到 {} 条VIP记录", plateNumber, vips != null ? vips.size() : 0);

                if (vips == null || vips.isEmpty()) {
                    result.addError("车牌 " + plateNumber + " 未查询到VIP记录");
                    continue;
                }

                // 处理该车牌的所有VIP
                for (AkeVipService.VipTicketInfo vip : vips) {
                    // 只处理"生效中"的VIP
                    if (!"生效中".equals(vip.getTicketStatus()) && !"1".equals(vip.getTicketStatus())) {
                        result.setSkipCount(result.getSkipCount() + 1);
                        log.debug("跳过非生效中VIP: 车牌={}, 状态={}", plateNumber, vip.getTicketStatus());
                        continue;
                    }

                    // 检查结束时间是否以9999开头
                    String endTime = vip.getEndTime();
                    if (endTime == null || !endTime.trim().startsWith("9999")) {
                        result.setSkipCount(result.getSkipCount() + 1);
                        log.debug("跳过非9999开头时间的VIP: 车牌={}, 结束时间={}", plateNumber, endTime);
                        continue;
                    }

                    // 需要修复
                    result.setNeedFixCount(result.getNeedFixCount() + 1);
                    boolean success = fixSingleVip(vip, result);
                    if (success) {
                        result.setSuccessCount(result.getSuccessCount() + 1);
                    } else {
                        result.setFailedCount(result.getFailedCount() + 1);
                    }
                }

            } catch (Exception e) {
                result.addError("处理车牌 " + plateNumber + " 异常: " + e.getMessage());
                log.error("处理车牌 {} 异常", plateNumber, e);
            }
        }

        log.info("========== 车牌列表VIP时间修复完成 ==========");
        log.info(result.getSummary());

        return result;
    }

    /**
     * 修复单个VIP的时间
     * 步骤：
     * 1. 退费现有VIP
     * 2. 重新开通VIP，将结束时间从9999改为2099
     *
     * @param originalVip 原始VIP信息
     * @param result 修复结果
     * @return 是否成功
     */
    private boolean fixSingleVip(AkeVipService.VipTicketInfo originalVip, TimeFixResult result) {
        String plateNumber = originalVip.getCarNo();
        String vipTypeName = originalVip.getVipTypeName();
        String originalEndTime = originalVip.getEndTime();
        String newEndTime = convert9999To2099(originalEndTime);

        log.info("开始修复VIP: 车牌={}, 类型={}, 原结束时间={}, 新结束时间={}",
                plateNumber, vipTypeName, originalEndTime, newEndTime);

        try {
            // 步骤1：退费现有VIP
            boolean refundSuccess = akeVipService.refundVipTicket(
                    originalVip.getVipTicketSeq(), null, null, "0");

            if (!refundSuccess) {
                String error = String.format("退费VIP失败: 车牌=%s, 类型=%s", plateNumber, vipTypeName);
                result.addError(error);
                log.warn(error);
                return false;
            }

            log.info("退费VIP成功: 车牌={}, 票序列号={}", plateNumber, originalVip.getVipTicketSeq());

            // 步骤2：重新开通VIP，使用新的结束时间
            OpenVipTicketRequest request = buildOpenVipTicketRequest(originalVip, newEndTime);
            boolean openSuccess = akeVipService.openVipTicket(request);

            if (openSuccess) {
                String detail = String.format("修复成功: 车牌=%s, 类型=%s, 原时间=%s, 新时间=%s",
                        plateNumber, vipTypeName, originalEndTime, newEndTime);
                result.addDetail(detail);
                log.info(detail);
                return true;
            } else {
                String error = String.format("开通VIP失败: 车牌=%s, 类型=%s", plateNumber, vipTypeName);
                result.addError(error);
                log.warn(error);
                return false;
            }

        } catch (Exception e) {
            String error = String.format("修复VIP异常: 车牌=%s, 错误=%s", plateNumber, e.getMessage());
            result.addError(error);
            log.error("修复VIP异常: 车牌={}", plateNumber, e);
            return false;
        }
    }

    /**
     * 将9999开头的年份转换为2099
     * 保持月日时分秒不变
     *
     * @param originalTime 原始时间（格式：yyyy-MM-dd HH:mm:ss）
     * @return 转换后的时间
     */
    private String convert9999To2099(String originalTime) {
        if (originalTime == null || originalTime.trim().isEmpty()) {
            return null;
        }

        String time = originalTime.trim();
        if (time.startsWith("9999")) {
            // 将9999替换为2099，保持后面的月日时分秒不变
            return "2099" + time.substring(4);
        }

        return time;
    }

    /**
     * 根据原始VIP信息构建开通VIP票请求
     *
     * @param originalVip 原始VIP信息
     * @param newEndTime 新的结束时间
     * @return 开通VIP票请求
     */
    private OpenVipTicketRequest buildOpenVipTicketRequest(AkeVipService.VipTicketInfo originalVip, String newEndTime) {
        OpenVipTicketRequest request = new OpenVipTicketRequest();

        // 基本信息 - 使用原VIP的信息
        request.setVipTypeName(originalVip.getVipTypeName());
        request.setTicketNo(generateTicketNo(originalVip.getCarNo()));
        request.setCarOwner(originalVip.getCarOwner() != null ? originalVip.getCarOwner() : "系统同步");
        request.setTelphone("");
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
        carInfo.setCarNo(originalVip.getCarNo());
        carList.add(carInfo);
        request.setCarList(carList);

        // 时间段列表（使用原VIP的开始时间和新的结束时间）
        List<OpenVipTicketRequest.TimePeriod> timePeriodList = new ArrayList<>();
        OpenVipTicketRequest.TimePeriod timePeriod = new OpenVipTicketRequest.TimePeriod();

        String startTime = originalVip.getStartTime();
        if (startTime != null && !startTime.isEmpty()) {
            timePeriod.setStartTime(startTime);
        } else {
            timePeriod.setStartTime(LocalDateTime.now().format(FORMATTER));
        }

        timePeriod.setEndTime(newEndTime);

        timePeriodList.add(timePeriod);
        request.setTimePeriodList(timePeriodList);

        return request;
    }

    /**
     * 获取所有VIP票（分页查询）
     *
     * @return 所有VIP票列表
     */
    private List<AkeVipService.VipTicketInfo> getAllVipTickets() {
        log.info("开始分页查询所有VIP票");

        List<AkeVipService.VipTicketInfo> allVips = new ArrayList<>();
        int pageSize = 100;
        int pageNumber = 1;
        boolean hasMore = true;

        try {
            while (hasMore) {
                List<AkeVipService.VipTicketInfo> pageData = getVipTicketPage(pageNumber, pageSize);

                if (pageData == null || pageData.isEmpty()) {
                    hasMore = false;
                } else {
                    allVips.addAll(pageData);
                    if (pageData.size() < pageSize) {
                        hasMore = false;
                    } else {
                        pageNumber++;
                    }
                }
            }

            log.info("分页查询VIP票完成，共获取 {} 条记录", allVips.size());
            return allVips;

        } catch (Exception e) {
            log.error("分页查询VIP票失败", e);
            return allVips;
        }
    }

    /**
     * 分页查询VIP票
     *
     * @param pageNumber 页码
     * @param pageSize 每页大小
     * @return VIP票列表
     */
    private List<AkeVipService.VipTicketInfo> getVipTicketPage(int pageNumber, int pageSize) {
        try {
            // 使用AkeVipService的getVipTicket方法，传入空参数查询所有
            // 这里需要直接调用AKE接口进行分页查询
            List<AkeVipService.VipTicketInfo> allVips = akeVipService.getVipTicket(null, null, null);

            // 简单分页处理
            int fromIndex = (pageNumber - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, allVips.size());

            if (fromIndex >= allVips.size()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(allVips.subList(fromIndex, toIndex));

        } catch (Exception e) {
            log.error("分页查询VIP票失败: page={}, size={}", pageNumber, pageSize, e);
            return new ArrayList<>();
        }
    }

    /**
     * 生成票号
     */
    private String generateTicketNo(String plateNumber) {
        return plateNumber + "_FIX_" + System.currentTimeMillis();
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
