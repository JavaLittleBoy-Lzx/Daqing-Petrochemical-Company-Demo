package com.parkingmanage.service.sync;

import com.parkingmanage.dto.ake.AddBlacklistCarRequest;
import com.parkingmanage.dto.ake.OpenVipTicketRequest;
import com.parkingmanage.service.ake.AkeVipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * VIP迁移服务
 * 负责将现有VIP迁移到新的类型结构
 */
@Slf4j
@Service
public class VipMigrationService {

    @Autowired
    private AkeVipService akeVipService;

    @Value("${ake.default-operator:系统同步}")
    private String defaultOperator;

    /**
     * 迁移结果
     */
    @lombok.Data
    public static class MigrationResult {
        /** 查询到的VIP总数 */
        private int totalVipCount = 0;
        /** 生效中的VIP数量 */
        private int activeVipCount = 0;
        /** 查询到的黑名单总数 */
        private int totalBlacklistCount = 0;
        /** 成功删除的黑名单数量 */
        private int blacklistDeletedCount = 0;
        /** 删除黑名单失败数量 */
        private int blacklistDeleteFailedCount = 0;
        /** 成功退费的VIP数量 */
        private int vipRefundCount = 0;
        /** 退费失败的VIP数量 */
        private int vipRefundFailedCount = 0;
        /** 成功迁移到VIP的数量 */
        private int vipMigratedCount = 0;
        /** VIP迁移失败数量 */
        private int vipMigrateFailedCount = 0;
        /** 成功迁移到黑名单的数量 */
        private int blacklistMigratedCount = 0;
        /** 黑名单迁移失败数量 */
        private int blacklistMigrateFailedCount = 0;
        /** 错误信息列表 */
        private List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public String getSummary() {
            return String.format(
                    "VIP迁移完成 - 查询VIP: %d条, 生效中: %d条, 黑名单: %d条, " +
                    "删除黑名单: %d成功/%d失败, 退费VIP: %d成功/%d失败, " +
                    "迁移VIP: %d成功/%d失败, 迁移黑名单: %d成功/%d失败",
                    totalVipCount, activeVipCount, totalBlacklistCount,
                    blacklistDeletedCount, blacklistDeleteFailedCount,
                    vipRefundCount, vipRefundFailedCount,
                    vipMigratedCount, vipMigrateFailedCount,
                    blacklistMigratedCount, blacklistMigrateFailedCount);
        }
    }

