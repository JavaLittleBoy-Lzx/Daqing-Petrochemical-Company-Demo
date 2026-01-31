package com.parkingmanage.service.sync;

import com.parkingmanage.dto.VehicleSyncResult;
import com.parkingmanage.entity.GroupedVehicleInfo;
import com.parkingmanage.entity.OracleVehicleInfo;
import com.parkingmanage.service.ake.AkeVipService;
import com.parkingmanage.service.sync.impl.DataSyncServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 车辆同步测试服务
 * 用于测试VIP月票和黑名单同步的各种场景
 */
@Slf4j
@Service
public class VehicleBlacklistTestService {

    @Autowired
    private DataSyncServiceImpl dataSyncService;

    @Autowired
    private AkeVipService akeVipService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== VIP月票测试方法 ====================

    /**
     * 测试场景1：添加新VIP月票（无现有VIP）
     * 
     * @param plateNumber 车牌号
     * @param gateNames 门名称列表，逗号分隔
     * @param ownerName 车主姓名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 测试结果
     */
    public String testAddNewVip(String plateNumber, String gateNames, String ownerName, 
                               String startTime, String endTime) {
        log.info("=== 测试场景1：添加新VIP月票 ===");
        log.info("车牌: {}, 门: {}, 车主: {}, 时间: {} ~ {}", 
                plateNumber, gateNames, ownerName, startTime, endTime);

        try {
            // 1. 先查询是否已有VIP
            List<AkeVipService.VipTicketInfo> existingVips = akeVipService.getVipTicket(plateNumber, null, null);
            List<AkeVipService.VipTicketInfo> activeVips = new ArrayList<>();
            if (existingVips != null) {
                for (AkeVipService.VipTicketInfo vip : existingVips) {
                    if ("生效中".equals(vip.getTicketStatus())) {
                        activeVips.add(vip);
                    }
                }
            }
            
            if (!activeVips.isEmpty()) {
                return String.format("车辆[%s]已存在生效中的VIP票，请先退费或使用更新接口。当前VIP类型: %s", 
                        plateNumber, activeVips.get(0).getVipTypeName());
            }

            // 2. 构建测试数据
            GroupedVehicleInfo groupedVehicle = buildGroupedVehicleInfo(
                    plateNumber, gateNames, ownerName, startTime, endTime, false);
            groupedVehicle.setNeedCheck(false); // 标记为VIP（不需要检查）

            // 3. 调用VIP同步流程
            VehicleSyncResult result = new VehicleSyncResult();
            boolean success = invokeProcessVipSync(groupedVehicle, result);

            // 4. 返回结果
            if (success) {
                return String.format("✅ 开通VIP月票成功！\n车牌: %s\n门权限: %s\n时间: %s ~ %s\n费用: 0元", 
                        plateNumber, 
                        String.join(", ", groupedVehicle.getOrgNames()),
                        startTime,
                        endTime);
            } else {
                return String.format("❌ 开通VIP月票失败！\n失败原因: %s", 
                        result.getFailedRecords().isEmpty() ? "未知" : 
                        result.getFailedRecords().get(0).getReason());
            }
        } catch (Exception e) {
            log.error("测试开通VIP月票异常", e);
            return "❌ 测试异常: " + e.getMessage();
        }
    }

