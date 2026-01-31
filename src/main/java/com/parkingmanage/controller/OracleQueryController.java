package com.parkingmanage.controller;

import com.parkingmanage.common.R;
import com.parkingmanage.dto.oracle.VehicleValidInfoDTO;
import com.parkingmanage.service.oracle.OracleQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Oracle数据查询控制器
 * 提供Oracle数据库查询的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/oracle")
@Api(tags = "Oracle数据查询接口")
public class OracleQueryController {

    @Autowired
    private OracleQueryService oracleQueryService;

    /**
     * 根据车牌号码查询车辆有效信息
     *
     * @param plateNumber 车牌号码
     * @return 车辆有效信息列表
     */
    @GetMapping("/vehicle/query")
    @ApiOperation("根据车牌号码查询车辆有效信息")
    public R queryVehicleByPlateNumber(
            @ApiParam(value = "车牌号码", required = true, example = "京A12345")
            @RequestParam("plateNumber") String plateNumber) {

        log.info("收到车辆查询请求: 车牌号={}", plateNumber);

        try {
            // 参数校验
            if (plateNumber == null || plateNumber.trim().isEmpty()) {
                return R.error().message("车牌号码不能为空");
            }

            // 查询车辆信息
            List<VehicleValidInfoDTO> vehicles = oracleQueryService.queryVehicleByPlateNumber(plateNumber);

            if (vehicles.isEmpty()) {
                return R.ok()
                        .message("未查询到该车牌的记录")
                        .data("plateNumber", plateNumber)
                        .data("count", 0)
                        .data("list", vehicles);
            }

            return R.ok()
                    .message("查询成功")
                    .data("plateNumber", plateNumber)
                    .data("count", vehicles.size())
                    .data("list", vehicles);

        } catch (Exception e) {
            log.error("查询车辆信息失败: 车牌号={}, 错误: {}", plateNumber, e.getMessage(), e);
            return R.error().message("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有车辆信息（用于测试，返回前N条）
     *
     * @param limit 返回记录数限制，默认10条
     * @return 车辆有效信息列表
     */
    @GetMapping("/vehicle/list")
    @ApiOperation("查询所有车辆信息（测试用）")
    public R listVehicles(
            @ApiParam(value = "返回记录数限制", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        log.info("收到车辆列表查询请求: limit={}", limit);
        try {
            // 限制最大返回100条
            if (limit > 100) {
                limit = 100;
            }
            // 查询车辆信息
            List<VehicleValidInfoDTO> vehicles = oracleQueryService.queryAllVehicles(limit);
            return R.ok()
                    .message("查询成功")
                    .data("count", vehicles.size())
                    .data("list", vehicles);

        } catch (Exception e) {
            log.error("查询车辆列表失败: {}", e.getMessage(), e);
            return R.error().message("查询失败: " + e.getMessage());
        }
    }
}