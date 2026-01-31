package com.parkingmanage.service.sync;

import com.parkingmanage.dto.PersonSyncResult;
import com.parkingmanage.dto.SyncResult;
import com.parkingmanage.dto.VehicleSyncResult;

import java.time.LocalDateTime;

/**
 * 数据同步主服务接口
 * 协调各服务完成数据同步
 * 
 * Requirements: 7.1
 */
public interface DataSyncService {

    /**
     * 执行完整同步
     * 包括人员同步和车辆同步
     * 
     * @return 同步结果
     */
    SyncResult executeSync();

    /**
     * 同步人员数据
     * 从Oracle获取最新人员数据，同步到威尔门禁系统
     * 
     * Requirements: 3.1, 3.2, 3.3, 3.4
     * 
     * @return 人员同步结果
     */
    PersonSyncResult syncPersonData();

    /**
     * 同步车辆数据
     * 从Oracle获取最新车辆数据，同步到AKE停车系统
     * 
     * Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.2
     * 
     * @return 车辆同步结果
     */
    VehicleSyncResult syncVehicleData(); 

    /**
     * 获取上次同步时间
     * 
     * Requirements: 2.3, 7.2
     * 
     * @return 上次同步时间，如果没有则返回默认时间
     */
    LocalDateTime getLastSyncTime();

    /**
     * 更新同步时间
     * 
     * Requirements: 7.2
     * 
     * @param time 同步时间
     */
    void updateLastSyncTime(LocalDateTime time);

    /**
     * 检查同步服务是否正在运行
     * 
     * @return true表示正在运行
     */
    boolean isSyncRunning();
}
