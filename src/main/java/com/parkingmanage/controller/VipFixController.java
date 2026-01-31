package com.parkingmanage.controller;

import com.parkingmanage.common.R;
import com.parkingmanage.service.ake.VipFixService;
import com.parkingmanage.service.sync.VipSupplementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * VIP票数据修复控制器
 * 提供VIP票数据修复的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/vip/fix")
@Api(tags = "VIP票数据修复接口")
public class VipFixController {

    @Autowired
    private VipFixService vipFixService;

    @Autowired
    private VipSupplementService vipSupplementService;

    /**
     * 修复没有手机号的VIP票（智能补全车主姓名）
     *
     * 流程：
     * 1. 查询所有VIP票
     * 2. 筛选出telphone为空的记录
     * 3. 生成唯一手机号（时间戳格式：13 + 时间戳后9位）
     * 4. 智能判断是否需要补全车主姓名：
     *    - 如果车主姓名为空或"无"，则使用车牌号作为车主姓名
     *    - 如果车主姓名有值，则保持不变
     * 5. 先退费，再用补全的信息重新开通
     *
     * 覆盖场景：
     * - 有车主姓名 + 没手机号 → 补手机号
     * - 没车主姓名 + 没手机号 → 补手机号 + 补车主姓名
     * - 车主姓名是"无" + 没手机号 → 补手机号 + 用车牌号替换"无"
     *
     * @return 修复结果
     */
    @PostMapping("/phone")
    @ApiOperation("修复没有手机号的VIP票（智能补全车主姓名）")
    public R fixVipsWithoutPhone() {
        log.info("========== 收到修复没有手机号VIP票的请求 ==========");

        try {
            VipFixService.FixResult result = vipFixService.fixVipsWithoutPhone();

            if (result.isSuccess()) {
                return R.ok()
                        .message("修复完成")
                        .data("total", result.getTotal())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount())
                        .data("startTime", result.getStartTime())
                        .data("endTime", result.getEndTime())
                        .data("failedRecords", result.getFailedRecords());
            } else {
                return R.error()
                        .message("修复失败: " + result.getErrorMessage())
                        .data("total", result.getTotal())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount());
            }

        } catch (Exception e) {
            log.error("修复没有手机号的VIP票失败", e);
            return R.error().message("修复失败: " + e.getMessage());
        }
    }

    /**
     * 修复既没有手机号也没有车主姓名的VIP票
     *
     * 流程：
     * 1. 查询所有VIP票
     * 2. 筛选出telphone和car_owner都为空的记录
     * 3. 生成唯一手机号（时间戳）
     * 4. 车主姓名使用车牌号
     * 5. 先退费，再用补全的信息重新开通
     *
     * @return 修复结果
     */
    @PostMapping("/phone-and-owner")
    @ApiOperation("修复既没有手机号也没有车主姓名的VIP票")
    public R fixVipsWithoutPhoneAndOwner() {
        log.info("========== 收到修复既没有手机号也没有车主姓名VIP票的请求 ==========");

        try {
            VipFixService.FixResult result = vipFixService.fixVipsWithoutPhoneAndOwner();

            if (result.isSuccess()) {
                return R.ok()
                        .message("修复完成")
                        .data("total", result.getTotal())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount())
                        .data("startTime", result.getStartTime())
                        .data("endTime", result.getEndTime())
                        .data("failedRecords", result.getFailedRecords());
            } else {
                return R.error()
                        .message("修复失败: " + result.getErrorMessage())
                        .data("total", result.getTotal())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount());
            }

        } catch (Exception e) {
            log.error("修复既没有手机号也没有车主姓名的VIP票失败", e);
            return R.error().message("修复失败: " + e.getMessage());
        }
    }

    /**
     * 根据指定车牌号列表修复VIP票（智能补全手机号和车主姓名）
     *
     * 流程：
     * 1. 解析传入的车牌号字符串（支持逗号、分号、换行符分隔）
     * 2. 根据车牌号查询对应的VIP票
     * 3. 生成唯一手机号
     * 4. 智能判断是否需要补全车主姓名
     * 5. 先退费，再用补全的信息重新开通
     *
     * @return 修复结果
     */
    @PostMapping("/phone-by-plates")
    @ApiOperation("根据指定车牌号列表修复VIP票")
    public R fixVipsByPlateNumbers() {
       String plateNumbersStr = "黑E710JP,黑E823PS,黑ED43222,黑ECS385,黑E898CY,黑E0DE77,黑E2667E,黑EF15957,黑E79A31,黑EF96466,黑E938HB,黑E8N567,黑AT658S,黑E638PB,黑E3028F,黑EBP001,黑EWK281,黑E53H69,黑ED97770,黑ED68567,黑E1808V,黑E870CC,黑E1104H,黑EFD5567,黑E7A835,黑EF38333,黑ER2010,黑E111CZ,黑E43R68,黑E78759,黑EFH5661,黑E656AB,黑E61H69,黑EF36670,黑EJY016,黑E8385C,黑E0LX22,黑E8JN93,黑A980RV,黑E038E0,黑E2080U,黑E787P2,黑E0C708,黑E59T11,黑EQG165,黑AD21122,黑ERL586,黑E5852M,黑E2WV90,黑E6ZM26,黑E2046L,黑E80V84,黑EPY355,黑EAR316,黑E2LR08,黑E8LL66,黑E02M88,黑EF95987,黑E977CN,黑E1FA26,黑A3K5T3,黑E80992,黑E957HK,黑EE1657,黑E6B653,黑E5098Y,黑E18681,黑ELW128,黑E990MZ,黑E83A89,黑E77Q07,黑E1469K,黑ERS576,黑E3511A,黑E2SF98,黑E5182B,黑E7616C,黑E8A183,黑E9642H,黑E8282C,黑EKX639,黑E05J17,黑E65788,黑E9KM32,黑E6C610,黑E79V28,黑EDG370,黑E01V00,黑E0XP60,黑E7860B,黑E2BQ06,黑E87N41,黑E31A58,黑EF73688,黑ENL056,黑E72H67,黑EPP756,黑E697LN,黑E39V71,黑E177G5,黑E81350,黑E5QU94,黑E3BA57,黑E9L817,黑ED0780,黑EVS712,黑EBD998,黑E6C173,黑E35K90,黑EPY958,黑E178JH,黑E517MU,黑ED99786,黑ED21277,黑E918H7,黑EM9833,黑E0YG62,黑E167AS,黑E1LD78,黑E2108H,黑E3535E,黑MF42221,黑E2E930,黑E1597N,黑E317KA,黑EF81699,黑EEJ802,黑E716F1,鲁BMY605,黑E7YV83,黑ED79865,黑E7105L,黑E673BE,黑E044BY,黑E9T105,黑E8H635,黑E2JJ56,黑E775N5,黑EF65687,黑ES7339,黑EF78948,黑E8ZN37,黑ED13985,黑EH4967,黑EWR521,黑E24R88,黑ER9353,黑E9093W,黑E258ME,黑EB8298,黑EL7386,黑E03V22,黑EAK182,辽A612EG,黑E86E18,黑E6HC00,黑E7965A,黑E3FH15,鲁K7621Y,黑E096EB,黑EF16967,黑EF15532,黑E768C8,黑E1387E,黑E9VC28,黑E0TJ89,黑E15H27,黑E3736Z,黑E3CA62,黑EG9278,黑EFF1939,黑EJ3363,黑M2D002,黑EF68780,黑EEV007,黑E5900D,黑E0713A,黑EGC771,黑E109BZ,黑E1UA86,黑EG2510,黑E372BJ,黑E203FN,黑E9920L,黑EVF850,黑E1SJ97,黑EUJ615,黑E3SG02,黑E789PD,黑E7EA59,黑E273GJ,黑E0CA38,黑ECS511,黑E538CA,黑EL3989,黑EK6557,黑E1M456,黑E1BB92,黑E8CQ30,黑EKK633,黑EL2535,黑EBY627,黑EV9597,黑E340DK,黑E893BY,黑EK1706,黑E519MA,黑EJ1236,黑E00183,黑EK4089,黑EJ7666,黑ERA122,黑E129AT,黑EJ0296,黑EL6552,黑E00368,黑EL5007,黑E987DY,黑EK0228,黑E0DV52,黑E017DN,黑EU1702,黑EL7489,黑E050EW,黑EK2782,黑EK8491,黑E999NX,黑E893CS,黑E8LK58,黑EL5501,黑EC3883,黑E702CB,黑E901CM,黑EV9291,黑EGH280,黑EL4586,黑EC0760,黑E152CZ,黑E665HM,黑E226DR,黑EJ7313,黑EL5043,黑E660AN,黑E55052,黑E279CW,黑EK4910,黑EJ2962,黑E938KD,黑E965DL,黑ED3191,黑E1T043,黑E2CE03,黑EL3109,黑E59271,黑E965NX,黑E568DF,黑E8TG12,黑EL2622,黑EC3690,黑EH0301,黑E692MX,黑EL8086,黑EK7783,黑EK9460,黑E0VP30,黑EQK509,黑E0Y257,黑E7MM19,黑EU6967,黑E380HM,黑ERB936,黑E162AL,黑E7DX18,黑EJ7601,黑E592PG,黑EJ6175,黑E5CP92,黑E1HQ80,黑EL3999,黑EA2512,黑E182CH,黑EL3941,黑EF8595,黑EGE587,黑E701DF,黑ECD010,黑EKE870,黑EA2381,黑E603BR,黑ECS395,黑E8L895,黑E369PQ,黑E999MR,黑AE4133,黑E1T740,黑EW7062,黑E713BX,黑EC5097,黑E380CE,黑E339HN,黑EJ1732,黑EWG365,黑E394MR,黑ED3138,黑EG2375,黑EL0792,黑E806CY,黑EJ8098,黑E257LM,黑E00031,黑E596PS,黑EW3817,黑EJ6593,黑EL6066,黑E670KN,黑E3HN20,黑EC0330,黑E767NL,黑EL8252,黑E011KC,黑EE1157,黑EK9297,黑E082HW,黑E5AT51,黑EC6082,黑E357EN,黑E107DK,黑E762NV,黑E2BJ33,黑EC3906,黑EE4500,黑E775CG,黑E59203,黑E196PE,黑ECV826,黑EK0886,黑EJ2646,黑E9DE72,黑E691AC,黑EK6767,黑E1M293,黑EK3216,黑E780KU,黑EG6150,黑EBY765,黑EB0060,黑E00430,黑E2CD96,黑E2BR31,黑EJ0804,黑E59387,黑EGD583,黑E391AZ,黑EK6115,黑E6Q440,黑EL2160,黑E711HE,黑E021PX,黑E072CV,黑E620GS,黑E701CV,黑E316HQ,黑E789GS,黑E00188,黑E204ES,黑EG1689,黑EJ3696,黑EK8753,黑E321PG,黑EP5130,黑EJ2069,黑E5NJ60,黑E799CN,黑E211BW,黑E652CC,黑E8DN31,黑EW1205,黑EGD286,黑E201CW,黑E1V728,黑EK4961,黑E555NS,黑EGG692,黑E7HC37,黑E116AF,黑E419PZ,黑E263AW,黑E690JR,黑E907DB,黑E305CB,黑E00035,黑E9CB31,黑E00025,黑EK4275,黑EK0961,黑E389KC,黑E00452,黑EL5590,黑EA2398,黑E3DQ06,黑EL8481,黑EJ6942,黑ECQ575,黑EK3311,黑E231PD,黑E232PC,黑E879GD,黑E829MS,黑E6EV99,黑EC5030,黑EJ7526,黑ENU629,黑EVA375,黑ECF751,黑E1M359,黑EA2385,黑E38725,黑EGD297,黑EGD300,黑EK2758,黑E936CN,黑E00033,黑EA7358,黑E298LH,黑EA2160,黑EJ5628,黑EJ7608,黑EC3968,黑E501MD,黑EL2101,黑EL6847,黑E788CP,黑EW7163,黑E84482,黑EGE526,黑EL0986,黑E532CA,黑E8L909,黑EBL796,黑EGD219,黑EL1225,黑E00199,黑E683DK,黑EA2511,黑E9MM22,黑EA0027,黑E701BZ,黑E721MN,黑EJ5295,黑EGS202,黑E265DH,黑E293LH,黑E295JB,黑E597AB,黑E362EA,黑E569AT,黑E619CY,黑E86378,黑E59551,黑E5DP31,黑E981CN,黑E3EN20,黑ECS379,黑E7EH99,黑EGJ705,黑E208HS,黑E1DQ97,黑ED2803,黑E3L327,黑E268CD,黑E0HF71,黑E566CW,黑EL5753,黑E8L305,黑E59267,黑E8L996,黑E514KU,黑E220HB,黑E632CC,黑E92488,黑E6DH75,黑EVG053,黑E787EE,黑E891AH,黑EG6630,黑EL6011,黑E861JT,黑E117EX,黑EGD299,黑E85669,黑EGC712,黑E985DY,黑EK2117,黑EJ2491,黑EK8539,黑E638AC,黑EL1639,黑EL7595,黑E261EJ,黑EJ3282,黑E876MC,黑E0RR07,黑ED2817,黑EA2165,黑E498MN,黑E538JY,黑E1UL32,黑E206EK,黑E253CD,黑E466NK,黑E411JQ,黑E037LN,黑EH6968,黑E8AP53,黑EL6722,黑ED5686,黑E03850,黑E2SH75,黑EL7026,黑E1H036,黑EL7930,黑EK6390,黑ED3058,黑EGC773,黑EL6458,黑EGT108,黑E30927,黑E259EV,黑EJ2115,黑E101EF,黑EM4857,黑MX6999,黑ME7177,黑EJ0128,黑EL9264,黑EH4112,黑ABL725,黑EL8488,吉JH1295,辽HA5809,辽HB9166,黑EJ7145,黑ME0268"; // 从请求中获取车牌号字符串
        log.info("========== 收到根据车牌号列表修复VIP票的请求 ==========");
        log.info("车牌号字符串: {}", plateNumbersStr);

        try {
            if (plateNumbersStr == null || plateNumbersStr.trim().isEmpty()) {
                return R.error().message("车牌号列表不能为空");
            }

            VipFixService.FixResult result = vipFixService.fixVipsByPlateNumbers(plateNumbersStr);

            if (result.isSuccess()) {
                return R.ok()
                        .message("修复完成")
                        .data("total", result.getTotal())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount())
                        .data("startTime", result.getStartTime())
                        .data("endTime", result.getEndTime())
                        .data("failedRecords", result.getFailedRecords());
            } else {
                return R.error()
                        .message("修复失败: " + result.getErrorMessage())
                        .data("total", result.getTotal())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount());
            }

        } catch (Exception e) {
            log.error("根据车牌号列表修复VIP票失败", e);
            return R.error().message("修复失败: " + e.getMessage());
        }
    }

    /**
     * 为指定车牌补开VIP月票
     * 
     * 流程：
     * 1. 查询车牌的VIP记录
     * 2. 找到"停用tcjc"类型的VIP
     * 3. 使用相同的有效期开通"请停车检查(化工西化肥西复合肥南)"VIP
     *
     * @return 补开结果
     */
    @PostMapping("/supplement-vip")
    @ApiOperation("为指定车牌补开VIP月票")
    public R supplementVipForPlates() {
        String plateNumbersStr = "黑E627KW,黑E7105L,黑E8891S,黑EF50377,黑EFF1939,黑EJ3363,黑EP2999,黑M2D002";
        log.info("========== 收到补开VIP月票的请求 ==========");
        log.info("车牌号字符串: {}", plateNumbersStr);

        try {
            if (plateNumbersStr == null || plateNumbersStr.trim().isEmpty()) {
                return R.error().message("车牌号列表不能为空");
            }

            // 解析车牌号列表
            String[] plates = plateNumbersStr.split("[,，;；\\s]+");
            List<String> plateList = new ArrayList<>();
            for (String plate : plates) {
                if (plate != null && !plate.trim().isEmpty()) {
                    plateList.add(plate.trim());
                }
            }

            if (plateList.isEmpty()) {
                return R.error().message("未解析到有效的车牌号");
            }

            log.info("解析到 {} 个车牌号", plateList.size());

            // 调用补开服务
            com.parkingmanage.service.sync.VipSupplementService.SupplementResult result = 
                    vipSupplementService.supplementVipForPlates(plateList);

            if (result.getSuccessCount() > 0) {
                return R.ok()
                        .message("补开完成")
                        .data("total", result.getTotalPlateCount())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount())
                        .data("skipCount", result.getSkipCount())
                        .data("startTime", result.getStartTime())
                        .data("endTime", result.getEndTime())
                        .data("details", result.getDetails())
                        .data("errors", result.getErrors());
            } else {
                return R.error()
                        .message("补开失败")
                        .data("total", result.getTotalPlateCount())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount())
                        .data("skipCount", result.getSkipCount())
                        .data("errors", result.getErrors());
            }

        } catch (Exception e) {
            log.error("补开VIP月票失败", e);
            return R.error().message("补开失败: " + e.getMessage());
        }
    }

    /**
     * 清理重复的VIP票记录（保留一条，退费其他）
     *
     * 清理规则：
     * 1. 如果一个车牌有多条"生效中"的VIP记录
     * 2. 优先退费：VIP名称为"停用tcjc"的记录
     * 3. 然后退费：有效期包含"9999"的记录（不合理期限）
     * 4. 最终保留：有效期最合理的那一条（2099年是合理范围）
     *
     * @return 清理结果
     */
    @PostMapping("/clean-duplicates")
    @ApiOperation("清理重复的VIP票记录")
    public R cleanDuplicateVips() {
        String plateNumbersStr = "黑E0713A,黑E35K90,黑EF15957,黑E5900D,黑E02M88,黑E101EF,黑EJ2115,黑E30927,黑EGT108,黑EGC773,黑ED3058,黑EK6390,黑EL7930,黑E1H036,黑EL7026,黑E2SH75,黑E03850,黑ED5686,黑E8AP53,黑E037LN,黑E466NK,黑E1UL32,黑E498MN,黑EA2165,黑E261EJ,黑EL7595,黑EK2117,黑E985DY,黑E117EX,黑E861JT,黑EG6630,黑E891AH,黑E92488,黑E514KU,黑E59267,黑E566CW,黑E0HF71,黑E268CD,黑E3L327,黑ED2803,黑E1DQ97,黑E208HS,黑EGJ705,黑E7EH99,黑ECS379,黑E3EN20,黑E981CN,黑E5DP31,黑E59551,黑E265DH,黑EGS202,黑E721MN,黑EA0027,黑E00199,黑E8L909,黑E532CA,黑EGE526,黑E84482,黑EW7163,黑E788CP,黑EL2101,黑EC3968,黑EJ7608,黑EA2160,黑EA7358,黑E936CN,黑E38725,黑EA2385,黑ECF751,黑EVA375,黑ENU629,黑EC5030,黑E829MS,黑E879GD,黑ECQ575,黑E3DQ06,黑EA2398,黑E00452,黑EK4275,黑E00025,黑E9CB31,黑E305CB,黑E907DB,黑E690JR,黑E419PZ,黑E116AF,黑EGG692,黑EK4961,黑E1V728,黑E201CW,黑EGD286,黑EW1205,黑E8DN31,黑EP5130,黑EG1689,黑E204ES,黑E701CV,黑E021PX,黑EL2160,黑E6Q440,黑E391AZ,黑EJ0804,黑E2CD96,黑EB0060,黑EBY765,黑EG6150,黑E780KU,黑E1M293,黑EK6767,黑E691AC,黑EJ2646,黑EK0886,黑ECV826,黑E196PE,黑E59203,黑E775CG,黑EC3906,黑E2BJ33,黑E762NV,黑E357EN,黑EC6082,黑E5AT51,黑E082HW,黑EK9297,黑E767NL,黑EC0330,黑EL6066,黑EW3817,黑E596PS,黑E806CY,黑EG2375,黑EWG365,黑E380CE,黑EC5097,黑EW7062,黑AE4133,黑E999MR,黑E369PQ,黑E8L895,黑ECS395,黑EA2381,黑EKE870,黑E701DF,黑EF8595,黑E182CH,黑EA2512,黑E592PG,黑E7DX18,黑ERB936,黑E0Y257,黑EK9460,黑EL8086,黑EH0301,黑E8TG12,黑E568DF,黑E59271,黑E2CE03,黑ED3191,黑E965DL,黑EJ2962,黑EK4910,黑E279CW,黑E226DR,黑E665HM,黑EC0760,黑EL4586,黑EGH280,黑EV9291,黑E901CM,黑EC3883,黑EL5501,黑E999NX,黑EK2782,黑E050EW,黑EL7489,黑EU1702,黑E017DN,黑E00368,黑EL6552,黑E129AT,黑ERA122,黑EJ7666,黑EK4089,黑E00183,黑EJ1236,黑EK1706,黑EV9597,黑EBY627,黑EL2535,黑E8CQ30,黑E1M456,黑EK6557,黑E538CA,黑E0CA38,黑E273GJ,黑E7EA59,黑E3SG02,黑EUJ615,黑E9920L,黑E372BJ,黑EG2510,黑E109BZ,黑ED99786,黑E9T105,黑ECS385,黑E710JP,黑E0LX22,黑E78759,黑E43R68,黑E111CZ,黑EF38333,黑E7A835,黑E870CC,黑ED68567,黑E53H69,黑EWK281,黑EBP001,黑E3028F,黑E638PB,黑AT658S,黑E8N567,黑E938HB,黑EF96466,黑E79A31,黑A980RV,黑E977CN,黑EF95987,黑E2LR08,黑EPY355,黑E80V84,黑E5852M,黑E72H67,黑ENL056,黑E2BQ06,黑E01V00,黑E9KM32,黑EKX639,黑E9642H,黑E5182B,黑E3511A,黑ERS576,黑E77Q07,黑E83A89,黑E990MZ,黑ELW128,黑E18681,黑E6B653,黑EE1657,黑EWR521,黑EH4967,黑ED13985,黑EF78948,黑ES7339,黑E2JJ56,黑E044BY,黑E673BE,黑E7YV83,鲁BMY605,黑E716F1,黑EEJ802,黑EF81699,黑E317KA,黑E1597N,黑E2E930,黑MF42221,黑E167AS,黑E0YG62,黑EM9833,黑E918H7,黑ED21277,黑E178JH,黑ED0780,黑E3BA57,黑E768C8,黑EF15532,黑E096EB,鲁K7621Y,黑E3FH15,黑E7965A,黑E6HC00,黑E86E18,辽A612EG,黑E03V22,黑EL7386,黑EB8298,黑E898CY,黑E823PS,黑E8JN93,黑E8385C,黑EF36670,黑E656AB,黑EFH5661,黑ER2010,黑EFD5567,黑E1104H,黑E1808V,黑ED97770,黑E2667E,黑E0DE77,黑E8LL66,黑EAR316,黑E2046L,黑E6ZM26,黑E2WV90,黑ERL586,黑EPP756,黑EF73688,黑E31A58,黑E87N41,黑E7860B,黑E0XP60,黑EDG370,黑E79V28,黑E6C610,黑E65788,黑E05J17,黑E8282C,黑E8A183,黑E7616C,黑E2SF98,黑E1469K,黑E5098Y,黑E957HK,黑E80992,黑E697LN,黑E8ZN37,黑EF65687,黑E775N5,黑E8H635,黑ED79865,黑E3535E,黑E2108H,黑E517MU,黑EPY958,黑EVS712,黑E9L817,黑E5QU94,黑E81350,黑E9093W,黑ER9353,黑E3736Z,黑E15H27,黑E0TJ89,黑E9VC28,黑EF16967,黑EAK182,黑E258ME,黑E259EV,黑EL6458,黑EL6722,黑EH6968,黑E411JQ,黑E253CD,黑E206EK,黑E538JY,黑ED2817,黑E0RR07,黑E876MC,黑EJ3282,黑EL1639,黑E638AC,黑EK8539,黑EJ2491,黑EGC712,黑E85669,黑EGD299,黑EL6011,黑E787EE,黑EVG053,黑E6DH75,黑E632CC,黑E220HB,黑E8L996,黑E8L305,黑EL5753,黑E86378,黑E619CY,黑E569AT,黑E362EA,黑E597AB,黑E295JB,黑E293LH,黑EJ5295,黑E701BZ,黑E9MM22,黑EA2511,黑E683DK,黑EL1225,黑EGD219,黑EBL796,黑EL0986,黑EL6847,黑E501MD,黑EJ5628,黑E298LH,黑E00033,黑EK2758,黑EGD300,黑EGD297,黑E1M359,黑EJ7526,黑E6EV99,黑E232PC,黑E231PD,黑EK3311,黑EJ6942,黑EL8481,黑EL5590,黑E389KC,黑EK0961,黑E00035,黑E263AW,黑E7HC37,黑E555NS,黑E652CC,黑E211BW,黑E799CN,黑E5NJ60,黑EJ2069,黑E321PG,黑EK8753,黑EJ3696,黑E00188,黑E789GS,黑E316HQ,黑E620GS,黑E072CV,黑E711HE,黑EK6115,黑EGD583,黑E59387,黑E2BR31,黑E00430,黑EK3216,黑E9DE72,黑EE4500,黑E107DK,黑EE1157,黑E011KC,黑EL8252,黑E3HN20,黑E670KN,黑EJ6593,黑E00031,黑E257LM,黑EJ8098,黑EL0792,黑ED3138,黑E394MR,黑EJ1732,黑E339HN,黑E713BX,黑E1T740,黑E603BR,黑ECD010,黑EGE587,黑EL3941,黑EL3999,黑E1HQ80,黑E5CP92,黑EJ6175,黑EJ7601,黑E162AL,黑E380HM,黑EU6967,黑E7MM19,黑EQK509,黑E0VP30,黑EK7783,黑E692MX,黑EC3690,黑EL2622,黑E965NX,黑EL3109,黑E1T043,黑E938KD,黑E55052,黑E660AN,黑EL5043,黑EJ7313,黑E152CZ,黑E702CB,黑E8LK58,黑E893CS,黑EK8491,黑E0DV52,黑EK0228,黑E987DY,黑EL5007,黑EJ0296,黑E519MA,黑E893BY,黑E340DK,黑EKK633,黑E1BB92,黑EL3989,黑ECS511,黑E789PD,黑E1SJ97,黑EVF850,黑E203FN,黑E1UA86,黑EGC771,黑ED43222,黑EJY016,黑E61H69,黑E038E0,黑E787P2,黑E0C708,黑AD21122,黑EQG165,黑E59T11,黑A3K5T3,黑E1FA26,黑E39V71,黑E1LD78,黑E6C173,黑EBD998,黑E177G5,黑E24R88,黑E1387E,黑EG9278"; // 从请求中获取车牌号字符串
        log.info("========== 收到清理重复VIP票的请求 ==========");
        log.info("车牌号字符串: {}", plateNumbersStr);

        try {
            if (plateNumbersStr == null || plateNumbersStr.trim().isEmpty()) {
                return R.error().message("车牌号列表不能为空");
            }

            VipFixService.FixResult result = vipFixService.cleanDuplicateVips(plateNumbersStr);

            if (result.isSuccess()) {
                return R.ok()
                        .message("清理完成")
                        .data("total", result.getTotal())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount())
                        .data("startTime", result.getStartTime())
                        .data("endTime", result.getEndTime())
                        .data("failedRecords", result.getFailedRecords());
            } else {
                return R.error()
                        .message("清理失败: " + result.getErrorMessage())
                        .data("total", result.getTotal())
                        .data("successCount", result.getSuccessCount())
                        .data("failedCount", result.getFailedCount());
            }

        } catch (Exception e) {
            log.error("清理重复VIP票失败", e);
            return R.error().message("清理失败: " + e.getMessage());
        }
    }
}