    /**
     * 测试场景2：VIP权限变化（先退费再开通）
     * 
     * @param plateNumber 车牌号
     * @param newGateNames 新的门名称列表
     * @param ownerName 车主姓名
     * @return 测试结果
     */
    public String testUpdateVipPermission(String plateNumber, String newGateNames, String ownerName) {
        log.info("=== 测试场景2：VIP权限变化 ===");
        log.info("车牌: {}, 新门: {}", plateNumber, newGateNames);

        try {
            // 1. 查询现有VIP
            List<AkeVipService.VipTicketInfo> existingVips = akeVipService.getVipTicket(plateNumber, null, null);
            List<AkeVipService.VipTicketInfo> activeVips = new ArrayList<>();
            if (existingVips != null) {
                for (AkeVipService.VipTicketInfo vip : existingVips) {
                    if ("生效中".equals(vip.getTicketStatus())) {
                        activeVips.add(vip);
                    }
                }
            }
            
            if (activeVips.isEmpty()) {
                return String.format("车辆[%s]不存在生效中的VIP票，请先开通", plateNumber);
            }

            AkeVipService.VipTicketInfo existingVip = activeVips.get(0);
            String oldPermission = existingVip.getVipTypeName();
            String startTime = existingVip.getStartTime();
            String endTime = existingVip.getEndTime();

            // 2. 构建新的测试数据（使用现有的时间）
            GroupedVehicleInfo groupedVehicle = buildGroupedVehicleInfo(
                    plateNumber, newGateNames, ownerName, startTime, endTime, false);
            groupedVehicle.setNeedCheck(false); // 标记为VIP

            // 3. 调用VIP同步流程
            VehicleSyncResult result = new VehicleSyncResult();
            boolean success = invokeProcessVipSync(groupedVehicle, result);

            // 4. 返回结果
            if (success) {
                return String.format("✅ VIP权限变化处理成功！\n车牌: %s\n旧权限: %s\n新权限: %s\n操作: 先退费，再开通新VIP（0元）", 
                        plateNumber, 
                        oldPermission,
                        String.join(", ", groupedVehicle.getOrgNames()));
            } else {
                return String.format("❌ VIP权限变化处理失败！\n失败原因: %s", 
                        result.getFailedRecords().isEmpty() ? "未知" : 
                        result.getFailedRecords().get(0).getReason());
            }
        } catch (Exception e) {
            log.error("测试VIP权限变化异常", e);
            return "❌ 测试异常: " + e.getMessage();
        }
    }

    /**
     * 测试场景3：VIP时间更新（续费）
     * 
     * @param plateNumber 车牌号
     * @param newStartTime 新的开始时间
     * @param newEndTime 新的结束时间
     * @param ownerName 车主姓名
     * @return 测试结果
     */
    public String testUpdateVipTime(String plateNumber, String newStartTime, 
                                   String newEndTime, String ownerName) {
        log.info("=== 测试场景3：VIP时间更新 ===");
        log.info("车牌: {}, 新时间: {} ~ {}", plateNumber, newStartTime, newEndTime);

        try {
            // 1. 查询现有VIP
            List<AkeVipService.VipTicketInfo> existingVips = akeVipService.getVipTicket(plateNumber, null, null);
            List<AkeVipService.VipTicketInfo> activeVips = new ArrayList<>();
            if (existingVips != null) {
                for (AkeVipService.VipTicketInfo vip : existingVips) {
                    if ("生效中".equals(vip.getTicketStatus())) {
                        activeVips.add(vip);
                    }
                }
            }
            
            if (activeVips.isEmpty()) {
                return String.format("车辆[%s]不存在生效中的VIP票，请先开通", plateNumber);
            }

            AkeVipService.VipTicketInfo existingVip = activeVips.get(0);
            String oldTime = existingVip.getStartTime() + " ~ " + existingVip.getEndTime();
            String oldPermission = existingVip.getVipTypeName();

            // 2. 直接调用续费接口
            log.info("调用续费接口，票序列号: {}, 新时间: {} ~ {}", 
                    existingVip.getVipTicketSeq(), newStartTime, newEndTime);
            
            boolean success = akeVipService.renewVipTicket(
                    existingVip.getVipTicketSeq(),
                    newStartTime,
                    newEndTime,
                    null,
                    null);

            // 3. 返回结果
            if (success) {
                return String.format("✅ VIP时间更新成功！\n车牌: %s\n权限: %s\n旧时间: %s\n新时间: %s ~ %s\n操作: 调用续费接口", 
                        plateNumber, 
                        oldPermission,
                        oldTime,
                        newStartTime,
                        newEndTime);
            } else {
                return String.format("❌ VIP时间更新失败！\n车牌: %s\n请查看日志了解详细原因", plateNumber);
            }
        } catch (Exception e) {
            log.error("测试VIP时间更新异常", e);
            return "❌ 测试异常: " + e.getMessage();
        }
    }

