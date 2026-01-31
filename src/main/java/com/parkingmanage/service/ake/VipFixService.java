package com.parkingmanage.service.ake;

import com.parkingmanage.dto.ake.OpenVipTicketRequest;
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
 * VIP票数据修复服务
 * 用于修复VIP票中缺失的车主信息（姓名、手机号）
 *
 * 处理流程：
 * 1. 查询所有VIP票
 * 2. 筛选出需要修复的记录
 * 3. 先退费，再补全信息重新开通
 */
@Slf4j
@Service
public class VipFixService {

    @Autowired
    private AkeVipService akeVipService;

    @Value("${ake.default-operator:系统同步}")
    private String defaultOperator;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成唯一的手机号
     * 格式：13 + 时间戳后9位
     *
     * @return 唯一的手机号
     */
    private String generateUniquePhone() {
        return "13" + String.valueOf(System.currentTimeMillis()).substring(4);
    }

    /**
     * 检查车主姓名是否无效（为null、空或"无"）
     *
     * @param owner 车主姓名
     * @return true表示无效
     */
    private boolean isInvalidOwner(String owner) {
        if (owner == null || owner.trim().isEmpty()) {
            return true;
        }
        // 检查是否为"无"或包含"无"
        String trimmed = owner.trim();
        return "无".equals(trimmed) || "无名".equals(trimmed);
    }