    /**
     * 执行VIP迁移
     * 步骤：
     * 1. 查询指定车牌号的VIP月票，筛选"生效中"的数据
     * 2. 查询所有黑名单并删除
     * 3. 对VIP进行退费
     * 4. 根据VIP类型判断迁移目标：
     *    - 请停车检查格式 -> 迁移到"请停车检查（化工西化肥西复合肥）"VIP
     *    - 其他格式 -> 迁移到对应的黑名单
     *
     * @param plateNumbers 车牌号列表
     * @return 迁移结果
     */
    public MigrationResult migrateVipToNewType(List<String> plateNumbers) {
        log.info("========== 开始VIP迁移，车牌数量: {} ==========", plateNumbers != null ? plateNumbers.size() : 0);
        MigrationResult result = new MigrationResult();

        try {
            // 步骤1：查询指定车牌号的VIP月票
            // 按车牌分组，对每个车牌直接使用查询到的VIP记录
            Map<String, List<AkeVipService.VipTicketInfo>> vipMap = new LinkedHashMap<>();
            if (plateNumbers != null && !plateNumbers.isEmpty()) {
                for (String plateNumber : plateNumbers) {
                    List<AkeVipService.VipTicketInfo> vips = akeVipService.getVipTicket(plateNumber, null, null);
                    if (vips != null && !vips.isEmpty()) {
                        vipMap.put(plateNumber, vips);
                    }
                }
            }

            // 对每个车牌的VIP记录进行筛选
            List<AkeVipService.VipTicketInfo> activeVips = new ArrayList<>();
            for (Map.Entry<String, List<AkeVipService.VipTicketInfo>> entry : vipMap.entrySet()) {
                String plateNumber = entry.getKey();
                List<AkeVipService.VipTicketInfo> vips = entry.getValue();
                result.setTotalVipCount(result.getTotalVipCount() + vips.size());

                // 筛选"已退款"状态的VIP，选择time_period范围最大的
                AkeVipService.VipTicketInfo selectedVip = selectBestRefundedVip(vips);
                if (selectedVip != null) {
                    activeVips.add(selectedVip);
                    log.info("选择已退款VIP: 车牌={}, 类型={}, 开始时间={}, 结束时间={}",
                            selectedVip.getCarNo(), selectedVip.getVipTypeName(),
                            selectedVip.getStartTime(), selectedVip.getEndTime());
                } else {
                    log.info("车牌 {} 无已退款的VIP记录", plateNumber);
                }
            }
            result.setActiveVipCount(activeVips.size());
            log.info("查询到VIP总数: {}, 已退款数量: {}", result.getTotalVipCount(), result.getActiveVipCount());

            // 步骤2：查询所有黑名单并删除
            // 使用分页查询接口 GET_BLACK_LIST 获取所有黑名单
            List<AkeVipService.BlacklistInfo> allBlacklists = akeVipService.getAllBlacklistsByPage();
            result.setTotalBlacklistCount(allBlacklists != null ? allBlacklists.size() : 0);
            log.info("查询到黑名单总数: {}", result.getTotalBlacklistCount());

            if (allBlacklists != null && !allBlacklists.isEmpty()) {
                for (AkeVipService.BlacklistInfo blacklist : allBlacklists) {
                    boolean deleteSuccess = akeVipService.deleteBlacklistCar(
                            blacklist.getCarLicenseNumber(), null, null);
                    if (deleteSuccess) {
                        result.setBlacklistDeletedCount(result.getBlacklistDeletedCount() + 1);
                        log.info("删除黑名单成功: 车牌={}, 类型={}",
                                blacklist.getCarLicenseNumber(), blacklist.getVipName());
                    } else {
                        result.setBlacklistDeleteFailedCount(result.getBlacklistDeleteFailedCount() + 1);
                        result.addError("删除黑名单失败: 车牌=" + blacklist.getCarLicenseNumber());
                        log.warn("删除黑名单失败: 车牌={}", blacklist.getCarLicenseNumber());
                    }
                }
            }

            // 步骤3：对VIP进行退费
            if (activeVips != null && !activeVips.isEmpty()) {
                for (AkeVipService.VipTicketInfo vip : activeVips) {
                    boolean refundSuccess = akeVipService.refundVipTicket(
                            vip.getVipTicketSeq(), null, null, "0");
                    if (refundSuccess) {
                        result.setVipRefundCount(result.getVipRefundCount() + 1);
                        log.info("VIP退费成功: 车牌={}, 类型={}",
                                vip.getCarNo(), vip.getVipTypeName());
                    } else {
                        result.setVipRefundFailedCount(result.getVipRefundFailedCount() + 1);
                        result.addError("VIP退费失败: 车牌=" + vip.getCarNo() + ", 类型=" + vip.getVipTypeName());
                        log.warn("VIP退费失败: 车牌={}", vip.getCarNo());
                        // 退费失败则跳过该VIP
                        continue;
                    }

                    // 步骤4：根据VIP类型判断迁移目标
                    String vipTypeName = vip.getVipTypeName();
                    if (isPleaseStopCheckType(vipTypeName)) {
                        // 请停车检查格式 -> 迁移到"请停车检查（化工西化肥西复合肥）"VIP
                        migrateToVip(vip, result);
                    } else {
                        // 其他格式 -> 迁移到对应的黑名单
                        migrateToBlacklist(vip, result);
                    }
                }
            }

            log.info("========== VIP迁移完成 ==========");
            log.info(result.getSummary());

        } catch (Exception e) {
            log.error("VIP迁移异常", e);
            result.addError("VIP迁移异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 判断是否为"请停车检查"类型的VIP
     *
     * @param vipTypeName VIP类型名称
     * @return true表示是请停车检查类型
     */
    private boolean isPleaseStopCheckType(String vipTypeName) {
        if (vipTypeName == null) {
            return false;
        }
        return vipTypeName.contains("请停车检查");
    }

    /**
     * 将VIP类型名称转换为黑名单类型名称
     * 由于黑名单类型与VIP类型使用相同的表，需要避免名称冲突
     * 规则：将"化工西VIP"改为"化工西门VIP"，其他保持不变
     *
     * @param vipTypeName VIP类型名称
     * @return 黑名单类型名称
     */
    private String convertVipTypeToBlacklistType(String vipTypeName) {
        if (vipTypeName == null) {
            return null;
        }
        // 将"化工西"替换为"化工西门"避免与VIP类型名称冲突
        return vipTypeName.replace("化工西", "化工西门");
    }

    /**
     * 从VIP列表中选择最佳的已退款VIP
     * 规则：
     * 1. 如果只有一个VIP，直接返回
     * 2. 如果有多个VIP，筛选出状态是"已退款"的
     * 3. 如果有多个已退款的，选择time_period范围最大的（结束时间最晚的）
     *
     * @param vips VIP列表
     * @return 最佳已退款VIP，如果没有则返回null
     */
    private AkeVipService.VipTicketInfo selectBestRefundedVip(List<AkeVipService.VipTicketInfo> vips) {
        if (vips == null || vips.isEmpty()) {
            return null;
        }

        // 如果只有一个VIP，直接返回
        if (vips.size() == 1) {
            return vips.get(0);
        }

        // 多个VIP，筛选出"已退款"状态的
        List<AkeVipService.VipTicketInfo> refundedVips = new ArrayList<>();

        for (AkeVipService.VipTicketInfo vip : vips) {
            if (isVipRefunded(vip)) {
                refundedVips.add(vip);
            }
        }

        // 如果没有已退款的VIP，返回null
        if (refundedVips.isEmpty()) {
            return null;
        }

        // 如果只有一个已退款的VIP，直接返回
        if (refundedVips.size() == 1) {
            return refundedVips.get(0);
        }

        // 多个已退款的VIP，选择范围最大的（结束时间最晚的）
        AkeVipService.VipTicketInfo bestVip = refundedVips.get(0);
        for (int i = 1; i < refundedVips.size(); i++) {
            AkeVipService.VipTicketInfo current = refundedVips.get(i);
            if (compareVipTimeRange(current, bestVip) > 0) {
                bestVip = current;
            }
        }

        return bestVip;
    }

    /**
     * 判断VIP是否已退款
     *
     * @param vip VIP信息
     * @return true表示已退款，false表示未退款
     */
    private boolean isVipRefunded(AkeVipService.VipTicketInfo vip) {
        String status = vip.getTicketStatus();
        // 判断状态是否为"已退款"
        return "已退款".equals(status) || "2".equals(status) || "退费".equals(status);
    }

    /**
     * 比较两个VIP的时间范围大小
     * 返回值：正数表示vip1范围更大，负数表示vip2范围更大，0表示相同
     *
     * @param vip1 VIP1
     * @param vip2 VIP2
     * @return 比较结果
     */
    private int compareVipTimeRange(AkeVipService.VipTicketInfo vip1, AkeVipService.VipTicketInfo vip2) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            LocalDateTime end1 = null;
            LocalDateTime end2 = null;
            LocalDateTime start1 = null;
            LocalDateTime start2 = null;

            if (vip1.getEndTime() != null && !vip1.getEndTime().isEmpty()) {
                end1 = LocalDateTime.parse(vip1.getEndTime(), formatter);
            }
            if (vip2.getEndTime() != null && !vip2.getEndTime().isEmpty()) {
                end2 = LocalDateTime.parse(vip2.getEndTime(), formatter);
            }
            if (vip1.getStartTime() != null && !vip1.getStartTime().isEmpty()) {
                start1 = LocalDateTime.parse(vip1.getStartTime(), formatter);
            }
            if (vip2.getStartTime() != null && !vip2.getStartTime().isEmpty()) {
                start2 = LocalDateTime.parse(vip2.getStartTime(), formatter);
            }

            // 首先比较结束时间，结束时间越晚范围越大
            if (end1 != null && end2 != null) {
                int endCompare = end1.compareTo(end2);
                if (endCompare != 0) {
                    return endCompare;
                }
            } else if (end1 != null) {
                return 1;  // vip1有结束时间，vip2没有，vip1范围更大
            } else if (end2 != null) {
                return -1;  // vip2有结束时间，vip1没有，vip2范围更大
            }

            // 结束时间相同，比较开始时间，开始时间越早范围越大
            if (start1 != null && start2 != null) {
                return start2.compareTo(start1);  // 反向比较，开始时间越早越大
            } else if (start1 != null) {
                return -1;  // vip1有开始时间，vip2没有，vip2范围更大
            } else if (start2 != null) {
                return 1;   // vip2有开始时间，vip1没有，vip1范围更大
            }

            return 0;

        } catch (Exception e) {
            log.warn("比较VIP时间范围失败: vip1={}, vip2={}",
                    vip1.getCarNo(), vip2.getCarNo());
            return 0;
        }
    }

    /**
     * 迁移到"请停车检查（化工西化肥西复合肥南）"VIP
     *
     * @param originalVip 原始VIP信息
     * @param result 迁移结果
     */
    private void migrateToVip(AkeVipService.VipTicketInfo originalVip, MigrationResult result) {
        String plateNumber = originalVip.getCarNo();
        log.info("迁移VIP到新类型: 车牌={}, 原类型={}", plateNumber, originalVip.getVipTypeName());

        try {
            // 构建开通VIP请求
            OpenVipTicketRequest request = new OpenVipTicketRequest();

            // VIP类型：请停车检查（化工西化肥西复合肥南）
            request.setVipTypeName("请停车检查（化工西化肥西复合肥南）");
            request.setTicketNo(generateTicketNo(plateNumber));
            request.setCarOwner(originalVip.getCarOwner());
            request.setTelphone(""); // VipTicketInfo中没有此字段，使用空字符串
            request.setCompany(""); // VipTicketInfo中没有此字段，使用空字符串
            request.setDepartment(""); // VipTicketInfo中没有此字段，使用空字符串
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
            carInfo.setCarNo(plateNumber);
            carList.add(carInfo);
            request.setCarList(carList);

            // 时间段列表（使用原VIP的时间）
            List<OpenVipTicketRequest.TimePeriod> timePeriodList = new ArrayList<>();
            OpenVipTicketRequest.TimePeriod timePeriod = new OpenVipTicketRequest.TimePeriod();

            String startTime = originalVip.getStartTime();
            String endTime = originalVip.getEndTime();

            if (startTime != null && !startTime.isEmpty()) {
                timePeriod.setStartTime(startTime);
            } else {
                timePeriod.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            if (endTime != null && !endTime.isEmpty()) {
                timePeriod.setEndTime(endTime);
            } else {
                timePeriod.setEndTime(LocalDateTime.now().plusYears(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            timePeriodList.add(timePeriod);
            request.setTimePeriodList(timePeriodList);

            // 调用开通接口
            boolean success = akeVipService.openVipTicket(request);

            if (success) {
                result.setVipMigratedCount(result.getVipMigratedCount() + 1);
                log.info("VIP迁移成功: 车牌={}, 新类型={}", plateNumber, request.getVipTypeName());
            } else {
                result.setVipMigrateFailedCount(result.getVipMigrateFailedCount() + 1);
                result.addError("VIP迁移失败: 车牌=" + plateNumber);
                log.warn("VIP迁移失败: 车牌={}", plateNumber);
            }

        } catch (Exception e) {
            result.setVipMigrateFailedCount(result.getVipMigrateFailedCount() + 1);
            result.addError("VIP迁移异常: 车牌=" + plateNumber + ", 错误=" + e.getMessage());
            log.error("VIP迁移异常: 车牌={}", plateNumber, e);
        }
    }

    /**
     * 迁移到对应的黑名单
     *
     * @param originalVip 原始VIP信息
     * @param result 迁移结果
     */
    private void migrateToBlacklist(AkeVipService.VipTicketInfo originalVip, MigrationResult result) {
        String plateNumber = originalVip.getCarNo();
        String originalVipTypeName = originalVip.getVipTypeName();
        log.info("迁移VIP到黑名单: 车牌={}, 原VIP类型={}", plateNumber, originalVipTypeName);

        try {
            // 构建黑名单请求
            AddBlacklistCarRequest request = new AddBlacklistCarRequest();

            // 黑名单类型名称：需要将"化工西VIP"转换为"化工西门VIP"避免与VIP类型名称冲突
            String blacklistTypeName = convertVipTypeToBlacklistType(originalVipTypeName);
            request.setVipTypeName(blacklistTypeName);
            request.setCarCode(plateNumber);
            request.setCarOwner(originalVip.getCarOwner());
            request.setReason("请停车检查"); // VipTicketInfo中没有telphone字段，使用默认值

            // 判断是否为永久黑名单（根据原VIP结束时间判断）
            String endTime = originalVip.getEndTime();
            if (endTime == null || endTime.isEmpty() || endTime.contains("2099") || endTime.contains("9999")) {
                // 永久黑名单
                request.setIsPermament(1);
            } else {
                // 临时黑名单
                request.setIsPermament(0);
                AddBlacklistCarRequest.TimePeriod timePeriod = new AddBlacklistCarRequest.TimePeriod();

                String startTime = originalVip.getStartTime();
                if (startTime != null && !startTime.isEmpty()) {
                    timePeriod.setStartTime(startTime);
                } else {
                    timePeriod.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                timePeriod.setEndTime(endTime);
                request.setTimePeriod(timePeriod);
            }

            request.setRemark1("");
            request.setRemark2("VIP迁移");
            request.setOperator(defaultOperator);
            request.setOperateTime(getCurrentTime());

            // 调用添加黑名单接口
            boolean success = akeVipService.addBlacklistCar(request);

            if (success) {
                result.setBlacklistMigratedCount(result.getBlacklistMigratedCount() + 1);
                log.info("黑名单迁移成功: 车牌={}, 黑名单类型={}", plateNumber, request.getVipTypeName());
            } else {
                result.setBlacklistMigrateFailedCount(result.getBlacklistMigrateFailedCount() + 1);
                result.addError("黑名单迁移失败: 车牌=" + plateNumber);
                log.warn("黑名单迁移失败: 车牌={}", plateNumber);
            }

        } catch (Exception e) {
            result.setBlacklistMigrateFailedCount(result.getBlacklistMigrateFailedCount() + 1);
            result.addError("黑名单迁移异常: 车牌=" + plateNumber + ", 错误=" + e.getMessage());
            log.error("黑名单迁移异常: 车牌={}", plateNumber, e);
        }
    }

    /**
     * 生成票号
     */
    private String generateTicketNo(String plateNumber) {
        return plateNumber + "_MIGRATE_" + System.currentTimeMillis();
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