    /**
     * 测试场景4：注销状态（退费VIP）
     * 
     * @param plateNumber 车牌号
     * @return 测试结果
     */
    public String testRefundVip(String plateNumber) {
        log.info("=== 测试场景4：退费VIP（注销状态） ===");
        log.info("车牌: {}", plateNumber);

        try {
            // 1. 查询现有VIP
            List<AkeVipService.VipTicketInfo> existingVips = akeVipService.getVipTicket(plateNumber, null, null);
            List<AkeVipService.VipTicketInfo> activeVips = new ArrayList<>();
            if (existingVips != null) {
                for (AkeVipService.VipTicketInfo vip : existingVips) {
                    if ("生效中".equals(vip.getTicketStatus())) {
                        activeVips.add(vip);
                    }
                }
            }
            
            if (activeVips.isEmpty()) {
                return String.format("车辆[%s]不存在生效中的VIP票，无需退费", plateNumber);
            }

            String oldPermission = activeVips.get(0).getVipTypeName();

            // 2. 构建注销状态的测试数据
            GroupedVehicleInfo groupedVehicle = buildGroupedVehicleInfo(
                    plateNumber, "化工西门", "测试车主", null, null, true);
            groupedVehicle.setNeedCheck(false); // 标记为VIP

            // 3. 调用VIP同步流程
            VehicleSyncResult result = new VehicleSyncResult();
            boolean success = invokeProcessVipSync(groupedVehicle, result);

            // 4. 返回结果
            if (success) {
                return String.format("✅ 退费VIP成功！\n车牌: %s\n旧权限: %s\n操作: 检测到注销状态(DQZT=D)，退费VIP（0元）", 
                        plateNumber, 
                        oldPermission);
            } else {
                return String.format("❌ 退费VIP失败！\n失败原因: %s", 
                        result.getFailedRecords().isEmpty() ? "未知" : 
                        result.getFailedRecords().get(0).getReason());
            }
        } catch (Exception e) {
            log.error("测试退费VIP异常", e);
            return "❌ 测试异常: " + e.getMessage();
        }
    }

