package com.parkingmanage.controller;

import com.parkingmanage.common.R;
import com.parkingmanage.dto.SyncHistoryDTO;
import com.parkingmanage.dto.SyncStatusDTO;
import com.parkingmanage.service.oracle.OracleDataService;
import com.parkingmanage.service.sync.DataSyncService;
import com.parkingmanage.service.sync.SyncStatusService;
import com.parkingmanage.service.sync.VehicleBlacklistTestService;
import com.parkingmanage.service.sync.VipMigrationService;
import com.parkingmanage.service.sync.VipTimeFixService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据同步控制器
 * 提供同步状态查询的API
 */
@Slf4j
@RestController
@RequestMapping("/api/sync")
@Api(tags = "数据同步接口")
public class SyncController {

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private SyncStatusService syncStatusService;

    @Autowired
    private OracleDataService oracleDataService;

    @Autowired(required = false)
    private VehicleBlacklistTestService vehicleBlacklistTestService;

    @Autowired
    private VipMigrationService vipMigrationService;

    @Autowired
    private VipTimeFixService vipTimeFixService;

    /**
     * 获取同步状态
     */
    @GetMapping("/status")
    @ApiOperation("获取同步状态")
    public R getSyncStatus() {
        try {
            SyncStatusDTO status = syncStatusService.getSyncStatus();
            return R.ok().data("status", status);
        } catch (Exception e) {
            log.error("获取同步状态失败", e);
            return R.error().message("获取同步状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近同步历史
     */
    @GetMapping("/history")
    @ApiOperation("获取同步历史记录")
    public R getSyncHistory(@RequestParam(defaultValue = "20") int limit) {
        try {
            List<SyncHistoryDTO> history = syncStatusService.getRecentHistory(limit);
            return R.ok().data("history", history);
        } catch (Exception e) {
            log.error("获取同步历史失败", e);
            return R.error().message("获取同步历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近失败记录
     * 
     * @return R
     */
    @GetMapping("/failed-records")
    @ApiOperation("获取失败记录")
    public R getFailedRecords(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<String> records = syncStatusService.getRecentFailedRecords(limit);
            return R.ok().data("records", records);
        } catch (Exception e) {
            log.error("获取失败记录失败", e);
            return R.error().message("获取失败记录失败: " + e.getMessage());
        }
    }


    /**
     * 获取上次同步时间
     */
    @GetMapping("/last-sync-time")
    @ApiOperation("获取上次同步时间")
    public R getLastSyncTime() {
        try {
            return R.ok().data("lastSyncTime", dataSyncService.getLastSyncTime().toString());
        } catch (Exception e) {
            log.error("获取上次同步时间失败", e);
            return R.error().message("获取上次同步时间失败: " + e.getMessage());
        }
    }

    /**
     * 检查同步是否正在运行
     */
    @GetMapping("/running")
    @ApiOperation("检查同步是否正在运行")
    public R isSyncRunning() {
        return R.ok().data("running", dataSyncService.isSyncRunning());
    }

    /**
     * 清理过期历史记录
     */
    @DeleteMapping("/history/cleanup")
    @ApiOperation("清理过期历史记录")
    public R cleanupHistory(@RequestParam(defaultValue = "30") int keepDays) {
        try {
            syncStatusService.cleanupOldHistory(keepDays);
            return R.ok().message("清理完成，保留最近 " + keepDays + " 天的记录");
        } catch (Exception e) {
            log.error("清理历史记录失败", e);
            return R.error().message("清理历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 测试车辆黑名单同步 - 场景1：添加新黑名单
     * 
     * @param plateNumber 车牌号
     * @param gateNames 门名称列表，逗号分隔（如：化工西门,化肥西门）
     * @param startTime 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（格式：yyyy-MM-dd HH:mm:ss，为空则为永久黑名单）
     */
    @PostMapping("/test/blacklist/add")
    @ApiOperation("测试添加新黑名单")
    public R testAddBlacklist(
            @ApiParam("车牌号") @RequestParam String plateNumber,
            @ApiParam("门名称列表，逗号分隔") @RequestParam String gateNames,
            @ApiParam("车主姓名") @RequestParam(defaultValue = "测试车主") String ownerName,
            @ApiParam("开始时间") @RequestParam(required = false) String startTime,
            @ApiParam("结束时间，为空则为永久黑名单") @RequestParam(required = false) String endTime) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            log.info("测试添加黑名单 - 车牌: {}, 门: {}, 车主: {}, 时间: {} ~ {}", 
                    plateNumber, gateNames, ownerName, startTime, endTime);
            
            String result = vehicleBlacklistTestService.testAddNewBlacklist(
                    plateNumber, gateNames, ownerName, startTime, endTime);
            
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("测试添加黑名单失败", e);
            return R.error().message("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试车辆黑名单同步 - 场景2：权限变化（先删除再添加）
     * 
     * @param plateNumber 车牌号
     * @param newGateNames 新的门名称列表，逗号分隔
     */
    @PostMapping("/test/blacklist/update-permission")
    @ApiOperation("测试黑名单权限变化")
    public R testUpdateBlacklistPermission(
            @ApiParam("车牌号") @RequestParam String plateNumber,
            @ApiParam("新的门名称列表，逗号分隔") @RequestParam String newGateNames,
            @ApiParam("车主姓名") @RequestParam(defaultValue = "测试车主") String ownerName) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            log.info("测试黑名单权限变化 - 车牌: {}, 新门: {}", plateNumber, newGateNames);
            
            String result = vehicleBlacklistTestService.testUpdateBlacklistPermission(
                    plateNumber, newGateNames, ownerName);
            
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("测试黑名单权限变化失败", e);
            return R.error().message("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试车辆黑名单同步 - 场景3：时间更新（临时黑名单）
     * 
     * @param plateNumber 车牌号
     * @param newStartTime 新的开始时间
     * @param newEndTime 新的结束时间
     */
    @PostMapping("/test/blacklist/update-time")
    @ApiOperation("测试黑名单时间更新")
    public R testUpdateBlacklistTime(
            @ApiParam("车牌号") @RequestParam String plateNumber,
            @ApiParam("新的开始时间") @RequestParam String newStartTime,
            @ApiParam("新的结束时间") @RequestParam String newEndTime,
            @ApiParam("车主姓名") @RequestParam(defaultValue = "测试车主") String ownerName) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            log.info("测试黑名单时间更新 - 车牌: {}, 新时间: {} ~ {}", 
                    plateNumber, newStartTime, newEndTime);
            
            String result = vehicleBlacklistTestService.testUpdateBlacklistTime(
                    plateNumber, newStartTime, newEndTime, ownerName);
            
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("测试黑名单时间更新失败", e);
            return R.error().message("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试车辆黑名单同步 - 场景4：注销状态（删除黑名单）
     * 
     * @param plateNumber 车牌号
     */
    @PostMapping("/test/blacklist/delete")
    @ApiOperation("测试删除黑名单（注销状态）")
    public R testDeleteBlacklist(
            @ApiParam("车牌号") @RequestParam String plateNumber) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            log.info("测试删除黑名单 - 车牌: {}", plateNumber);
            
            String result = vehicleBlacklistTestService.testDeleteBlacklist(plateNumber);
            
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("测试删除黑名单失败", e);
            return R.error().message("测试失败: " + e.getMessage());
        }
    }

    /**
     * 查询车辆当前黑名单状态
     * 
     * @param plateNumber 车牌号
     */
    @GetMapping("/test/blacklist/query")
    @ApiOperation("查询车辆黑名单状态")
    public R queryBlacklistStatus(
            @ApiParam("车牌号") @RequestParam String plateNumber) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            String result = vehicleBlacklistTestService.queryBlacklistStatus(plateNumber);
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("查询黑名单状态失败", e);
            return R.error().message("查询失败: " + e.getMessage());
        }
    }

    // ==================== VIP月票测试接口 ====================

    /**
     * 测试VIP月票同步 - 场景1：开通新VIP
     * 
     * @param plateNumber 车牌号
     * @param gateNames 门名称列表，逗号分隔（如：化工西门,化肥西门）
     * @param startTime 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    @PostMapping("/test/vip/add")
    @ApiOperation("测试开通新VIP月票")
    public R testAddVip(
            @ApiParam("车牌号") @RequestParam String plateNumber,
            @ApiParam("门名称列表，逗号分隔") @RequestParam String gateNames,
            @ApiParam("车主姓名") @RequestParam(defaultValue = "测试车主") String ownerName,
            @ApiParam("开始时间") @RequestParam String startTime,
            @ApiParam("结束时间") @RequestParam String endTime) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            log.info("测试开通VIP - 车牌: {}, 门: {}, 车主: {}, 时间: {} ~ {}", 
                    plateNumber, gateNames, ownerName, startTime, endTime);
            
            String result = vehicleBlacklistTestService.testAddNewVip(
                    plateNumber, gateNames, ownerName, startTime, endTime);
            
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("测试开通VIP失败", e);
            return R.error().message("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试VIP月票同步 - 场景2：VIP权限变化（先退费再开通）
     * 
     * @param plateNumber 车牌号
     * @param newGateNames 新的门名称列表，逗号分隔
     */
    @PostMapping("/test/vip/update-permission")
    @ApiOperation("测试VIP权限变化")
    public R testUpdateVipPermission(
            @ApiParam("车牌号") @RequestParam String plateNumber,
            @ApiParam("新的门名称列表，逗号分隔") @RequestParam String newGateNames,
            @ApiParam("车主姓名") @RequestParam(defaultValue = "测试车主") String ownerName) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            log.info("测试VIP权限变化 - 车牌: {}, 新门: {}", plateNumber, newGateNames);
            
            String result = vehicleBlacklistTestService.testUpdateVipPermission(
                    plateNumber, newGateNames, ownerName);
            
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("测试VIP权限变化失败", e);
            return R.error().message("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试VIP月票同步 - 场景3：VIP时间更新（续费）
     * 
     * @param plateNumber 车牌号
     * @param newStartTime 新的开始时间
     * @param newEndTime 新的结束时间
     */
    @PostMapping("/test/vip/update-time")
    @ApiOperation("测试VIP时间更新（续费）")
    public R testUpdateVipTime(
            @ApiParam("车牌号") @RequestParam String plateNumber,
            @ApiParam("新的开始时间") @RequestParam String newStartTime,
            @ApiParam("新的结束时间") @RequestParam String newEndTime,
            @ApiParam("车主姓名") @RequestParam(defaultValue = "测试车主") String ownerName) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            log.info("测试VIP时间更新 - 车牌: {}, 新时间: {} ~ {}", 
                    plateNumber, newStartTime, newEndTime);
            
            String result = vehicleBlacklistTestService.testUpdateVipTime(
                    plateNumber, newStartTime, newEndTime, ownerName);
            
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("测试VIP时间更新失败", e);
            return R.error().message("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试VIP月票同步 - 场景4：注销状态（退费VIP）
     * 
     * @param plateNumber 车牌号
     */
    @PostMapping("/test/vip/refund")
    @ApiOperation("测试退费VIP（注销状态）")
    public R testRefundVip(
            @ApiParam("车牌号") @RequestParam String plateNumber) {
        
        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }
        
        try {
            log.info("测试退费VIP - 车牌: {}", plateNumber);
            
            String result = vehicleBlacklistTestService.testRefundVip(plateNumber);
            
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("测试退费VIP失败", e);
            return R.error().message("测试失败: " + e.getMessage());
        }
    }

    /**
     * 查询车辆VIP状态
     *
     * @param plateNumber 车牌号
     */
    @GetMapping("/test/vip/query")
    @ApiOperation("查询车辆VIP状态")
    public R queryVipStatus(
            @ApiParam("车牌号") @RequestParam String plateNumber) {

        if (vehicleBlacklistTestService == null) {
            return R.error().message("测试服务未启用");
        }

        try {
            String result = vehicleBlacklistTestService.queryVipStatus(plateNumber);
            return R.ok().message(result);
        } catch (Exception e) {
            log.error("查询VIP状态失败", e);
            return R.error().message("查询失败: " + e.getMessage());
        }
    }

    // ==================== VIP迁移接口 ====================

    /**
     * VIP迁移接口
     *
     * 功能说明：
     * 1. 查询指定车牌号的VIP月票，筛选"生效中"的数据
     * 2. 查询所有黑名单并删除
     * 3. 对VIP进行退费
     * 4. 根据VIP类型判断迁移目标：
     *    - 请停车检查格式 -> 迁移到"请停车检查（化工西化肥西复合肥）"VIP
     *    - 其他格式 -> 迁移到对应的黑名单
     *
     * @return 迁移结果
     */
    @PostMapping("/migrate-vip")
    @ApiOperation("VIP迁移接口")
    public R migrateVipToNewType() {
        String plateNumbers = "黑E101EF,黑EJ2115,黑E259EV,黑E30927,黑EGT108,黑EL6458,黑EGC773,黑ED3058,黑EK6390,黑EL7930,黑E1H036,黑EL7026,黑E2SH75,黑E03850,黑ED5686,黑EL6722,黑E8AP53,黑EH6968,黑E037LN,黑E411JQ,黑E466NK,黑E253CD,黑E206EK,黑E1UL32,黑E538JY,黑E498MN,黑EA2165,黑ED2817,黑E0RR07,黑E876MC,黑EJ3282,黑E261EJ,黑EL7595,黑EL1639,黑E638AC,黑EK8539,黑EJ2491,黑EK2117,黑E985DY,黑EGC712,黑E85669,黑EGD299,黑E117EX,黑E861JT,黑EL6011,黑EG6630,黑E891AH,黑E787EE,黑EVG053,黑E6DH75,黑E92488,黑E632CC,黑E220HB,黑E514KU,黑E8L996,黑E59267,黑E8L305,黑EL5753,黑E566CW,黑E0HF71,黑E268CD,黑E3L327,黑ED2803,黑E1DQ97,黑E208HS,黑EGJ705,黑E7EH99,黑ECS379,黑E3EN20,黑E981CN,黑E5DP31,黑E59551,黑E86378,黑E619CY,黑E569AT,黑E362EA,黑E597AB,黑E295JB,黑E293LH,黑E265DH,黑EGS202,黑EJ5295,黑E721MN,黑E701BZ,黑EA0027,黑E9MM22,黑EA2511,黑E683DK,黑E00199,黑EL1225,黑EGD219,黑EBL796,黑E8L909,黑E532CA,黑EL0986,黑EGE526,黑E84482,黑EW7163,黑E788CP,黑EL6847,黑EL2101,黑E501MD,黑EC3968,黑EJ7608,黑EJ5628,黑EA2160,黑E298LH,黑EA7358,黑E00033,黑E936CN,黑EK2758,黑EGD300,黑EGD297,黑E38725,黑EA2385,黑E1M359,黑ECF751,黑EVA375,黑ENU629,黑EJ7526,黑EC5030,黑E6EV99,黑E829MS,黑E879GD,黑E232PC,黑E231PD,黑EK3311,黑ECQ575,黑EJ6942,黑EL8481,黑E3DQ06,黑EA2398,黑EL5590,黑E00452,黑E389KC,黑EK0961,黑EK4275,黑E00025,黑E9CB31,黑E00035,黑E305CB,黑E907DB,黑E690JR,黑E263AW,黑E419PZ,黑E116AF,黑E7HC37,黑EGG692,黑E555NS,黑EK4961,黑E1V728,黑E201CW,黑EGD286,黑EW1205,黑E8DN31,黑E652CC,黑E211BW,黑E799CN,黑E5NJ60,黑EJ2069,黑EP5130,黑E321PG,黑EK8753,黑EJ3696,黑EG1689,黑E204ES,黑E00188,黑E789GS,黑E316HQ,黑E701CV,黑E620GS,黑E072CV,黑E021PX,黑E711HE,黑EL2160,黑E6Q440,黑EK6115,黑E391AZ,黑EGD583,黑E59387,黑EJ0804,黑E2BR31,黑E2CD96,黑E00430,黑EB0060,黑EBY765,黑EG6150,黑E780KU,黑EK3216,黑E1M293,黑EK6767,黑E691AC,黑E9DE72,黑EJ2646,黑EK0886,黑ECV826,黑E196PE,黑E59203,黑E775CG,黑EE4500,黑EC3906,黑E2BJ33,黑E762NV,黑E107DK,黑E357EN,黑EC6082,黑E5AT51,黑E082HW,黑EK9297,黑EE1157,黑E011KC,黑EL8252,黑E767NL,黑EC0330,黑E3HN20,黑E670KN,黑EL6066,黑EJ6593,黑EW3817,黑E596PS,黑E00031,黑E257LM,黑EJ8098,黑E806CY,黑EL0792,黑EG2375,黑ED3138,黑E394MR,黑EWG365,黑EJ1732,黑E339HN,黑E380CE,黑EC5097,黑E713BX,黑EW7062,黑E1T740,黑AE4133,黑E999MR,黑E369PQ,黑E8L895,黑ECS395,黑E603BR,黑EA2381,黑EKE870,黑ECD010,黑E701DF,黑EGE587,黑EF8595,黑EL3941,黑E182CH,黑EA2512,黑EL3999,黑E1HQ80,黑E5CP92,黑EJ6175,黑E592PG,黑EJ7601,黑E7DX18,黑E162AL,黑ERB936,黑E380HM,黑EU6967,黑E7MM19,黑E0Y257,黑EQK509,黑E0VP30,黑EK9460,黑EK7783,黑EL8086,黑E692MX,黑EH0301,黑EC3690,黑EL2622,黑E8TG12,黑E568DF,黑E965NX,黑E59271,黑EL3109,黑E2CE03,黑E1T043,黑ED3191,黑E965DL,黑E938KD,黑EJ2962,黑EK4910,黑E279CW,黑E55052,黑E660AN,黑EL5043,黑EJ7313,黑E226DR,黑E665HM,黑E152CZ,黑EC0760,黑EL4586,黑EGH280,黑EV9291,黑E901CM,黑E702CB,黑EC3883,黑EL5501,黑E8LK58,黑E893CS,黑E999NX,黑EK8491,黑EK2782,黑E050EW,黑EL7489,黑EU1702,黑E017DN,黑E0DV52,黑EK0228,黑E987DY,黑EL5007,黑E00368,黑EL6552,黑EJ0296,黑E129AT,黑ERA122,黑EJ7666,黑EK4089,黑E00183,黑EJ1236,黑E519MA,黑EK1706,黑E893BY,黑E340DK,黑EV9597,黑EBY627,黑EL2535,黑EKK633,黑E8CQ30,黑E1BB92,黑E1M456,黑EK6557,黑EL3989,黑E538CA,黑ECS511,黑E0CA38,黑E273GJ,黑E7EA59,黑E789PD,黑E3SG02,黑EUJ615,黑E1SJ97,黑EVF850,黑E9920L,黑E203FN,黑E372BJ,黑EG2510,黑E1UA86,黑E109BZ,黑EGC771,黑EF5219,黑EP3000,黑E8533U,黑ECB666,黑E18H00,黑E69W36,京JZ9097,黑EGS129,黑E6LT28,黑EDS971,黑EBL968,黑E9766B,黑E9TJ85,黑EC4994,黑EPX690,黑EGR285,黑EBN052,黑EGT378,黑EY1166,黑EDD016,黑E1701E,黑E3372H,黑E028D5,黑E79Y88,黑EE6836,黑EFF003,黑EZ7600,黑E0035N,黑E86061,黑E3QR78,黑EF29515,黑MG9955,黑E0PY51,黑E66P89,黑E0756L,黑E909LV,黑E5919N,黑E96D98,黑E1917K,黑E59F49,黑EE6611,黑EE7860,黑E651JD,黑AF33839,黑E58H68,黑E84J81,黑E78AH5,黑E67H63,黑EBK208,黑E9807H,黑E0MH29,黑EN0619,黑EQD075,黑EGU595,黑EE8711,黑EAH381,黑E5S353,黑E2127D,黑E5916E,黑E526H0,黑E59B79,黑E05D10,黑EPG799,黑E4L916,黑EBU027,黑E7JY31,黑E6709B,黑EBW856,黑E6983B,黑E69Z38,黑E70P80,黑ERF897,黑AD31123,黑E929H6,黑ED58050,黑A82SW1,黑E203DE,黑EC3935,黑EC7062,黑ED5886,黑EUK711,黑EK4705,黑EC0191,黑E2NQ80,黑E5JM99,黑EJ8555,黑EF2535,黑EBV060,黑E3337K,黑E1KZ66,黑E2GB00,黑EF16958,黑EGD303,黑E53598,黑E893AD,黑E0D777,黑E662LX,京NDQ133,黑ADF7399,黑EL5601,黑EF8586,黑E9638H,黑EF8559,黑E6FL92,黑E65J96,黑EE6306,黑E805BZ,黑E1859Y,黑EV7391,黑E8FK07,黑E09A15,黑E723BP,黑EKZ186,黑EBP209,黑E00143,黑EW6477,黑ED2823,黑E9BA08,黑EE0788,黑A1ES39,黑EF8560,黑E902BQ,黑E1GA99,黑E6379P,黑E3HJ99,黑E1GV92,黑E070BY,黑E2CE31,黑E552KG,黑ECR389,黑E9HA97,黑E901EF,黑EC8115,黑E6KW00,黑E403AB,黑E258AH,黑E6323Z,黑E3YK10,黑E53662,黑E00781,黑E0HD66,黑EJ6290,黑E1Y066,黑ER9393,黑EC7662,黑EK2312,黑E6W045,黑E3JA86,黑E9BC09,黑EH6380,黑EBL911,黑EK7088,黑EU5100,黑EE6529,黑EKP860,黑E9416M,黑E6416D,黑EY9187,黑E1375H,辽D68A68,黑EAQ578,黑E7188B,黑EV3573,黑EB4753,黑BRH643,黑E67R96,黑E0423P,黑EGE126,黑E535KV,黑EVR025,黑E03903,黑ERW591,黑E8TE69,黑E71F57,黑E1018K,黑E9E160,黑E0239K,黑E80L58,黑ECR611,黑E2691U,黑ECS337,黑E6Q037,黑EE7657,黑E8PD92,黑E756DU,黑E077JP,黑EKE881,黑E16M29,黑E0716B,黑ENT668,黑EB8257,黑E99G61,黑E00K83,黑EPR379,黑EKT936,黑E375PX,黑E72R86,黑EF98833,黑EBX266,陕A521LR,黑EGM756,黑EE7367,黑EE5763,黑E9609D,黑EBQ328,黑E3H055,黑EVG386,黑EEG526,黑E3880S,黑E7195A,黑E5L276,黑EL0075,黑EF37010,黑EK9633,黑EHU859,黑E9SG57,黑E4U296,黑E0MN98,黑E1QV91,黑E2969S,黑E26Q69,黑E3659E,黑EPL382,黑E9FD80,黑E1Q538,川A548A0,黑E5516T,黑EMM000,黑EE3103,黑EGA020,黑ED97700,黑EEB079,黑E3W005,黑E7936E,黑EBE791,黑E21R69,黑E8ZE70,黑E9EH37,黑E8AY53,黑E7045Q,黑EBF991,黑ECD791,黑EAH925,琼AV0M76,黑E05G69,黑E0579P,黑E13E61,黑ED87172,黑E3017A,黑E72F08,黑E3386R,黑ED3958,黑ELM961,黑E1109L,黑E1ZC00,黑E608JC,黑E86505,黑E865HR,黑EG5199,黑E8970J,黑E6522A,黑EKA897,黑EPD279,黑E03D28,黑EGK789,黑EQ2908,黑ED09890,黑L7801A,黑EF19813,黑E9656B,黑E73V86,黑E7QC29,黑EN6876,黑EAY191,黑E9L798,黑E3828J,黑E1201B,黑E7HX95,黑E523NF,黑EHC829,黑E1881N,黑E1377A,黑EKN159,黑EQY961,黑EN7778,黑E79T67,黑EF05379,黑EZ5326,黑E41T35,黑E1259M,黑E87G58,黑E5S328,黑E70903,黑E5227H,黑E669LX,黑EGS185,黑ERG930,黑E9680E,黑EZZ115,黑E83J15,黑EL7385,黑EUD000,黑E57Z53,黑E3UB25,黑E805N3,黑E86F33,黑E0831D,黑E4V807,黑E0059L,黑E505BG,黑E019DY,黑E03F97,黑E189KN,黑EN7857,黑EF37200,黑E99A60,黑AFG7172,黑E8780Z,黑ERB000,黑E6056C,黑E89V31,黑E15U17,黑E63L60,黑E67V95,黑EEV007,黑EFB7270,黑EF68780,吉AFS0071,黑E2182D,黑EP2999,黑M2D002,黑EJ3363,黑EFF1939,黑EG9278,黑E3CA62,黑E331L7,甘M073F3,黑EFJ009,冀A40B16,黑E7YW76,黑E7WM95,黑E5YD36,黑E159QP,黑E9YQ67,黑E9BU57,黑E6962N,黑E3736Z,黑E15H27,黑E0TJ89,黑E9VC28,黑E1387E,黑E768C8,黑EF19137,黑EF15532,黑EF16967,黑E096EB,鲁K7621Y,黑E3FH15,黑E7965A,黑E6HC00,黑E86E18,辽A612EG,黑EAK182,黑E03V22,黑EL7386,黑EB8298,黑E258ME,黑E9093W,黑ER9353,黑E24R88,黑EWR521,黑EH4967,黑ED13985,黑E8ZN37,黑EF78948,黑ES7339,黑EF65687,黑E775N5,黑E2JJ56,黑E8H635,黑E9T105,黑E044BY,黑E673BE,黑E7105L,黑ED79865,黑E7YV83,鲁BMY605,黑E716F1,黑EEJ802,黑E8891S,黑EF81699,黑E317KA,黑E1597N,黑E2E930,黑MF42221,黑E3535E,黑E2108H,黑E606EU,黑E1LD78,黑E167AS,黑E0YG62,黑EM9833,黑E918H7,黑E82G10,黑ED21277,黑ED99786,黑E517MU,黑E178JH,黑ED12527,黑EPY958,黑CFB9958,黑E35K90,黑E6MU56,黑E6C173,黑EBD998,黑EVS712,黑E5Q040,黑ED0780,黑E9L817,黑E3BA57,黑E5QU94,黑E81350,黑E177G5,黑E39V71,黑E697LN,黑EPP756,黑E72H67,黑ENL056,黑EF73688,黑E31A58,黑E87N41,黑E2BQ06,黑E7860B,黑E0XP60,黑E01V00,黑EDG370,黑E79V28,黑E6C610,黑E9KM32,黑E65788,黑E05J17,黑EKX639,黑E8282C,黑E9642H,黑E8A183,黑E7616C,黑E5182B,黑E2SF98,黑E3511A,黑ERS576,黑E1469K,黑E77Q07,黑E83A89,黑E990MZ,黑ELW128,黑E18681,黑E5098Y,黑E6B653,黑EE1657,黑E957HK,黑E80992,黑A3K5T3,黑E1FA26,黑E977CN,黑EF95987,黑E02M88,黑E8LL66,黑E2LR08,黑EAR316,黑EPY355,黑E80V84,黑E2046L,黑E6ZM26,黑E2WV90,黑E5852M,黑ERL586,黑AD21122,黑EQG165,黑E59T11,黑E0C708,黑EF17881,黑E787P2,黑E2080U,黑E0713A,黑E038E0,黑A980RV,黑E8JN93,黑E0LX22,黑E8385C,黑EJY016,黑EF36670,黑E61H69,黑E656AB,黑EFH5661,黑E78759,黑E43R68,黑E111CZ,黑ER2010,黑EF38333,黑E7A835,黑EFD5567,黑E1104H,黑E870CC,黑E1808V,黑ED68567,黑ED97770,黑E53H69,黑EWK281,黑EBP001,黑E3028F,黑E638PB,黑AT658S,黑E8N567,黑E938HB,黑EF96466,黑E79A31,黑EF15957,黑E2667E,黑E0DE77,黑E898CY,黑ECS385,黑ED43222,黑E823PS,黑E710JP,黑E5900D";
        log.info("========== 收到VIP迁移请求，车牌数量: {} ==========",
                plateNumbers != null ? plateNumbers.split(",").length : 0);

        try {
            // 解析车牌号列表
            List<String> plateNumberList = new ArrayList<>();
            if (plateNumbers != null && !plateNumbers.trim().isEmpty()) {
                String[] plates = plateNumbers.split("[,，\\s]+");
                for (String plate : plates) {
                    if (plate != null && !plate.trim().isEmpty()) {
                        plateNumberList.add(plate.trim());
                    }
                }
            }

            if (plateNumberList.isEmpty()) {
                return R.error().message("车牌号列表不能为空");
            }

            log.info("解析后的车牌号列表: {}", plateNumberList);

            VipMigrationService.MigrationResult result = vipMigrationService.migrateVipToNewType(plateNumberList);

            if (result.getErrors().isEmpty()) {
                return R.ok()
                        .message(result.getSummary())
                        .data("result", result);
            } else {
                return R.ok()
                        .message(result.getSummary())
                        .data("result", result)
                        .data("errors", result.getErrors());
            }

        } catch (Exception e) {
            log.error("VIP迁移失败", e);
            return R.error().message("VIP迁移失败: " + e.getMessage());
        }
    }

    // ==================== VIP时间修复接口 ====================

    /**
     * 修复VIP的9999结束时间（固定车牌列表）
     *
     * 功能说明：
     * 1. 根据固定的车牌号码列表查询VIP月票，筛选"生效中"且结束时间以9999开头的数据
     * 2. 对这些VIP进行退费
     * 3. 重新开通VIP，将结束时间从9999改为2099（月日时分秒保持不变）
     * 4. VIP名称、车主等信息保持和原来一样
     *
     * @return 修复结果
     */
    @PostMapping("/fix-vip-time")
    @ApiOperation("修复VIP的9999结束时间")
    public R fixVipTimeByPlateNumbers() {
        // 固定的车牌号码列表
        String plateNumbers = "黑E101EF,黑EJ2115,黑E259EV,黑E30927,黑EGT108,黑EL6458,黑EGC773,黑ED3058,黑EK6390,黑EL7930,黑E1H036,黑EL7026,黑E2SH75,黑E03850,黑ED5686,黑EL6722,黑E8AP53,黑EH6968,黑E037LN,黑E411JQ,黑E466NK,黑E253CD,黑E206EK,黑E1UL32,黑E538JY,黑E498MN,黑EA2165,黑ED2817,黑E0RR07,黑E876MC,黑EJ3282,黑E261EJ,黑EL7595,黑EL1639,黑E638AC,黑EK8539,黑EJ2491,黑EK2117,黑E985DY,黑EGC712,黑E85669,黑EGD299,黑E117EX,黑E861JT,黑EL6011,黑EG6630,黑E891AH,黑E787EE,黑EVG053,黑E6DH75,黑E92488,黑E632CC,黑E220HB,黑E514KU,黑E8L996,黑E59267,黑E8L305,黑EL5753,黑E566CW,黑E0HF71,黑E268CD,黑E3L327,黑ED2803,黑E1DQ97,黑E208HS,黑EGJ705,黑E7EH99,黑ECS379,黑E3EN20,黑E981CN,黑E5DP31,黑E59551,黑E86378,黑E619CY,黑E569AT,黑E362EA,黑E597AB,黑E295JB,黑E293LH,黑E265DH,黑EGS202,黑EJ5295,黑E721MN,黑E701BZ,黑EA0027,黑E9MM22,黑EA2511,黑E683DK,黑E00199,黑EL1225,黑EGD219,黑EBL796,黑E8L909,黑E532CA,黑EL0986,黑EGE526,黑E84482,黑EW7163,黑E788CP,黑EL6847,黑EL2101,黑E501MD,黑EC3968,黑EJ7608,黑EJ5628,黑EA2160,黑E298LH,黑EA7358,黑E00033,黑E936CN,黑EK2758,黑EGD300,黑EGD297,黑E38725,黑EA2385,黑E1M359,黑ECF751,黑EVA375,黑ENU629,黑EJ7526,黑EC5030,黑E6EV99,黑E829MS,黑E879GD,黑E232PC,黑E231PD,黑EK3311,黑ECQ575,黑EJ6942,黑EL8481,黑E3DQ06,黑EA2398,黑EL5590,黑E00452,黑E389KC,黑EK0961,黑EK4275,黑E00025,黑E9CB31,黑E00035,黑E305CB,黑E907DB,黑E690JR,黑E263AW,黑E419PZ,黑E116AF,黑E7HC37,黑EGG692,黑E555NS,黑EK4961,黑E1V728,黑E201CW,黑EGD286,黑EW1205,黑E8DN31,黑E652CC,黑E211BW,黑E799CN,黑E5NJ60,黑EJ2069,黑EP5130,黑E321PG,黑EK8753,黑EJ3696,黑EG1689,黑E204ES,黑E00188,黑E789GS,黑E316HQ,黑E701CV,黑E620GS,黑E072CV,黑E021PX,黑E711HE,黑EL2160,黑E6Q440,黑EK6115,黑E391AZ,黑EGD583,黑E59387,黑EJ0804,黑E2BR31,黑E2CD96,黑E00430,黑EB0060,黑EBY765,黑EG6150,黑E780KU,黑EK3216,黑E1M293,黑EK6767,黑E691AC,黑E9DE72,黑EJ2646,黑EK0886,黑ECV826,黑E196PE,黑E59203,黑E775CG,黑EE4500,黑EC3906,黑E2BJ33,黑E762NV,黑E107DK,黑E357EN,黑EC6082,黑E5AT51,黑E082HW,黑EK9297,黑EE1157,黑E011KC,黑EL8252,黑E767NL,黑EC0330,黑E3HN20,黑E670KN,黑EL6066,黑EJ6593,黑EW3817,黑E596PS,黑E00031,黑E257LM,黑EJ8098,黑E806CY,黑EL0792,黑EG2375,黑ED3138,黑E394MR,黑EWG365,黑EJ1732,黑E339HN,黑E380CE,黑EC5097,黑E713BX,黑EW7062,黑E1T740,黑AE4133,黑E999MR,黑E369PQ,黑E8L895,黑ECS395,黑E603BR,黑EA2381,黑EKE870,黑ECD010,黑E701DF,黑EGE587,黑EF8595,黑EL3941,黑E182CH,黑EA2512,黑EL3999,黑E1HQ80,黑E5CP92,黑EJ6175,黑E592PG,黑EJ7601,黑E7DX18,黑E162AL,黑ERB936,黑E380HM,黑EU6967,黑E7MM19,黑E0Y257,黑EQK509,黑E0VP30,黑EK9460,黑EK7783,黑EL8086,黑E692MX,黑EH0301,黑EC3690,黑EL2622,黑E8TG12,黑E568DF,黑E965NX,黑E59271,黑EL3109,黑E2CE03,黑E1T043,黑ED3191,黑E965DL,黑E938KD,黑EJ2962,黑EK4910,黑E279CW,黑E55052,黑E660AN,黑EL5043,黑EJ7313,黑E226DR,黑E665HM,黑E152CZ,黑EC0760,黑EL4586,黑EGH280,黑EV9291,黑E901CM,黑E702CB,黑EC3883,黑EL5501,黑E8LK58,黑E893CS,黑E999NX,黑EK8491,黑EK2782,黑E050EW,黑EL7489,黑EU1702,黑E017DN,黑E0DV52,黑EK0228,黑E987DY,黑EL5007,黑E00368,黑EL6552,黑EJ0296,黑E129AT,黑ERA122,黑EJ7666,黑EK4089,黑E00183,黑EJ1236,黑E519MA,黑EK1706,黑E893BY,黑E340DK,黑EV9597,黑EBY627,黑EL2535,黑EKK633,黑E8CQ30,黑E1BB92,黑E1M456,黑EK6557,黑EL3989,黑E538CA,黑ECS511,黑E0CA38,黑E273GJ,黑E7EA59,黑E789PD,黑E3SG02,黑EUJ615,黑E1SJ97,黑EVF850,黑E9920L,黑E203FN,黑E372BJ,黑EG2510,黑E1UA86,黑E109BZ,黑EGC771,黑E0713A,黑E5900D";

        log.info("========== 收到VIP时间修复请求 ==========");

        try {
            // 解析车牌号列表
            List<String> plateNumberList = new ArrayList<>();
            String[] plates = plateNumbers.split("[,，\\s]+");
            for (String plate : plates) {
                if (plate != null && !plate.trim().isEmpty()) {
                    plateNumberList.add(plate.trim());
                }
            }

            log.info("解析后的车牌号列表共 {} 个车牌", plateNumberList.size());

            VipTimeFixService.TimeFixResult result = vipTimeFixService.fixVipTimeByPlateNumbers(plateNumberList);

            if (result.getErrors().isEmpty()) {
                return R.ok()
                        .message(result.getSummary())
                        .data("result", result)
                        .data("details", result.getDetails());
            } else {
                return R.ok()
                        .message(result.getSummary())
                        .data("result", result)
                        .data("details", result.getDetails())
                        .data("errors", result.getErrors());
            }

        } catch (Exception e) {
            log.error("VIP时间修复失败", e);
            return R.error().message("VIP时间修复失败: " + e.getMessage());
        }
    }

    /**
     * 修复指定车牌的VIP的9999结束时间
     *
     * 功能说明：
     * 1. 查询指定车牌的VIP月票，筛选"生效中"且结束时间以9999开头的数据
     * 2. 对这些VIP进行退费
     * 3. 重新开通VIP，将结束时间从9999改为2099（月日时分秒保持不变）
     * 4. VIP名称、车主等信息保持和原来一样
     *
     * @param plateNumber 车牌号
     * @return 修复结果
     */
//    @PostMapping("/fix-vip-time/plate")
//    @ApiOperation("修复指定车牌的VIP的9999结束时间")
//    public R fixVipTimeByPlateNumber(
//            @ApiParam("车牌号") @RequestParam String plateNumber) {
//        log.info("========== 收到车牌VIP时间修复请求，车牌: {} ==========", plateNumber);
//
//        if (plateNumber == null || plateNumber.trim().isEmpty()) {
//            return R.error().message("车牌号不能为空");
//        }
//
//        try {
//            VipTimeFixService.TimeFixResult result = vipTimeFixService.fixVipTimeByPlateNumber(plateNumber.trim());
//
//            if (result.getErrors().isEmpty()) {
//                return R.ok()
//                        .message(result.getSummary())
//                        .data("result", result)
//                        .data("details", result.getDetails());
//            } else {
//                return R.ok()
//                        .message(result.getSummary())
//                        .data("result", result)
//                        .data("details", result.getDetails())
//                        .data("errors", result.getErrors());
//            }
//
//        } catch (Exception e) {
//            log.error("车牌 {} VIP时间修复失败", plateNumber, e);
//            return R.error().message("VIP时间修复失败: " + e.getMessage());
//        }
//    }

    /**
     * 查询指定车牌的VIP信息（检查是否有9999开头的结束时间）
     *
     * @param plateNumber 车牌号
     * @return 查询结果
     */
    @GetMapping("/check-vip-time")
    @ApiOperation("查询指定车牌的VIP时间信息")
    public R checkVipTime(
            @ApiParam("车牌号") @RequestParam String plateNumber) {
        log.info("========== 查询VIP时间信息，车牌: {} ==========", plateNumber);

        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            return R.error().message("车牌号不能为空");
        }

        try {
            StringBuilder result = new StringBuilder();
            result.append("车牌: ").append(plateNumber).append("\n\n");

            // 这里可以添加具体的查询逻辑
            // 暂时返回提示信息
            result.append("请使用修复接口来处理9999开头的结束时间。\n");
            result.append("接口：POST /api/sync/fix-vip-time/plate?plateNumber=").append(plateNumber);

            return R.ok().message(result.toString());

        } catch (Exception e) {
            log.error("查询VIP时间信息失败，车牌: {}", plateNumber, e);
            return R.error().message("查询失败: " + e.getMessage());
        }
    }
}
