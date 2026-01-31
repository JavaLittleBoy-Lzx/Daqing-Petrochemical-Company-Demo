package com.parkingmanage.service.sync;

import com.parkingmanage.dto.SyncHistoryDTO;
import com.parkingmanage.dto.SyncResult;
import com.parkingmanage.dto.SyncStatusDTO;

import java.util.List;

/**
 * @author: Jia
 * 同步状态服务接口
 * 管理同步历史记录和状态查询
 * 
 * Requirements: 8.1, 8.2
 */
public interface SyncStatusService {

    /**
     * 保存同步结果到历史记录
     * 
     * @param result 同步结果
     */
    void saveSyncHistory(SyncResult result);

    /**
     * @author: Jia
     * 获取完整同步状态
     * 包括运行状态、最近同步结果、历史记录等
     * 
     * @return 同步状态
     */
    SyncStatusDTO getSyncStatus();

    /**
     * 获取最近的同步历史记录
     * 
     * @param limit 返回记录数量限制
     * @return 历史记录列表
     */
    List<SyncHistoryDTO> getRecentHistory(int limit);

    /**
     * 获取最近的失败记录
     * 
     * @param limit 返回记录数量限制
     * @return 失败记录列表
     */
    List<String> getRecentFailedRecords(int limit);

    /**
     * 获取最后一次同步结果
     * 
     * @return 最后一次同步结果，如果没有则返回null
     */
    SyncResult getLastSyncResult();

    /**
     * 清理过期的历史记录
     * 
     * @param keepDays 保留天数
     */
    void cleanupOldHistory(int keepDays);
}