    /**
     * 修复没有手机号的VIP票（智能补全车主姓名）
     *
     * 流程：
     * 1. 查询所有VIP票
     * 2. 筛选出telphone为空的记录
     * 3. 生成唯一手机号
     * 4. 智能判断是否需要补全车主姓名：
     *    - 如果车主姓名为空或"无"，则使用车牌号作为车主姓名
     *    - 如果车主姓名有值，则保持不变
     * 5. 先退费，再用补全的信息重新开通
     *
     * @return 修复结果
     */
    public FixResult fixVipsWithoutPhone() {
        log.info("========== 开始修复没有手机号的VIP票（智能补全车主姓名） ==========");
        FixResult result = new FixResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // 1. 查询所有VIP票（分页获取）
            List<AkeVipService.VipTicketInfo> allVips = getAllVipTickets();

            if (allVips.isEmpty()) {
                log.info("未查询到任何VIP票");
                result.setSuccess(true);
                result.setTotal(0);
                return result;
            }

            log.info("查询到 {} 条VIP票记录", allVips.size());

            // 2. 筛选出手机号为空的VIP票（不限状态）
            List<AkeVipService.VipTicketInfo> needFixVips = new ArrayList<>();
            int withOwnerCount = 0;  // 有车主姓名的数量
            int withoutOwnerCount = 0;  // 没有车主姓名的数量
            
            for (AkeVipService.VipTicketInfo vip : allVips) {
                // 只检查手机号是否为空
                boolean hasNoPhone = vip.getTelphone() == null || vip.getTelphone().trim().isEmpty();
                if (hasNoPhone) {
                    boolean hasInvalidOwner = isInvalidOwner(vip.getCarOwner());
                    if (hasInvalidOwner) {
                        withoutOwnerCount++;
                        log.info("发现需要修复的VIP票（无车主姓名）: 车牌={}, 车主={}, 状态={}",
                                vip.getCarNo(), vip.getCarOwner(), vip.getTicketStatus());
                    } else {
                        withOwnerCount++;
                        log.info("发现需要修复的VIP票（有车主姓名）: 车牌={}, 车主={}, 状态={}",
                                vip.getCarNo(), vip.getCarOwner(), vip.getTicketStatus());
                    }
                    needFixVips.add(vip);
                }
            }

            result.setTotal(needFixVips.size());
            log.info("筛选出 {} 条需要修复的VIP票（有车主姓名: {}, 无车主姓名: {}）", 
                    needFixVips.size(), withOwnerCount, withoutOwnerCount);

            if (needFixVips.isEmpty()) {
                result.setSuccess(true);
                return result;
            }

            // 3. 逐个处理需要修复的VIP票
            for (AkeVipService.VipTicketInfo vip : needFixVips) {
                try {
                    boolean success = fixVipWithoutPhone(vip, result);
                    if (success) {
                        result.setSuccessCount(result.getSuccessCount() + 1);
                    } else {
                        result.setFailedCount(result.getFailedCount() + 1);
                    }
                } catch (Exception e) {
                    log.error("修复VIP票异常: 车牌={}", vip.getCarNo(), e);
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.addFailedRecord(vip.getCarNo(), "修复异常: " + e.getMessage());
                }
            }

            result.setSuccess(true);
            log.info("========== VIP票修复完成 ==========");
            return result;

        } catch (Exception e) {
            log.error("修复VIP票失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
    }

    /**
     * 根据指定车牌号列表修复VIP票（智能补全手机号和车主姓名）
     *
     * @param plateNumbersStr 车牌号字符串，支持多种分隔符
     * @return 修复结果
     */
    public FixResult fixVipsByPlateNumbers(String plateNumbersStr) {
        log.info("========== 开始根据车牌号列表修复VIP票 ==========");
        FixResult result = new FixResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // 1. 解析车牌号字符串
            List<String> plateNumbers = parsePlateNumbers(plateNumbersStr);
            
            if (plateNumbers.isEmpty()) {
                log.warn("解析车牌号列表为空");
                result.setSuccess(true);
                result.setTotal(0);
                return result;
            }

            log.info("解析到 {} 个车牌号", plateNumbers.size());

            // 2. 逐个车牌号查询并修复
            int withOwnerCount = 0;
            int withoutOwnerCount = 0;
            
            for (String plateNumber : plateNumbers) {
                try {
                    // 根据车牌号查询VIP票
                    List<AkeVipService.VipTicketInfo> vipTickets = akeVipService.getVipTicket(plateNumber, null, null);
                    
                    if (vipTickets == null || vipTickets.isEmpty()) {
                        log.warn("未找到车牌号对应的VIP票: {}", plateNumber);
                        result.addFailedRecord(plateNumber, "未找到对应的VIP票");
                        result.setFailedCount(result.getFailedCount() + 1);
                        continue;
                    }
                    
                    // 处理该车牌的所有VIP票
                    for (AkeVipService.VipTicketInfo vip : vipTickets) {
                        result.setTotal(result.getTotal() + 1);
                        
                        boolean hasInvalidOwner = isInvalidOwner(vip.getCarOwner());
                        if (hasInvalidOwner) {
                            withoutOwnerCount++;
                            log.info("找到需要修复的VIP票（无车主姓名）: 车牌={}, 车主={}, 状态={}",
                                    vip.getCarNo(), vip.getCarOwner(), vip.getTicketStatus());
                        } else {
                            withOwnerCount++;
                            log.info("找到需要修复的VIP票（有车主姓名）: 车牌={}, 车主={}, 状态={}",
                                    vip.getCarNo(), vip.getCarOwner(), vip.getTicketStatus());
                        }
                        
                        // 修复VIP票
                        boolean success = fixVipWithoutPhone(vip, result);
                        if (success) {
                            result.setSuccessCount(result.getSuccessCount() + 1);
                        } else {
                            result.setFailedCount(result.getFailedCount() + 1);
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("处理车牌号异常: {}", plateNumber, e);
                    result.addFailedRecord(plateNumber, "处理异常: " + e.getMessage());
                    result.setFailedCount(result.getFailedCount() + 1);
                }
            }

            log.info("筛选出 {} 条需要修复的VIP票（有车主姓名: {}, 无车主姓名: {}）", 
                    result.getTotal(), withOwnerCount, withoutOwnerCount);

            result.setSuccess(true);
            log.info("========== VIP票修复完成 ==========");
            return result;

        } catch (Exception e) {
            log.error("根据车牌号列表修复VIP票失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
    }

    /**
     * 解析车牌号字符串
     * 支持多种分隔符：逗号、分号、换行符、空格
     *
     * @param plateNumbersStr 车牌号字符串
     * @return 车牌号列表
     */
    private List<String> parsePlateNumbers(String plateNumbersStr) {
        List<String> plateNumbers = new ArrayList<>();
        
        if (plateNumbersStr == null || plateNumbersStr.trim().isEmpty()) {
            return plateNumbers;
        }

        // 支持多种分隔符：逗号、分号、换行符、空格
        String[] parts = plateNumbersStr.split("[,;\\s\\n\\r]+");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                plateNumbers.add(trimmed);
            }
        }

        log.info("解析车牌号: 原始字符串长度={}, 解析出{}个车牌号", 
                plateNumbersStr.length(), plateNumbers.size());
        
        return plateNumbers;
    }

    /**
     * 修复既没有手机号也没有车主姓名的VIP票
     *
     * 流程：
     * 1. 查询所有VIP票
     * 2. 筛选出telphone和car_owner都为空/无的记录
     * 3. 生成唯一手机号，车主姓名使用车牌号
     * 4. 先退费，再用补全的信息重新开通
     *
     * @return 修复结果
     */
    public FixResult fixVipsWithoutPhoneAndOwner() {
        log.info("========== 开始修复既没有手机号也没有车主姓名的VIP票 ==========");
        FixResult result = new FixResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // 1. 查询所有VIP票（分页获取）
            List<AkeVipService.VipTicketInfo> allVips = getAllVipTickets();

            if (allVips.isEmpty()) {
                log.info("未查询到任何VIP票");
                result.setSuccess(true);
                result.setTotal(0);
                return result;
            }

            log.info("查询到 {} 条VIP票记录", allVips.size());

            // 2. 筛选出既没有手机号也没有车主姓名的VIP票（不限状态）
            List<AkeVipService.VipTicketInfo> needFixVips = new ArrayList<>();
            for (AkeVipService.VipTicketInfo vip : allVips) {
                // 检查是否既没有手机号也没有车主姓名
                boolean hasNoPhone = vip.getTelphone() == null || vip.getTelphone().trim().isEmpty();
                boolean hasNoOwner = isInvalidOwner(vip.getCarOwner());
                if (hasNoPhone && hasNoOwner) {
                    log.info("发现需要修复的VIP票: 车牌={}, 车主={}, 状态={}, 手机号={}",
                            vip.getCarNo(), vip.getCarOwner(), vip.getTicketStatus(), "(空)");
                    needFixVips.add(vip);
                }
            }

            result.setTotal(needFixVips.size());
            log.info("筛选出 {} 条需要修复的VIP票", needFixVips.size());

            if (needFixVips.isEmpty()) {
                result.setSuccess(true);
                return result;
            }

            // 3. 逐个处理需要修复的VIP票
            for (AkeVipService.VipTicketInfo vip : needFixVips) {
                try {
                    boolean success = fixVipWithoutPhoneAndOwner(vip, result);
                    if (success) {
                        result.setSuccessCount(result.getSuccessCount() + 1);
                    } else {
                        result.setFailedCount(result.getFailedCount() + 1);
                    }
                } catch (Exception e) {
                    log.error("修复VIP票异常: 车牌={}", vip.getCarNo(), e);
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.addFailedRecord(vip.getCarNo(), "修复异常: " + e.getMessage());
                }
            }

            result.setSuccess(true);
            log.info("========== VIP票修复完成 ==========");
            return result;

        } catch (Exception e) {
            log.error("修复VIP票失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
    }

    /**
     * 修复单个没有手机号的VIP票
     *
     * @param vip VIP票信息
     * @param result 修复结果
     * @return 是否成功
     */
    private boolean fixVipWithoutPhone(AkeVipService.VipTicketInfo vip, FixResult result) {
        String plateNumber = vip.getCarNo();
        log.info("修复VIP票[车牌={}]: 补全手机号和车主姓名", plateNumber);

        // 1. 先退费
        boolean refundSuccess = akeVipService.refundVipTicket(
                vip.getVipTicketSeq(), null, null, "0");

        if (!refundSuccess) {
            log.warn("VIP票退费失败: 车牌={}", plateNumber);
            result.addFailedRecord(plateNumber, "退费失败");
            return false;
        }

        log.info("VIP票退费成功: 车牌={}", plateNumber);

        // 2. 生成唯一手机号
        String newPhone = generateUniquePhone();
        // 3. 确定车主姓名（如果为"无"或空，则使用车牌号）
        String newOwner = isInvalidOwner(vip.getCarOwner()) ? plateNumber : vip.getCarOwner();
        log.info("生成新信息: 车牌={}, 车主={}, 手机号={}", plateNumber, newOwner, newPhone);

        // 4. 重新开通（补全手机号和车主姓名）
        return reopenVipWithFixedInfo(vip, newPhone, newOwner);
    }

    /**
     * 修复单个既没有手机号也没有车主姓名的VIP票
     *
     * @param vip VIP票信息
     * @param result 修复结果
     * @return 是否成功
     */
    private boolean fixVipWithoutPhoneAndOwner(AkeVipService.VipTicketInfo vip, FixResult result) {
        String plateNumber = vip.getCarNo();
        log.info("修复VIP票[车牌={}]: 补全手机号和车主姓名", plateNumber);

        // 1. 先退费
        boolean refundSuccess = akeVipService.refundVipTicket(
                vip.getVipTicketSeq(), null, null, "0");

        if (!refundSuccess) {
            log.warn("VIP票退费失败: 车牌={}", plateNumber);
            result.addFailedRecord(plateNumber, "退费失败");
            return false;
        }

        log.info("VIP票退费成功: 车牌={}", plateNumber);

        // 2. 生成唯一手机号和车主姓名
        String newPhone = generateUniquePhone();
        String newOwner = plateNumber; // 车主姓名使用车牌号
        log.info("生成新信息: 车牌={}, 车主={}, 手机号={}", plateNumber, newOwner, newPhone);

        // 3. 重新开通（补全手机号和车主姓名）
        return reopenVipWithFixedInfo(vip, newPhone, newOwner);
    }

    /**
     * 使用修复后的信息重新开通VIP票
     * 只修改手机号和车主姓名，其他信息保持原样
     *
     * @param originalVip 原始VIP票信息
     * @param newPhone 新手机号
     * @param newOwner 新车主姓名
     * @return 是否成功
     */
    private boolean reopenVipWithFixedInfo(AkeVipService.VipTicketInfo originalVip,
                                           String newPhone, String newOwner) {
        try {
            // 构建开通请求 - 保留所有原始信息
            OpenVipTicketRequest request = new OpenVipTicketRequest();
            
            // 保留原VIP类型
            request.setVipTypeName(originalVip.getVipTypeName());
            
            // 生成新的票号（必须唯一）
            request.setTicketNo(originalVip.getCarNo() + "_" + System.currentTimeMillis());
            
            // ===== 只修改这两个字段 =====
            request.setCarOwner(newOwner);      // 修复后的车主姓名
            request.setTelphone(newPhone);      // 修复后的手机号
            // ===========================
            
            // 其他字段使用默认值（AKE系统不存储这些信息）
            request.setCompany("");
            request.setDepartment("");
            request.setSex("0");
            request.setOperator(defaultOperator);
            request.setOperateTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            request.setOriginalPrice("0");
            request.setDiscountPrice("0");
            request.setOpenValue("1");
            request.setOpenCarCount("1");

            // 保留原车牌号
            List<OpenVipTicketRequest.CarInfo> carList = new ArrayList<>();
            OpenVipTicketRequest.CarInfo carInfo = new OpenVipTicketRequest.CarInfo();
            carInfo.setCarNo(originalVip.getCarNo());
            carList.add(carInfo);
            request.setCarList(carList);

            // ===== 保留原有效期（重要！）=====
            List<OpenVipTicketRequest.TimePeriod> timePeriodList = new ArrayList<>();
            OpenVipTicketRequest.TimePeriod timePeriod = new OpenVipTicketRequest.TimePeriod();
            timePeriod.setStartTime(originalVip.getStartTime());
            timePeriod.setEndTime(originalVip.getEndTime());
            timePeriodList.add(timePeriod);
            request.setTimePeriodList(timePeriodList);
            // ================================

            // 调用开通接口
            boolean success = akeVipService.openVipTicket(request);

            if (success) {
                log.info("VIP票重新开通成功: 车牌={}, 车主={}, 手机号={}, 有效期={} ~ {}",
                        originalVip.getCarNo(), newOwner, newPhone, 
                        originalVip.getStartTime(), originalVip.getEndTime());
            } else {
                log.warn("VIP票重新开通失败: 车牌={}", originalVip.getCarNo());
            }

            return success;

        } catch (Exception e) {
            log.error("重新开通VIP票异常: 车牌={}", originalVip.getCarNo(), e);
            return false;
        }
    }

    /**
     * 获取所有VIP票（分页获取）
     *
     * @return 所有VIP票列表
     */
    private List<AkeVipService.VipTicketInfo> getAllVipTickets() {
        List<AkeVipService.VipTicketInfo> allVips = new ArrayList<>();
        int pageSize = 100;
        int pageNumber = 1;
        boolean hasMore = true;

        try {
            while (hasMore) {
                List<AkeVipService.VipTicketInfo> pageData = getVipTicketsByPage(pageNumber, pageSize);

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
    private List<AkeVipService.VipTicketInfo> getVipTicketsByPage(int pageNumber, int pageSize) {
        try {
            // 调用 AkeVipService 的 getVipTicket 方法
            // 传入空参数查询所有VIP票
            return akeVipService.getVipTicket("", "", "");

        } catch (Exception e) {
            log.error("查询VIP票第{}页失败", pageNumber, e);
            return new ArrayList<>();
        }
    }

    /**
     * 修复结果
     */
    @Data
    public static class FixResult {
        /** 是否成功 */
        private boolean success;

        /** 开始时间 */
        private LocalDateTime startTime;

        /** 结束时间 */
        private LocalDateTime endTime;

        /** 总数 */
        private int total;

        /** 成功数 */
        private int successCount;

        /** 失败数 */
        private int failedCount;

        /** 错误信息 */
        private String errorMessage;

        /** 失败记录 */
        private List<FailedRecord> failedRecords = new ArrayList<>();

        /**
         * 添加失败记录
         */
        public void addFailedRecord(String plateNumber, String reason) {
            failedRecords.add(new FailedRecord(plateNumber, reason));
        }

        /** 失败记录 */
        @Data
        @lombok.AllArgsConstructor
        public static class FailedRecord {
            /** 车牌号 */
            private String plateNumber;

            /** 失败原因 */
            private String reason;
        }
    }

    /**
     * 清理重复的VIP票记录
     *
     * 清理规则：
     * 1. 如果一个车牌有多条"生效中"的VIP记录
     * 2. 优先退费：VIP名称为"停用tcjc"的记录
     * 3. 然后退费：有效期包含"9999"的记录（不合理）
     * 4. 最终保留：有效期最合理的那一条（2099年是合理范围）
     *
     * @param plateNumbersStr 车牌号字符串
     * @return 清理结果
     */
    public FixResult cleanDuplicateVips(String plateNumbersStr) {
        log.info("========== 开始清理重复的VIP票记录 ==========");
        FixResult result = new FixResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // 1. 解析车牌号字符串
            List<String> plateNumbers = parsePlateNumbers(plateNumbersStr);
            
            if (plateNumbers.isEmpty()) {
                log.warn("解析车牌号列表为空");
                result.setSuccess(true);
                result.setTotal(0);
                return result;
            }

            log.info("解析到 {} 个车牌号", plateNumbers.size());

            // 2. 逐个车牌号处理
            for (String plateNumber : plateNumbers) {
                try {
                    // 根据车牌号查询VIP票
                    List<AkeVipService.VipTicketInfo> vipTickets = akeVipService.getVipTicket(plateNumber, null, null);
                    
                    if (vipTickets == null || vipTickets.isEmpty()) {
                        log.info("车牌 {} 没有VIP票，跳过", plateNumber);
                        continue;
                    }
                    
                    // 筛选出"生效中"的VIP票
                    List<AkeVipService.VipTicketInfo> activeTickets = new ArrayList<>();
                    for (AkeVipService.VipTicketInfo vip : vipTickets) {
                        if ("生效中".equals(vip.getTicketStatus()) || "1".equals(vip.getTicketStatus())) {
                            activeTickets.add(vip);
                        }
                    }
                    
                    if (activeTickets.size() <= 1) {
                        log.info("车牌 {} 只有 {} 条生效中的VIP票，无需清理", plateNumber, activeTickets.size());
                        continue;
                    }
                    
                    log.warn("车牌 {} 有 {} 条生效中的VIP票，需要清理", plateNumber, activeTickets.size());
                    result.setTotal(result.getTotal() + activeTickets.size());
                    
                    // 清理重复的VIP票
                    boolean success = cleanDuplicateTicketsForPlate(plateNumber, activeTickets, result);
                    if (success) {
                        result.setSuccessCount(result.getSuccessCount() + (activeTickets.size() - 1)); // 退费数量
                    }
                    
                } catch (Exception e) {
                    log.error("处理车牌号异常: {}", plateNumber, e);
                    result.addFailedRecord(plateNumber, "处理异常: " + e.getMessage());
                    result.setFailedCount(result.getFailedCount() + 1);
                }
            }

            result.setSuccess(true);
            log.info("========== VIP票清理完成 ==========");
            return result;

        } catch (Exception e) {
            log.error("清理重复VIP票失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
    }

    /**
     * 清理单个车牌的重复VIP票
     * 
     * @param plateNumber 车牌号
     * @param activeTickets 生效中的VIP票列表
     * @param result 修复结果
     * @return 是否成功
     */
    private boolean cleanDuplicateTicketsForPlate(String plateNumber, 
                                                   List<AkeVipService.VipTicketInfo> activeTickets,
                                                   FixResult result) {
        log.info("开始清理车牌 {} 的重复VIP票，共 {} 条", plateNumber, activeTickets.size());
        
        // 1. 找出需要退费的VIP票
        List<AkeVipService.VipTicketInfo> toRefund = new ArrayList<>();
        AkeVipService.VipTicketInfo toKeep = null;
        
        // 优先级1：退费VIP名称为"停用tcjc"的记录
        for (AkeVipService.VipTicketInfo vip : activeTickets) {
            if ("停用tcjc".equals(vip.getVipTypeName())) {
                log.info("找到需要退费的VIP票（停用tcjc）: 车牌={}, VIP类型={}, 有效期={} ~ {}",
                        plateNumber, vip.getVipTypeName(), vip.getStartTime(), vip.getEndTime());
                toRefund.add(vip);
            }
        }
        
        // 移除已标记退费的票
        activeTickets.removeAll(toRefund);

        // 优先级2：退费有效期包含"9999"的记录（不合理期限）
        // 注意：2099年是合理范围，不退费
        for (AkeVipService.VipTicketInfo vip : activeTickets) {
            String endTime = vip.getEndTime();
            if (endTime != null && endTime.contains("9999")) {
                log.info("找到需要退费的VIP票（9999不合理有效期）: 车牌={}, VIP类型={}, 有效期={} ~ {}",
                        plateNumber, vip.getVipTypeName(), vip.getStartTime(), endTime);
                toRefund.add(vip);
            }
        }
        
        // 移除已标记退费的票
        activeTickets.removeAll(toRefund);
        
        // 3. 保留剩下的第一条（应该是有效期最合理的）
        if (!activeTickets.isEmpty()) {
            toKeep = activeTickets.get(0);
            log.info("保留VIP票: 车牌={}, VIP类型={}, 有效期={} ~ {}",
                    plateNumber, toKeep.getVipTypeName(), toKeep.getStartTime(), toKeep.getEndTime());
            
            // 如果还有多余的，也加入退费列表
            for (int i = 1; i < activeTickets.size(); i++) {
                toRefund.add(activeTickets.get(i));
            }
        }
        
        // 4. 执行退费
        boolean allSuccess = true;
        for (AkeVipService.VipTicketInfo vip : toRefund) {
            log.info("退费VIP票: 车牌={}, VIP类型={}, 票序列号={}",
                    plateNumber, vip.getVipTypeName(), vip.getVipTicketSeq());
            
            boolean success = akeVipService.refundVipTicket(vip.getVipTicketSeq(), null, null, "0");
            if (!success) {
                log.warn("退费失败: 车牌={}, 票序列号={}", plateNumber, vip.getVipTicketSeq());
                result.addFailedRecord(plateNumber, "退费失败: " + vip.getVipTypeName());
                allSuccess = false;
            } else {
                log.info("退费成功: 车牌={}, VIP类型={}", plateNumber, vip.getVipTypeName());
            }
        }
        
        log.info("车牌 {} 清理完成，退费 {} 条，保留 1 条", plateNumber, toRefund.size());
        return allSuccess;
    }
}
