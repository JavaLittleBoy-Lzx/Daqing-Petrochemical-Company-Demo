package com.parkingmanage.schedule;

import com.parkingmanage.dto.SyncResult;
import com.parkingmanage.service.sync.DataSyncService;
import com.parkingmanage.service.sync.SyncStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据同步定时任务
 * 从Oracle获取人员和车辆数据，同步到威尔门禁系统和AKE停车系统
 * 
 * Requirements: 7.1, 8.1, 8.2
 * 
 * @author System
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sync.enabled", havingValue = "true", matchIfMissing = true)
public class DataSyncScheduledTask {

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private SyncStatusService syncStatusService;

    @Value("${sync.enabled:true}")
    private boolean syncEnabled;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 定时执行数据同步任务
     * 使用配置文件中的cron表达式控制执行频率
     * 默认每5分钟执行一次
     * 
     * Requirements: 7.1, 8.1, 8.2
     */
    @Scheduled(cron = "${sync.cron:0 */5 * * * ?}")
    public void executeDataSync() {
        if (!syncEnabled) {
            log.debug("定时同步已禁用，跳过执行");
            return;
        }

        String startTime = LocalDateTime.now().format(formatter);
        log.info("========================================");
        log.info("🕐 [定时任务-数据同步] 开始执行 - {}", startTime);
        log.info("========================================");

        try {
            // 检查是否有同步任务正在运行
            if (dataSyncService.isSyncRunning()) {
                log.warn("⚠️ 同步任务正在运行中，跳过本次定时执行");
                return;
            }

            // 执行完整同步
            SyncResult result = dataSyncService.executeSync();

            // 保存同步历史记录 (Requirements: 8.1, 8.2)
            syncStatusService.saveSyncHistory(result);

            // 输出同步结果
            if (result.isSuccess()) {
                log.info("✅ [定时任务-数据同步] 执行成功");
                log.info("   人员同步: 总数={}, 成功={}, 失败={}", 
                        result.getPersonTotal(), result.getPersonSuccess(), result.getPersonFailed());
                log.info("   车辆同步: 总数={}, 成功={}, 失败={}", 
                        result.getVehicleTotal(), result.getVehicleSuccess(), result.getVehicleFailed());
                log.info("   黑名单: 总数={}", result.getBlacklistTotal());
                log.info("   耗时: {}ms", result.getDuration());
            } else {
                log.error("❌ [定时任务-数据同步] 执行失败: {}", result.getErrorMessage());
            }
            // 输出失败记录
            if (result.getFailedRecords() != null && !result.getFailedRecords().isEmpty()) {
                log.warn("⚠️ 失败记录 ({} 条):", result.getFailedRecords().size());
                for (String record : result.getFailedRecords()) {
                    log.warn("   - {}", record);
                }
            }
        } catch (Exception e) {
            log.error("❌ [定时任务-数据同步] 执行异常", e);
        }
        String endTime = LocalDateTime.now().format(formatter);
        log.info("========================================");
        log.info("🕐 [定时任务-数据同步] 执行结束 - {}", endTime);
        log.info("========================================");
    }
}
