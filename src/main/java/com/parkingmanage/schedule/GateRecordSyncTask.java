package com.parkingmanage.schedule;

import com.parkingmanage.dto.well.WellGateRecordResponse;
import com.parkingmanage.service.oracle.OracleRecordWriteService;
import com.parkingmanage.service.well.WellGateRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 门禁记录同步定时任务
 * 定期从威尔门禁系统获取最新的进出记录
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "gate-record-sync.enabled", havingValue = "true", matchIfMissing = true)
public class GateRecordSyncTask {

    @Autowired
    private WellGateRecordService wellGateRecordService;
    
    @Autowired
    private OracleRecordWriteService oracleRecordWriteService;

    @Value("${gate-record-sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${gate-record-sync.last-sync-time-file:./data/last-gate-record-sync-time.txt}")
    private String lastSyncTimeFile;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 定时获取门禁记录
     * 默认每1分钟执行一次
     */
    @Scheduled(cron = "${gate-record-sync.cron:0 */1 * * * ?}")
    public void syncGateRecords() {
        if (!syncEnabled) {
            log.debug("门禁记录同步已禁用，跳过执行");
            return;
        }

        log.info("========================================");
        log.info("🚪 [定时任务-门禁记录同步] 开始执行 - {}", LocalDateTime.now().format(formatter));
        log.info("========================================");

        try {
            // 总是查询最近5分钟的数据
            long currentTimestamp = System.currentTimeMillis();
            long lastSyncTimestamp = currentTimestamp - (5 * 60 * 1000L); // 5分钟前

            log.info("查询时间范围: {} ~ {}",
                    formatTimestamp(lastSyncTimestamp),
                    formatTimestamp(currentTimestamp));

            // 查询门禁记录
            List<WellGateRecordResponse> records = wellGateRecordService.getGateRecordsByTimeRange(
                    lastSyncTimestamp, currentTimestamp);

            // 筛选有效记录 (recStatus = 1)
            List<WellGateRecordResponse> validRecords = records.stream()
                    .filter(record -> "1".equals(record.getRecStatus()))
                    .collect(java.util.stream.Collectors.toList());

            if (validRecords.isEmpty()) {
                log.info("✅ 本次查询无新增有效门禁记录 (总记录数: {}, 有效记录数: 0)", records.size());
            } else {
                log.info("✅ 获取到 {} 条有效门禁记录 (总记录数: {}, 过滤掉无效/报警记录: {})", 
                        validRecords.size(), records.size(), records.size() - validRecords.size());
                
                // 输出记录详情并写入Oracle数据库
                int successCount = 0;
                for (WellGateRecordResponse record : validRecords) {
                    log.info("   📋 记录: 工号={}, 姓名={}, 门={}, 方向={}, 时间={}", 
                            record.getUserNo(), 
                            record.getUserName(), 
                            record.getDoorName(),
                            parseDirection(record.getRecDic()),
                            record.getRecTime());
                    
                    // 写入Oracle数据库
                    if (oracleRecordWriteService.writePersonRecord(record)) {
                        successCount++;
                    }
                }
                
                log.info("✅ 成功写入 {} 条人员进出记录到Oracle数据库", successCount);
            }

            // 更新同步时间
            updateLastSyncTimestamp(currentTimestamp);

        } catch (Exception e) {
            log.error("❌ [定时任务-门禁记录同步] 执行异常", e);
        }

        log.info("========================================");
        log.info("🚪 [定时任务-门禁记录同步] 执行结束 - {}", LocalDateTime.now().format(formatter));
        log.info("========================================");
    }

    /**
     * 获取上次同步时间戳
     * 如果文件不存在，返回5分钟前的时间戳
     */
    private long getLastSyncTimestamp() {
        try {
            Path path = Paths.get(lastSyncTimeFile);
            if (!Files.exists(path)) {
                // 第一次运行，查询最近5分钟的数据
                long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L);
                log.info("同步时间文件不存在，首次运行，查询最近5分钟的数据");
                return fiveMinutesAgo;
            }

            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L);
                log.info("同步时间文件为空，查询最近5分钟的数据");
                return fiveMinutesAgo;
            }

            return Long.parseLong(content);
        } catch (Exception e) {
            log.error("读取同步时间文件失败: {}", e.getMessage());
            // 异常情况，返回5分钟前
            return System.currentTimeMillis() - (5 * 60 * 1000L);
        }
    }

    /**
     * 更新同步时间戳
     */
    private void updateLastSyncTimestamp(long timestamp) {
        try {
            Path path = Paths.get(lastSyncTimeFile);
            // 确保父目录存在
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 写入时间戳
            Files.write(path, String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8));
            log.debug("更新同步时间戳: {}", timestamp);
        } catch (IOException e) {
            log.error("更新同步时间文件失败: {}", e.getMessage());
        }
    }

    /**
     * 格式化时间戳为可读字符串
     */
    private String formatTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp), 
                java.time.ZoneId.systemDefault()
        ).format(formatter);
    }

    /**
     * 解析门方向
     */
    private String parseDirection(String recDic) {
        if (recDic == null) {
            return "未知";
        }
        switch (recDic) {
            case "0":
                return "进门";
            case "1":
                return "出门";
            case "255":
                return "无";
            default:
                return "未知(" + recDic + ")";
        }
    }
}