    /**
     * 查询车辆VIP状态
     * 
     * @param plateNumber 车牌号
     * @return 查询结果
     */
    public String queryVipStatus(String plateNumber) {
        log.info("查询车辆VIP状态 - 车牌: {}", plateNumber);

        try {
            List<AkeVipService.VipTicketInfo> vips = akeVipService.getVipTicket(plateNumber, null, null);
            
            if (vips == null || vips.isEmpty()) {
                return String.format("车辆[%s]无VIP票记录", plateNumber);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📋 VIP票信息\n");
            sb.append("共 ").append(vips.size()).append(" 条记录\n\n");
            
            for (int i = 0; i < vips.size(); i++) {
                AkeVipService.VipTicketInfo vip = vips.get(i);
                sb.append("【记录 ").append(i + 1).append("】\n");
                sb.append("票号: ").append(vip.getTicketNo()).append("\n");
                sb.append("VIP类型: ").append(vip.getVipTypeName()).append("\n");
                sb.append("车牌号: ").append(vip.getCarNo()).append("\n");
                sb.append("车主: ").append(vip.getCarOwner()).append("\n");
                sb.append("状态: ").append(vip.getTicketStatus()).append("\n");
                sb.append("有效期: ").append(vip.getStartTime()).append(" ~ ").append(vip.getEndTime()).append("\n");
                if (i < vips.size() - 1) {
                    sb.append("\n");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("查询VIP状态异常", e);
            return "❌ 查询异常: " + e.getMessage();
        }
    }

    /**
     * 从VIP类型名称中提取门名称
     * 格式："化工西化肥西VIP" -> "化工西门,化肥西门"
     */
    private String extractGateNamesFromVipType(String vipType) {
        if (vipType == null || !vipType.endsWith("VIP")) {
            return "";
        }

        String gateNamesStr = vipType.substring(0, vipType.length() - 3);
        
        // 简单处理：假设门名称都是已知的
        return gateNamesStr.replace("化工西", "化工西门,")
                          .replace("化肥西", "化肥西门,")
                          .replace("复合肥南", "复合肥南门,")
                          .replaceAll(",$", "");
    }

    /**
     * 通过反射调用私有方法 processVipSync
     */
    private boolean invokeProcessVipSync(GroupedVehicleInfo groupedVehicle, 
                                        VehicleSyncResult result) throws Exception {
        Method method = DataSyncServiceImpl.class.getDeclaredMethod(
                "processVipSync", GroupedVehicleInfo.class, VehicleSyncResult.class);
        method.setAccessible(true);
        return (boolean) method.invoke(dataSyncService, groupedVehicle, result);
    }

    // ==================== 黑名单测试方法 ====================

    /**
     * 测试场景1：添加新黑名单（无现有黑名单）
     * 
     * @param plateNumber 车牌号
     * @param gateNames 门名称列表，逗号分隔（如：化工西门,化肥西门）
     * @param ownerName 车主姓名
     * @param startTime 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（为空则为永久黑名单）
     * @return 测试结果
     */
    public String testAddNewBlacklist(String plateNumber, String gateNames, String ownerName, 
                                     String startTime, String endTime) {
        log.info("=== 测试场景1：添加新黑名单 ===");
        log.info("车牌: {}, 门: {}, 车主: {}, 时间: {} ~ {}", 
                plateNumber, gateNames, ownerName, startTime, endTime);

        try {
            // 1. 先查询是否已有黑名单
            AkeVipService.BlacklistInfo existing = akeVipService.getBlacklistByPlateNumber(plateNumber);
            if (existing != null) {
                return String.format("车辆[%s]已存在黑名单，请先删除或使用更新接口。当前黑名单类型: %s", 
                        plateNumber, existing.getVipName());
            }

            // 2. 构建测试数据
            GroupedVehicleInfo groupedVehicle = buildGroupedVehicleInfo(
                    plateNumber, gateNames, ownerName, startTime, endTime, false);

            // 3. 调用黑名单同步流程
            VehicleSyncResult result = new VehicleSyncResult();
            boolean success = invokeProcessBlacklistSync(groupedVehicle, result);

            // 4. 返回结果
            if (success) {
                return String.format("✅ 添加黑名单成功！\n车牌: %s\n门权限: %s\n时间: %s ~ %s\n类型: %s", 
                        plateNumber, 
                        String.join(", ", groupedVehicle.getOrgNames()),
                        startTime != null ? startTime : "永久",
                        endTime != null ? endTime : "永久",
                        endTime != null ? "临时黑名单" : "永久黑名单");
            } else {
                return String.format("❌ 添加黑名单失败！\n失败原因: %s", 
                        result.getFailedRecords().isEmpty() ? "未知" : 
                        result.getFailedRecords().get(0).getReason());
            }
        } catch (Exception e) {
            log.error("测试添加黑名单异常", e);
            return "❌ 测试异常: " + e.getMessage();
        }
    }

    /**
     * 测试场景2：权限变化（先删除再添加）
     * 
     * @param plateNumber 车牌号
     * @param newGateNames 新的门名称列表
     * @param ownerName 车主姓名
     * @return 测试结果
     */
    public String testUpdateBlacklistPermission(String plateNumber, String newGateNames, String ownerName) {
        log.info("=== 测试场景2：黑名单权限变化 ===");
        log.info("车牌: {}, 新门: {}", plateNumber, newGateNames);

        try {
            // 1. 查询现有黑名单
            AkeVipService.BlacklistInfo existing = akeVipService.getBlacklistByPlateNumber(plateNumber);
            if (existing == null) {
                return String.format("车辆[%s]不存在黑名单，请先添加", plateNumber);
            }

            String oldPermission = existing.getVipName();
            String timeInfo = existing.getTimeperiodList();

            // 2. 构建新的测试数据（使用现有的时间）
            String startTime = null;
            String endTime = null;
            if (timeInfo != null && !timeInfo.isEmpty()) {
                String[] parts = timeInfo.split("~");
                if (parts.length == 2) {
                    startTime = parts[0].trim();
                    endTime = parts[1].trim();
                }
            }

            GroupedVehicleInfo groupedVehicle = buildGroupedVehicleInfo(
                    plateNumber, newGateNames, ownerName, startTime, endTime, false);

            // 3. 调用黑名单同步流程
            VehicleSyncResult result = new VehicleSyncResult();
            boolean success = invokeProcessBlacklistSync(groupedVehicle, result);

            // 4. 返回结果
            if (success) {
                return String.format("✅ 权限变化处理成功！\n车牌: %s\n旧权限: %s\n新权限: %s\n操作: 先删除旧黑名单，再添加新黑名单", 
                        plateNumber, 
                        oldPermission,
                        String.join(", ", groupedVehicle.getOrgNames()));
            } else {
                return String.format("❌ 权限变化处理失败！\n失败原因: %s", 
                        result.getFailedRecords().isEmpty() ? "未知" : 
                        result.getFailedRecords().get(0).getReason());
            }
        } catch (Exception e) {
            log.error("测试权限变化异常", e);
            return "❌ 测试异常: " + e.getMessage();
        }
    }

    /**
     * 测试场景3：时间更新（临时黑名单）
     * 
     * @param plateNumber 车牌号
     * @param newStartTime 新的开始时间
     * @param newEndTime 新的结束时间
     * @param ownerName 车主姓名
     * @return 测试结果
     */
    public String testUpdateBlacklistTime(String plateNumber, String newStartTime, 
                                         String newEndTime, String ownerName) {
        log.info("=== 测试场景3：黑名单时间更新 ===");
        log.info("车牌: {}, 新时间: {} ~ {}", plateNumber, newStartTime, newEndTime);

        try {
            // 1. 查询现有黑名单
            AkeVipService.BlacklistInfo existing = akeVipService.getBlacklistByPlateNumber(plateNumber);
            if (existing == null) {
                return String.format("车辆[%s]不存在黑名单，请先添加", plateNumber);
            }

            if ("1".equals(existing.getBlacklistForeverFlag())) {
                return String.format("车辆[%s]是永久黑名单，无法更新时间", plateNumber);
            }

            String oldTime = existing.getTimeperiodList();
            String oldPermission = existing.getVipName();

            // 2. 从旧的黑名单类型名称中提取门名称
            // 格式："请停车检查（化工西化肥西）" -> "化工西,化肥西"
            String gateNames = extractGateNamesFromBlacklistType(oldPermission);

            // 3. 构建新的测试数据（保持权限不变，只改时间）
            GroupedVehicleInfo groupedVehicle = buildGroupedVehicleInfo(
                    plateNumber, gateNames, ownerName, newStartTime, newEndTime, false);

            // 4. 调用黑名单同步流程
            VehicleSyncResult result = new VehicleSyncResult();
            boolean success = invokeProcessBlacklistSync(groupedVehicle, result);

            // 5. 返回结果
            if (success) {
                return String.format("✅ 时间更新成功！\n车牌: %s\n权限: %s\n旧时间: %s\n新时间: %s ~ %s\n操作: 先删除旧黑名单，再添加新黑名单", 
                        plateNumber, 
                        oldPermission,
                        oldTime,
                        newStartTime,
                        newEndTime);
            } else {
                return String.format("❌ 时间更新失败！\n失败原因: %s", 
                        result.getFailedRecords().isEmpty() ? "未知" : 
                        result.getFailedRecords().get(0).getReason());
            }
        } catch (Exception e) {
            log.error("测试时间更新异常", e);
            return "❌ 测试异常: " + e.getMessage();
        }
    }

    /**
     * 测试场景4：注销状态（删除黑名单）
     * 
     * @param plateNumber 车牌号
     * @return 测试结果
     */
    public String testDeleteBlacklist(String plateNumber) {
        log.info("=== 测试场景4：删除黑名单（注销状态） ===");
        log.info("车牌: {}", plateNumber);

        try {
            // 1. 查询现有黑名单
            AkeVipService.BlacklistInfo existing = akeVipService.getBlacklistByPlateNumber(plateNumber);
            if (existing == null) {
                return String.format("车辆[%s]不存在黑名单，无需删除", plateNumber);
            }

            String oldPermission = existing.getVipName();

            // 2. 构建注销状态的测试数据
            GroupedVehicleInfo groupedVehicle = buildGroupedVehicleInfo(
                    plateNumber, "化工西门", "测试车主", null, null, true);

            // 3. 调用黑名单同步流程
            VehicleSyncResult result = new VehicleSyncResult();
            boolean success = invokeProcessBlacklistSync(groupedVehicle, result);

            // 4. 返回结果
            if (success) {
                return String.format("✅ 删除黑名单成功！\n车牌: %s\n旧权限: %s\n操作: 检测到注销状态(DQZT=D)，删除黑名单", 
                        plateNumber, 
                        oldPermission);
            } else {
                return String.format("❌ 删除黑名单失败！\n失败原因: %s", 
                        result.getFailedRecords().isEmpty() ? "未知" : 
                        result.getFailedRecords().get(0).getReason());
            }
        } catch (Exception e) {
            log.error("测试删除黑名单异常", e);
            return "❌ 测试异常: " + e.getMessage();
        }
    }

    /**
     * 查询车辆黑名单状态
     * 
     * @param plateNumber 车牌号
     * @return 查询结果
     */
    public String queryBlacklistStatus(String plateNumber) {
        log.info("查询车辆黑名单状态 - 车牌: {}", plateNumber);

        try {
            AkeVipService.BlacklistInfo blacklist = akeVipService.getBlacklistByPlateNumber(plateNumber);
            
            if (blacklist == null) {
                return String.format("车辆[%s]不在黑名单中", plateNumber);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📋 黑名单信息\n");
            sb.append("车牌号: ").append(blacklist.getCarLicenseNumber()).append("\n");
            sb.append("类型名称: ").append(blacklist.getVipName()).append("\n");
            sb.append("车主: ").append(blacklist.getOwner()).append("\n");
            sb.append("原因: ").append(blacklist.getReason()).append("\n");
            sb.append("永久标志: ").append("1".equals(blacklist.getBlacklistForeverFlag()) ? "永久" : "临时").append("\n");
            sb.append("时间段: ").append(blacklist.getTimeperiodList() != null ? blacklist.getTimeperiodList() : "无").append("\n");
            sb.append("添加人: ").append(blacklist.getAddBy()).append("\n");
            sb.append("添加时间: ").append(blacklist.getAddTime()).append("\n");
            sb.append("操作人: ").append(blacklist.getOperateBy()).append("\n");
            sb.append("操作时间: ").append(blacklist.getOperateTime());

            return sb.toString();
        } catch (Exception e) {
            log.error("查询黑名单状态异常", e);
            return "❌ 查询异常: " + e.getMessage();
        }
    }

    /**
     * 构建分组车辆信息
     */
    private GroupedVehicleInfo buildGroupedVehicleInfo(String plateNumber, String gateNames, 
                                                      String ownerName, String startTime, 
                                                      String endTime, boolean isDeleted) {
        GroupedVehicleInfo groupedVehicle = new GroupedVehicleInfo();
        groupedVehicle.setPlateNumber(plateNumber);
        groupedVehicle.setOwnerName(ownerName);
        groupedVehicle.setOwnerPhone("13800138000");
        groupedVehicle.setCompany("测试单位");
        groupedVehicle.setNeedCheck(true); // 标记为需要检查（黑名单）
        groupedVehicle.setCheckReason("请停车检查");
        
        // 设置注销状态
        if (isDeleted) {
            groupedVehicle.setRemark("D"); // 注销状态
        } else {
            groupedVehicle.setRemark(""); // 正常状态
        }

        // 解析门名称列表
        List<String> orgNames = new ArrayList<>();
        if (gateNames != null && !gateNames.isEmpty()) {
            String[] names = gateNames.split(",");
            for (String name : names) {
                orgNames.add(name.trim());
            }
        }
        groupedVehicle.setOrgNames(orgNames);

        // 设置时间
        if (startTime != null && !startTime.isEmpty()) {
            groupedVehicle.setValidStartTime(LocalDateTime.parse(startTime, FORMATTER));
        }
        if (endTime != null && !endTime.isEmpty()) {
            groupedVehicle.setValidEndTime(LocalDateTime.parse(endTime, FORMATTER));
        }

        // 添加一条原始记录（用于满足分组要求）
        OracleVehicleInfo record = new OracleVehicleInfo();
        record.setPlateNumber(plateNumber);
        record.setOwnerName(ownerName);
        record.setNeedCheck(true);
        groupedVehicle.addRecord(record);

        return groupedVehicle;
    }

    /**
     * 从黑名单类型名称中提取门名称
     * 格式："请停车检查（化工西化肥西）" -> "化工西门,化肥西门"
     */
    private String extractGateNamesFromBlacklistType(String blacklistType) {
        if (blacklistType == null || !blacklistType.contains("（")) {
            return "";
        }

        int start = blacklistType.indexOf("（");
        int end = blacklistType.indexOf("）");
        if (start == -1 || end == -1 || end <= start) {
            return "";
        }

        String gateNamesStr = blacklistType.substring(start + 1, end);
        
        // 简单处理：假设门名称都是已知的
        // 实际应该使用VipPermissionUtil来解析
        return gateNamesStr.replace("化工西", "化工西门,")
                          .replace("化肥西", "化肥西门,")
                          .replace("复合肥南", "复合肥南门,")
                          .replaceAll(",$", "");
    }

    /**
     * 通过反射调用私有方法 processBlacklistSync
     */
    private boolean invokeProcessBlacklistSync(GroupedVehicleInfo groupedVehicle, 
                                              VehicleSyncResult result) throws Exception {
        Method method = DataSyncServiceImpl.class.getDeclaredMethod(
                "processBlacklistSync", GroupedVehicleInfo.class, VehicleSyncResult.class);
        method.setAccessible(true);
        return (boolean) method.invoke(dataSyncService, groupedVehicle, result);
    }
}
