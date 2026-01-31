package com.parkingmanage.service.sync.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parkingmanage.dto.SyncHistoryDTO;
import com.parkingmanage.dto.SyncResult;
import com.parkingmanage.dto.SyncStatusDTO;
import com.parkingmanage.service.sync.DataSyncService;
import com.parkingmanage.service.sync.SyncStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 同步状态服务实现类
 * 使用文件存储同步历史记录
 * 
 * Requirements: 8.1, 8.2
 */
@Slf4j
@Service
public class SyncStatusServiceImpl implements SyncStatusService {

    @Autowired
    private DataSyncService dataSyncService;

    @Value("${sync.history-file:./data/sync-history.json}")
    private String historyFilePath;

    @Value("${sync.max-history-count:100}")
    private int maxHistoryCount;

    /** 内存中的历史记录缓存 */
    private final CopyOnWriteArrayList<SyncHistoryDTO> historyCache = new CopyOnWriteArrayList<>();

    /** 最后一次同步结果 */
    private volatile SyncResult lastSyncResult;

    /** JSON序列化工具 */
    private final ObjectMapper objectMapper;

    public SyncStatusServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        loadHistoryFromFile();
    }


    /**
     * 从文件加载历史记录
     */
    private void loadHistoryFromFile() {
        try {
            Path path = Paths.get(historyFilePath);
            if (!Files.exists(path)) {
                log.info("同步历史文件不存在，将创建新文件: {}", historyFilePath);
                return;
            }

            String content = new String(Files.readAllBytes(path));
            if (content.trim().isEmpty()) {
                return;
            }

            List<SyncHistoryDTO> history = objectMapper.readValue(content, 
                    new TypeReference<List<SyncHistoryDTO>>() {});
            historyCache.addAll(history);
            log.info("从文件加载了 {} 条同步历史记录", history.size());

            // 设置最后一次同步结果
            if (!historyCache.isEmpty()) {
                SyncHistoryDTO lastHistory = historyCache.get(historyCache.size() - 1);
                lastSyncResult = convertToSyncResult(lastHistory);
            }

        } catch (Exception e) {
            log.error("加载同步历史文件失败: {}", e.getMessage());
        }
    }

    /**
     * 保存历史记录到文件
     */
    private void saveHistoryToFile() {
        try {
            Path path = Paths.get(historyFilePath);
            
            // 确保父目录存在
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            String content = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(historyCache);
            Files.write(path, content.getBytes());
            log.debug("同步历史已保存到文件: {}", historyFilePath);

        } catch (IOException e) {
            log.error("保存同步历史文件失败: {}", e.getMessage());
        }
    }

    @Override
    public void saveSyncHistory(SyncResult result) {
        log.info("保存同步历史记录 - 人员:{}/{}, 车辆:{}/{}", 
                result.getPersonSuccess(), result.getPersonTotal(),
                result.getVehicleSuccess(), result.getVehicleTotal());

        // 创建历史记录
        SyncHistoryDTO history = new SyncHistoryDTO();
        history.setSyncId(String.valueOf(System.currentTimeMillis()));
        history.setStartTime(result.getSyncTime().minusNanos(result.getDuration() * 1_000_000));
        history.setEndTime(result.getSyncTime());
        history.setDuration(result.getDuration());
        history.setSuccess(result.isSuccess());
        history.setErrorMessage(result.getErrorMessage());
        history.setPersonTotal(result.getPersonTotal());
        history.setPersonSuccess(result.getPersonSuccess());
        history.setPersonFailed(result.getPersonFailed());
        history.setVehicleTotal(result.getVehicleTotal());
        history.setVehicleSuccess(result.getVehicleSuccess());
        history.setVehicleFailed(result.getVehicleFailed());
        history.setBlacklistTotal(result.getBlacklistTotal());
        history.setBlacklistSuccess(result.getBlacklistSuccess());
        history.setFailedRecords(result.getFailedRecords());

        // 添加到缓存
        historyCache.add(history);

        // 限制历史记录数量
        while (historyCache.size() > maxHistoryCount) {
            historyCache.remove(0);
        }

        // 更新最后一次同步结果
        lastSyncResult = result;

        // 保存到文件
        saveHistoryToFile();
    }

    @Override
    public SyncStatusDTO getSyncStatus() {
        SyncStatusDTO status = new SyncStatusDTO();
        
        // 运行状态
        status.setRunning(dataSyncService.isSyncRunning());
        
        // 时间信息
        status.setLastSyncTime(dataSyncService.getLastSyncTime());
        status.setCurrentTime(LocalDateTime.now());
        
        // 最后一次同步结果
        status.setLastSyncResult(lastSyncResult);
        
        // 最近历史记录（最多10条）
        status.setRecentHistory(getRecentHistory(10));
        
        // 失败记录统计
        SyncStatusDTO.FailedRecordSummary failedSummary = new SyncStatusDTO.FailedRecordSummary();
        int personFailed = 0;
        int vehicleFailed = 0;
        List<String> recentFailed = new ArrayList<>();
        
        // 统计最近10次同步的失败记录
        List<SyncHistoryDTO> recentHistory = getRecentHistory(10);
        for (SyncHistoryDTO history : recentHistory) {
            personFailed += history.getPersonFailed();
            vehicleFailed += history.getVehicleFailed();
            if (history.getFailedRecords() != null) {
                recentFailed.addAll(history.getFailedRecords());
            }
        }
        
        failedSummary.setPersonFailedCount(personFailed);
        failedSummary.setVehicleFailedCount(vehicleFailed);
        // 只保留最近20条失败记录
        failedSummary.setRecentFailedRecords(
                recentFailed.stream().limit(20).collect(Collectors.toList()));
        status.setFailedSummary(failedSummary);
        
        return status;
    }

    @Override
    public List<SyncHistoryDTO> getRecentHistory(int limit) {
        if (historyCache.isEmpty()) {
            return Collections.emptyList();
        }
        
        int size = historyCache.size();
        int start = Math.max(0, size - limit);
        
        // 返回最近的记录，按时间倒序
        List<SyncHistoryDTO> result = new ArrayList<>(historyCache.subList(start, size));
        Collections.reverse(result);
        return result;
    }

    @Override
    public List<String> getRecentFailedRecords(int limit) {
        List<String> allFailed = new ArrayList<>();
        
        // 从最近的历史记录中收集失败记录
        List<SyncHistoryDTO> recentHistory = getRecentHistory(20);
        for (SyncHistoryDTO history : recentHistory) {
            if (history.getFailedRecords() != null) {
                allFailed.addAll(history.getFailedRecords());
            }
            if (allFailed.size() >= limit) {
                break;
            }
        }
        
        return allFailed.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public SyncResult getLastSyncResult() {
        return lastSyncResult;
    }

    @Override
    public void cleanupOldHistory(int keepDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(keepDays);
        
        int removedCount = 0;
        while (!historyCache.isEmpty()) {
            SyncHistoryDTO oldest = historyCache.get(0);
            if (oldest.getEndTime() != null && oldest.getEndTime().isBefore(cutoffTime)) {
                historyCache.remove(0);
                removedCount++;
            } else {
                break;
            }
        }
        
        if (removedCount > 0) {
            log.info("清理了 {} 条过期的同步历史记录", removedCount);
            saveHistoryToFile();
        }
    }

    /**
     * 将历史记录转换为同步结果
     */
    private SyncResult convertToSyncResult(SyncHistoryDTO history) {
        SyncResult result = new SyncResult();
        result.setSyncTime(history.getEndTime());
        result.setDuration(history.getDuration());
        result.setSuccess(history.isSuccess());
        result.setErrorMessage(history.getErrorMessage());
        result.setPersonTotal(history.getPersonTotal());
        result.setPersonSuccess(history.getPersonSuccess());
        result.setPersonFailed(history.getPersonFailed());
        result.setVehicleTotal(history.getVehicleTotal());
        result.setVehicleSuccess(history.getVehicleSuccess());
        result.setVehicleFailed(history.getVehicleFailed());
        result.setBlacklistTotal(history.getBlacklistTotal());
        result.setBlacklistSuccess(history.getBlacklistSuccess());
        if (history.getFailedRecords() != null) {
            result.setFailedRecords(new ArrayList<>(history.getFailedRecords()));
        }
        return result;
    }
}
